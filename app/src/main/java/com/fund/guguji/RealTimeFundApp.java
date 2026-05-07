package com.fund.guguji;

import android.app.Application;

import com.fund.guguji.data.api.EastMoneyApi;
import com.fund.guguji.data.api.FundSearchApi;
import com.fund.guguji.data.api.TencentQuoteApi;
import com.fund.guguji.data.db.AppDatabase;
import com.fund.guguji.data.repository.FundRepository;
import com.fund.guguji.data.repository.LocalFundRepository;
import com.fund.guguji.util.Constants;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * 实时估值 Application
 */
public class RealTimeFundApp extends Application {

    private AppDatabase database;
    private OkHttpClient httpClient;
    private EastMoneyApi eastMoneyApi;
    private TencentQuoteApi tencentQuoteApi;
    private FundSearchApi fundSearchApi;
    private FundRepository fundRepository;
    private LocalFundRepository localFundRepository;

    @Override
    public void onCreate() {
        super.onCreate();

        database = AppDatabase.getInstance(this);

        httpClient = new OkHttpClient.Builder()
                .connectTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(Constants.READ_TIMEOUT, TimeUnit.SECONDS)
                .build();

        eastMoneyApi = new EastMoneyApi(httpClient);
        tencentQuoteApi = new TencentQuoteApi(httpClient);
        fundSearchApi = new FundSearchApi(httpClient, Constants.GSON);

        fundRepository = new FundRepository(
                database.fundDao(),
                database.holdingDao(),
                database.seriesDao(),
                eastMoneyApi,
                tencentQuoteApi
        );

        localFundRepository = new LocalFundRepository(
                database.fundDao(),
                database.holdingDao(),
                database.groupDao()
        );
    }

    public AppDatabase getDatabase() { return database; }
    public OkHttpClient getHttpClient() { return httpClient; }
    public EastMoneyApi getEastMoneyApi() { return eastMoneyApi; }
    public TencentQuoteApi getTencentQuoteApi() { return tencentQuoteApi; }
    public FundSearchApi getFundSearchApi() { return fundSearchApi; }
    public FundRepository getFundRepository() { return fundRepository; }
    public LocalFundRepository getLocalFundRepository() { return localFundRepository; }
}
