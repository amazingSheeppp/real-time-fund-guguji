package com.fund.guguji.data.repository;

import androidx.lifecycle.LiveData;

import com.fund.guguji.data.api.EastMoneyApi;
import com.fund.guguji.data.api.TencentQuoteApi;
import com.fund.guguji.data.db.dao.FundDao;
import com.fund.guguji.data.db.entity.FundEntity;
import com.fund.guguji.data.model.HoldingsResult;
import com.fund.guguji.data.model.NavResult;
import com.fund.guguji.util.MarketUtils;

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
    private final TencentQuoteApi tencentQuoteApi;

    public FundRepository(FundDao fundDao, EastMoneyApi eastMoneyApi, TencentQuoteApi tencentQuoteApi) {
        this.fundDao = fundDao;
        this.eastMoneyApi = eastMoneyApi;
        this.tencentQuoteApi = tencentQuoteApi;
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

    public EastMoneyApi getEastMoneyApi() {
        return eastMoneyApi;
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
     * 非交易时段(收盘后/周末)额外同步官方净值:官方数据为当日或更新时覆盖估值展示
     */
    public Observable<FundEntity> refreshSingleValuation(FundEntity fund) {
        Observable<FundEntity> valuationFlow = eastMoneyApi.fetchValuation(fund.getCode())
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
                });

        // 盘中只用估值;收盘后官方净值陆续公布,顺带同步官方数据
        if (MarketUtils.isTradingTime()) {
            return valuationFlow.subscribeOn(Schedulers.io());
        }
        return valuationFlow
                .flatMap(f -> eastMoneyApi.fetchOfficialNav(f.getCode())
                        .map(nav -> {
                            applyOfficialNav(f, nav);
                            return f;
                        })
                        // 官方净值同步失败不影响估值展示
                        .onErrorResumeNext(throwable -> Observable.just(f)))
                .subscribeOn(Schedulers.io());
    }

    /**
     * 应用官方净值:仅当官方日期比已展示的净值日期新(或相同但尚未落库)时写入
     */
    private void applyOfficialNav(FundEntity fund, NavResult nav) {
        if (nav.getDate() == null || nav.getNav() <= 0) return;
        String currentDate = fund.getOfficialNavDate();
        if (currentDate != null && currentDate.compareTo(nav.getDate()) >= 0) {
            // 已同步过相同或更新的官方净值,跳过
            return;
        }
        fund.setOfficialNav(String.valueOf(nav.getNav()));
        fund.setOfficialNavDate(nav.getDate());
        fund.setOfficialNavChange(nav.getGrowth());
        fundDao.updateFund(fund);
    }

    /**
     * 获取持仓股票信息并合并当日实时涨跌(基金前十大重仓股)
     * 行情来自腾讯接口,单次批量请求;未匹配到市场的标的涨跌为 null
     */
    public Observable<HoldingsResult> fetchHoldingsWithQuotes(String fundCode) {
        return eastMoneyApi.fetchHoldings(fundCode)
                .flatMap(result -> {
                    List<HoldingsResult.HoldingStock> stocks = result.getStocks();
                    if (stocks == null || stocks.isEmpty()) {
                        return Observable.just(result);
                    }
                    String[] marketCodes = new String[stocks.size()];
                    for (int i = 0; i < stocks.size(); i++) {
                        marketCodes[i] = toMarketCode(stocks.get(i).getCode());
                    }
                    return tencentQuoteApi.fetchBatchChangePercent(marketCodes)
                            .map(changes -> {
                                for (int i = 0; i < stocks.size() && i < changes.length; i++) {
                                    stocks.get(i).setChangePercent(changes[i]);
                                }
                                return result;
                            })
                            .onErrorResumeNext(throwable -> Observable.just(result));
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 股票代码转腾讯行情市场前缀
     * 6/9 开头=沪,0/3 开头=深,其余(如 5 位港股代码)由具体格式判断;无法识别返回 null
     */
    private String toMarketCode(String code) {
        if (code == null || code.isEmpty()) return null;
        if (code.length() == 6) {
            char first = code.charAt(0);
            if (first == '6' || first == '9') return "sh" + code;
            if (first == '0' || first == '3') return "sz" + code;
            if (first == '4' || first == '8') return "bj" + code;
            return null;
        }
        // 港股 5 位数字
        if (code.length() == 5 && code.chars().allMatch(Character::isDigit)) {
            return "hk" + code;
        }
        return null;
    }
}
