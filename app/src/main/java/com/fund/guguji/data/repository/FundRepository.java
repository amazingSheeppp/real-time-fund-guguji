package com.fund.guguji.data.repository;

import androidx.lifecycle.LiveData;

import com.fund.guguji.data.api.EastMoneyApi;
import com.fund.guguji.data.db.dao.FundDao;
import com.fund.guguji.data.db.entity.FundEntity;
import com.fund.guguji.data.model.HoldingsResult;

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
    private final EastMoneyApi eastMoneyApi;

    public FundRepository(FundDao fundDao, EastMoneyApi eastMoneyApi) {
        this.fundDao = fundDao;
        this.eastMoneyApi = eastMoneyApi;
    }

    // ── 本地数据 ──

    public LiveData<List<FundEntity>> getAllFunds() {
        return fundDao.getAllFunds();
    }

    public void addFund(FundEntity fund) {
        Integer maxOrder = fundDao.getMaxOrderIndex();
        fund.setOrderIndex(maxOrder == null ? 1 : maxOrder + 1);
        fundDao.insertFund(fund);
    }

    public void removeFund(String code) {
        fundDao.deleteFundByCode(code);
    }

    public FundEntity getFund(String code) {
        return fundDao.getFundByCode(code);
    }

    // ── 远程数据 ──

    /**
     * 刷新所有基金的实时估值
     */
    public Observable<List<FundEntity>> refreshAllValuations() {
        return Observable.fromCallable(() -> {
                    List<FundEntity> funds = fundDao.getAllFundsSync();
                    if (funds == null || funds.isEmpty()) {
                        return java.util.Collections.<FundEntity>emptyList();
                    }

                    List<FundEntity> updated = new java.util.ArrayList<>();
                    for (FundEntity fund : funds) {
                        try {
                            FundEntity refreshed = refreshSingleValuation(fund).blockingFirst();
                            updated.add(refreshed);
                        } catch (Exception e) {
                            // 单只基金异常兜底，避免阻断整批基金刷新
                            updated.add(fund);
                        }
                    }
                    return updated;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 刷新单只基金的实时估值并持久化
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
                    // 若已有名称则保留，避免被远程简写或非标准名称覆盖
                    if (fund.getName() == null || fund.getName().isEmpty()) {
                        fund.setName(valuation.getName());
                    }
                    fund.setNoValuation(false);

                    fundDao.updateFund(fund);

                    return Observable.just(fund);
                })
                .onErrorResumeNext(throwable -> {
                    // 无实时估值或获取失败时标记 noValuation 并持久化
                    fund.setNoValuation(true);
                    fundDao.updateFund(fund);
                    return Observable.just(fund);
                })
                .subscribeOn(Schedulers.io());
    }

    /**
     * 获取持仓股票信息(基金前十大重仓股)
     */
    public Observable<HoldingsResult> fetchHoldingsWithQuotes(String fundCode) {
        return eastMoneyApi.fetchHoldings(fundCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
