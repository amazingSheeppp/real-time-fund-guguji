package com.fund.guguji.data.api;

import com.fund.guguji.data.model.FundSearchResult;
import com.google.gson.Gson;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 基金搜索接口
 * 对应 Web 项目中的 fundsuggest.eastmoney.com 搜索接口
 */
public class FundSearchApi {

    private static final String SEARCH_URL =
            "https://fundsuggest.eastmoney.com/FundSearch/api/FundSearchAPI.ashx?m=1&key=%s";

    // 支持分页的端点:pageindex 从 0 开始,pagesize 实测最大 50
    private static final String SEARCH_PAGE_URL =
            "https://fundsuggest.eastmoney.com/FundSearch/api/FundSearchPageAPI.ashx?m=1&key=%s&pageindex=%d&pagesize=%d";

    private static final int PAGE_SIZE = 20;

    private final OkHttpClient client;
    private final Gson gson;

    public FundSearchApi(OkHttpClient client, Gson gson) {
        this.client = client;
        this.gson = gson;
    }

    /**
     * 搜索基金
     *
     * @param keyword 关键字（代码、名称、拼音）
     * @return Observable<List<FundSearchResult.FundSearchItem>>
     */
    public Observable<List<FundSearchResult.FundSearchItem>> searchFunds(String keyword) {
        return searchFundsByPage(keyword, 0, PAGE_SIZE);
    }

    /**
     * 按页搜索基金
     *
     * @param keyword   关键字（代码、名称、拼音）
     * @param pageIndex 页码,从 0 开始
     * @param pageSize  每页条数
     * @return Observable<List<FundSearchResult.FundSearchItem>>
     */
    public Observable<List<FundSearchResult.FundSearchItem>> searchFundsByPage(String keyword, int pageIndex, int pageSize) {
        return Observable.create(emitter -> {
            try {
                String url = String.format(SEARCH_PAGE_URL, keyword, pageIndex, pageSize);
                Request request = new Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0")
                        .header("Referer", "https://fund.eastmoney.com/")
                        .build();
                Response response = client.newCall(request).execute();
                String body = response.body() != null ? response.body().string() : "";

                FundSearchResult result = gson.fromJson(body, FundSearchResult.class);
                if (result != null && result.getErrCode() == 0 && result.getDatas() != null) {
                    emitter.onNext(result.getDatas());
                } else {
                    emitter.onNext(new java.util.ArrayList<>());
                }
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }
}
