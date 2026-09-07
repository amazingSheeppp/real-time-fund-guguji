package com.fund.guguji.data.api;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.core.Observable;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 腾讯股票行情接口
 * 对应 Web 项目中的 qt.gtimg.cn，用于获取股票实时涨幅
 * 返回为 GBK 编码，字段以 ~ 分隔:
 * v_sz000001="51~平安银行~000001~11.70(现价,3)~11.89(昨收,4)~...~-0.19(涨跌额,31)~-1.60(涨跌幅%,32)~..."
 */
public class TencentQuoteApi {

    private static final String QUOTE_URL = "http://qt.gtimg.cn/q=%s";
    // v_xxxnnnnn="..." 的变量名，用于按代码匹配结果
    private static final Pattern VAR_PATTERN = Pattern.compile("v_(\\w+)=\"([^\"]*)\"");

    private final OkHttpClient client;

    public TencentQuoteApi(OkHttpClient client) {
        this.client = client;
    }

    /**
     * 获取股票实时涨跌幅
     *
     * @param marketCode 市场代码，如 "sz000001", "sh600519", "hk00700"
     * @return Observable<Double> 涨跌幅百分比
     */
    public Observable<Double> fetchStockChangePercent(String marketCode) {
        return fetchBatchChangePercent(marketCode).map(results -> results[0]);
    }

    /**
     * 批量获取股票涨跌幅
     * 接口一次请求返回多只股票，按回传的变量名与请求码对应
     *
     * @param marketCodes 市场代码列表
     * @return Observable<Double[]> 涨跌幅数组，与入参顺序一一对应；解析失败的位置为 null
     */
    public Observable<Double[]> fetchBatchChangePercent(String... marketCodes) {
        return Observable.create(emitter -> {
            try {
                StringBuilder sb = new StringBuilder();
                for (String code : marketCodes) {
                    sb.append(code).append(",");
                }
                sb.setLength(sb.length() - 1);

                Request request = new Request.Builder()
                        .url(String.format(QUOTE_URL, sb))
                        .header("User-Agent", "Mozilla/5.0")
                        .build();
                Response response = client.newCall(request).execute();
                // qt.gtimg.cn 返回 GBK 编码，按 UTF-8 读中文会乱码
                String body = response.body() != null
                        ? new String(response.body().bytes(), "GBK")
                        : "";

                // 解析出 每只股票的变量名 -> 涨跌幅
                Map<String, Double> byCode = new HashMap<>();
                Matcher matcher = VAR_PATTERN.matcher(body);
                while (matcher.find()) {
                    String[] parts = matcher.group(2).split("~");
                    if (parts.length > CHANGE_PERCENT_INDEX) {
                        try {
                            byCode.put(matcher.group(1).toLowerCase(),
                                    Double.parseDouble(parts[CHANGE_PERCENT_INDEX].trim()));
                        } catch (NumberFormatException ignored) {}
                    }
                }

                Double[] results = new Double[marketCodes.length];
                for (int i = 0; i < marketCodes.length; i++) {
                    results[i] = byCode.get(marketCodes[i].toLowerCase());
                }
                emitter.onNext(results);
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    // 涨跌幅在 ~ 分隔字段中的索引(索引3=现价,4=昨收,31=涨跌额,32=涨跌幅%)
    private static final int CHANGE_PERCENT_INDEX = 32;
}
