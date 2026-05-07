package com.fund.guguji.data.repository;

import androidx.lifecycle.LiveData;

import com.fund.guguji.data.api.EastMoneyApi;
import com.fund.guguji.data.api.TencentQuoteApi;
import com.fund.guguji.data.db.dao.FundDao;
import com.fund.guguji.data.db.dao.HoldingDao;
import com.fund.guguji.data.db.dao.ValuationSeriesDao;
import com.fund.guguji.data.db.entity.FundEntity;
import com.fund.guguji.data.db.entity.HoldingEntity;
import com.fund.guguji.data.model.HoldingsResult;
import com.fund.guguji.data.model.ProfitResult;
import com.fund.guguji.util.ProfitCalculator;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 基金数据仓库
 * 协调本地数据库和远程 API 的数据同步
 */
public class FundRepository {

    private final FundDao fundDao;
    private final HoldingDao holdingDao;
    private final ValuationSeriesDao seriesDao;
    private final EastMoneyApi eastMoneyApi;
    private final TencentQuoteApi tencentQuoteApi;

    public FundRepository(FundDao fundDao, HoldingDao holdingDao,
                          ValuationSeriesDao seriesDao,
                          EastMoneyApi eastMoneyApi, TencentQuoteApi tencentQuoteApi) {
        this.fundDao = fundDao;
        this.holdingDao = holdingDao;
        this.seriesDao = seriesDao;
        this.eastMoneyApi = eastMoneyApi;
        this.tencentQuoteApi = tencentQuoteApi;
    }

    // ── 本地数据 ──

    public LiveData<List<FundEntity>> getAllFunds() {
        return fundDao.getAllFunds();
    }

    public LiveData<List<HoldingEntity>> getAllHoldings() {
        return holdingDao.getAllHoldings();
    }

    public void addFund(FundEntity fund) {
        fundDao.insertFund(fund);
    }

    public void removeFund(String code) {
        fundDao.deleteFundByCode(code);
        holdingDao.deleteByCode(code);
    }

    public void saveHolding(HoldingEntity holding) {
        holdingDao.insertOrUpdate(holding);
    }

    public HoldingEntity getHolding(String fundCode) {
        return holdingDao.getHoldingByCode(fundCode);
    }

    public FundEntity getFund(String code) {
        return fundDao.getFundByCode(code);
    }

    // ── 远程数据 ──

    /**
     * 刷新所有基金的估值
     */
    public Observable<List<FundEntity>> refreshAllValuations() {
        return Observable.fromCallable(() -> {
                    List<FundEntity> funds = fundDao.getAllFundsSync();
                    if (funds == null || funds.isEmpty()) {
                        return java.util.Collections.<FundEntity>emptyList();
                    }

                    List<FundEntity> updated = new java.util.ArrayList<>();
                    for (FundEntity fund : funds) {
                        FundEntity refreshed = refreshSingleValuation(fund).blockingFirst();
                        updated.add(refreshed);
                    }
                    return updated;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 刷新单只基金的估值及收益
     */
    public Observable<FundEntity> refreshSingleValuation(FundEntity fund) {
        return eastMoneyApi.fetchValuation(fund.getCode())
                .flatMap(valuation -> {
                    fund.setGsz(valuation.getGsz());
                                        try {
                        fund.setGszzl(Double.parseDouble(valuation.getGszzl()));
                    } catch (NumberFormatException e) {
                        fund.setGszzl(0.0);
                    }
                    fund.setGztime(valuation.getGztime());
                    fund.setDwjz(valuation.getDwjz());
                    fund.setJzrq(valuation.getJzrq());
                    fund.setName(valuation.getName());

                    // 计算收益
                    HoldingEntity holding = holdingDao.getHoldingByCode(fund.getCode());
                    if (holding != null && holding.getShare() != null && holding.getShare() > 0
                            && valuation.getDwjz() != null && valuation.getGsz() != null) {
                        try {
                            double nav = Double.parseDouble(valuation.getDwjz());
                            double gsz = Double.parseDouble(valuation.getGsz());
                            double prevNav = nav;
                            if (Math.abs(gsz - nav) > 0.0001) {
                                prevNav = nav;
                            }
                            double share = holding.getShare();
                            double cost = holding.getCost() != null ? holding.getCost() : 0.0;
                            ProfitResult profit = ProfitCalculator.calculate(
                                    share,
                                    cost,
                                    gsz,
                                    (gsz - prevNav) / prevNav * 100.0
                            );
                            fund.setProfitAmount(profit.getAmount());
                            fund.setProfitToday(profit.getProfitToday());
                            fund.setProfitTotal(profit.getProfitTotal());
                            fund.setProfitTodayPercent(profit.getProfitTodayPercent());
                            fund.setProfitTotalPercent(profit.getProfitTotalPercent());
                        } catch (NumberFormatException ignored) {}
                    }

                    // 持久化
                    fundDao.updateFund(fund);

                    return Observable.just(fund);
                })
                .subscribeOn(Schedulers.io());
    }

    /**
     * 刷新单只基金的估值（无持仓信息时只更新估值字段）
     */
    public Observable<FundEntity> refreshValuationOnly(FundEntity fund) {
        return eastMoneyApi.fetchValuation(fund.getCode())
                .flatMap(valuation -> {
                    fund.setGsz(valuation.getGsz());
                                        try {
                        fund.setGszzl(Double.parseDouble(valuation.getGszzl()));
                    } catch (NumberFormatException e) {
                        fund.setGszzl(0.0);
                    }
                    fund.setGztime(valuation.getGztime());
                    fund.setDwjz(valuation.getDwjz());
                    fund.setJzrq(valuation.getJzrq());
                    fund.setName(valuation.getName());
                    fundDao.updateFund(fund);
                    return Observable.just(fund);
                })
                .subscribeOn(Schedulers.io());
    }

    /**
     * 获取持仓股票信息及实时涨跌
     */
    public Observable<HoldingsResult> fetchHoldingsWithQuotes(String fundCode) {
        return eastMoneyApi.fetchHoldings(fundCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 获取单只基金历史净值
     */
    public Observable<List<com.fund.guguji.data.model.NavResult>> fetchFundHistory(String fundCode) {
        return eastMoneyApi.fetchFundHistory(fundCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
