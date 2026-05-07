package com.fund.guguji.data.api;

import java.util.regex.Pattern;

import io.reactivex.rxjava3.core.Observable;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 腾讯股票行情接口
 * 对应 Web 项目中的 qt.gtimg.cn，用于获取股票实时涨幅
 * 返回格式: ~...~...~涨跌幅~... 等字段
 */
public class TencentQuoteApi {

    private static final String QUOTE_URL = "http://qt.gtimg.cn/q=%s%s";
    private static final Pattern PRICE_CHANGE_PATTERN = Pattern.compile("~(\\d+\\.?\\d*)~");

    private final OkHttpClient client;

    public TencentQuoteApi(OkHttpClient client) {
        this.client = client;
    }

    /**
     * 获取股票实时涨跌幅
     *
     * @param marketCode 市场代码，如 "sz000001", "sh600519"
     * @return Observable<Double> 涨跌幅百分比
     */
    public Observable<Double> fetchStockChangePercent(String marketCode) {
        return Observable.create(emitter -> {
            try {
                String url = String.format(QUOTE_URL, "", marketCode);
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                String body = response.body() != null ? response.body().string() : "";

                // qt.gtimg.cn 返回格式: v_sz000001="...~...~...~...";
                // 涨跌幅在字段索引 5 (从0开始)
                String[] parts = body.split("~");
                if (parts.length > 6) {
                    try {
                        double changePercent = Double.parseDouble(parts[5].trim());
                        emitter.onNext(changePercent);
                        emitter.onComplete();
                        return;
                    } catch (NumberFormatException ignored) {}
                }

                emitter.onError(new RuntimeException("无法解析股票行情: " + marketCode));
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    /**
     * 批量获取股票涨跌幅
     *
     * @param marketCodes 市场代码列表
     * @return Observable<Double[]> 涨跌幅数组
     */
    public Observable<Double[]> fetchBatchChangePercent(String... marketCodes) {
        return Observable.create(emitter -> {
            try {
                StringBuilder sb = new StringBuilder();
                for (String code : marketCodes) {
                    sb.append(",").append(code);
                }
                String url = String.format(QUOTE_URL, "", sb.substring(1));
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                String body = response.body() != null ? response.body().string() : "";

                // 每个股票返回为 v_code="...~...~涨跌幅~...";
                Double[] results = new Double[marketCodes.length];
                String[] lines = body.split(";");
                for (int i = 0; i < lines.length && i < marketCodes.length; i++) {
                    String[] parts = lines[i].split("~");
                    if (parts.length > 6) {
                        try {
                            results[i] = Double.parseDouble(parts[5].trim());
                        } catch (NumberFormatException e) {
                            results[i] = 0.0;
                        }
                    } else {
                        results[i] = 0.0;
                    }
                }

                emitter.onNext(results);
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }
}
