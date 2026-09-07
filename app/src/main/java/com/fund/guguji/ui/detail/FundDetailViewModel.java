package com.fund.guguji.ui.detail;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fund.guguji.data.db.entity.FundEntity;
import com.fund.guguji.data.model.HoldingsResult;
import com.fund.guguji.data.model.NavResult;
import com.fund.guguji.data.repository.FundRepository;
import com.fund.guguji.util.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 基金详情 ViewModel
 * 提供持仓(含当日涨跌)与历史净值曲线数据,进程内会话级缓存避免反复请求
 */
public class FundDetailViewModel extends AndroidViewModel {

    public enum ChartRange {
        D7(7, "近7日"),
        M1(Constants.CHART_DAYS_1M, "近1月"),
        M6(Constants.CHART_DAYS_6M, "近半年"),
        Y1(Constants.CHART_DAYS_1Y, "近1年");

        public final int days;
        public final String label;

        ChartRange(int days, String label) {
            this.days = days;
            this.label = label;
        }
    }

    /** 图表数据:切片后的净值点(升序) */
    public static class ChartData {
        public final List<NavResult> points;
        public final ChartRange range;
        /** 区间累计涨跌%(基于单位净值首尾计算,分红日会有跳变) */
        public final double rangeChangePercent;

        ChartData(List<NavResult> points, ChartRange range, double rangeChangePercent) {
            this.points = points;
            this.range = range;
            this.rangeChangePercent = rangeChangePercent;
        }
    }

    private final FundRepository fundRepository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    // ── 会话级缓存:同一次 App 运行内复用,退出进程即失效 ──
    private List<NavResult> navHistoryCache;          // 全量净值历史(升序)
    private String navHistoryCode;                    // 缓存对应的基金代码
    private HoldingsResult holdingsCache;             // 持仓+当日涨跌
    private String holdingsCode;

    private final MutableLiveData<FundEntity> fund = new MutableLiveData<>();
    private final MutableLiveData<ChartData> chartData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> chartLoading = new MutableLiveData<>(true);
    private final MutableLiveData<String> chartError = new MutableLiveData<>();
    private final MutableLiveData<HoldingsResult> holdings = new MutableLiveData<>();
    private final MutableLiveData<Boolean> holdingsLoading = new MutableLiveData<>(true);
    private final MutableLiveData<String> holdingsError = new MutableLiveData<>();

    public FundDetailViewModel(Application app) {
        super(app);
        fundRepository = ((com.fund.guguji.RealTimeFundApp) app).getFundRepository();
    }

    public void init(FundEntity entity) {
        fund.setValue(entity);
        loadChart(ChartRange.M1);
        loadHoldings(entity.getCode());

        // 从数据库异步取最新估值(该基金在自选中时,覆盖 intent 传入的种子数据)
        disposables.add(Observable.fromCallable(() -> fundRepository.getFund(entity.getCode()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(dbFund -> {
                    if (dbFund != null) {
                        fund.setValue(dbFund);
                    }
                }, throwable -> { /* 保持种子数据展示 */ }));
    }

    /** 数据库中该基金的实时估值(首页刷新后自动同步) */
    public LiveData<FundEntity> getFund() {
        return fund;
    }

    public FundRepository getFundRepository() {
        return fundRepository;
    }

    public LiveData<ChartData> getChartData() { return chartData; }
    public LiveData<Boolean> getChartLoading() { return chartLoading; }
    public LiveData<String> getChartError() { return chartError; }
    public LiveData<HoldingsResult> getHoldings() { return holdings; }
    public LiveData<Boolean> getHoldingsLoading() { return holdingsLoading; }
    public LiveData<String> getHoldingsError() { return holdingsError; }

    /**
     * 切换图表区间:命中缓存直接切片,否则拉全量历史后切片
     */
    public void loadChart(ChartRange range) {
        chartLoading.setValue(true);
        chartError.setValue(null);

        if (navHistoryCache != null && navHistoryCode != null
                && navHistoryCode.equals(fund.getValue() != null ? fund.getValue().getCode() : null)) {
            chartData.setValue(sliceChart(navHistoryCache, range));
            chartLoading.setValue(false);
            return;
        }

        String code = fund.getValue() != null ? fund.getValue().getCode() : null;
        if (code == null) {
            chartError.setValue("缺少基金代码");
            chartLoading.setValue(false);
            return;
        }

        disposables.add(
                fundRepository.getEastMoneyApi().fetchNavHistory(code)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(history -> {
                            navHistoryCache = history;
                            navHistoryCode = code;
                            chartData.setValue(sliceChart(history, range));
                            chartLoading.setValue(false);
                        }, throwable -> {
                            chartError.setValue(throwable.getMessage());
                            chartLoading.setValue(false);
                        })
        );
    }

    public void loadHoldings(String code) {
        // 同一只基金已有缓存时直接复用(当日数据)
        if (holdingsCache != null && code.equals(holdingsCode)) {
            holdings.setValue(holdingsCache);
            holdingsLoading.setValue(false);
            return;
        }

        holdingsLoading.setValue(true);
        holdingsError.setValue(null);
        disposables.add(
                fundRepository.fetchHoldingsWithQuotes(code)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            holdingsCache = result;
                            holdingsCode = code;
                            holdings.setValue(result);
                            holdingsLoading.setValue(false);
                        }, throwable -> {
                            holdingsError.setValue(throwable.getMessage());
                            holdingsLoading.setValue(false);
                        })
        );
    }

    public void retryHoldings() {
        FundEntity entity = fund.getValue();
        if (entity != null) {
            holdingsCache = null;
            holdingsCode = null;
            loadHoldings(entity.getCode());
        }
    }

    /**
     * 按区间天数对升序净值序列切片,并计算区间累计涨跌%
     */
    private ChartData sliceChart(List<NavResult> history, ChartRange range) {
        if (history == null || history.isEmpty()) {
            return new ChartData(Collections.emptyList(), range, 0);
        }
        // pingzhongdata 返回为升序,截取尾部 days 个交易日
        int from = Math.max(0, history.size() - range.days - 1);
        List<NavResult> points = new ArrayList<>(history.subList(from, history.size()));
        double first = points.get(0).getNav();
        double last = points.get(points.size() - 1).getNav();
        double change = first > 0 ? (last - first) / first * 100.0 : 0;
        return new ChartData(points, range, change);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}
