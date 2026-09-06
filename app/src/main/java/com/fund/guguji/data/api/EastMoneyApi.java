package com.fund.guguji.data.api;

import com.fund.guguji.data.model.FundValuation;
import com.fund.guguji.data.model.HoldingsResult;
import com.fund.guguji.data.model.NavResult;
import com.fund.guguji.util.Constants;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.core.Observable;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 东方财富基金数据接口
 * 对应 Web 项目中的 fundgz (实时估值)、lsjz (历史净值)、jjcc (持仓) 等接口
 */
public class EastMoneyApi {

    private static final String FUNDGZ_URL = "http://fundgz.1234567.com.cn/js/%s.js";
    // 新浪盘中实时估值接口(f_接口已停发估值,fu_为官方页面在用的估值数据源)
    private static final String SINA_GZ_URL = "https://hq.sinajs.cn/list=fu_%s";
    private static final String LSJZ_URL = "http://fund.eastmoney.com/f10/jjjz_%s.html";
    private static final String JJCC_URL = "http://fund.eastmoney.com/f10/ccmx_%s.html";
    private static final String PINGZHONG_URL = "http://fundgz.1234567.com.cn/js/%s.js";

    private static final Pattern JSONP_PATTERN = Pattern.compile("jsonpgz\\((.+?)\\);?");

    // 新浪返回形如:var hq_str_fu_005827="名称,16:04:00,估算净值,昨净值,昨净值,0,涨跌幅%,净值日期,...";
    private static final Pattern SINA_GZ_PATTERN = Pattern.compile("\"([^\"]*)\"\\s*;?\\s*$");

    private final OkHttpClient client;

    public EastMoneyApi(OkHttpClient client) {
        this.client = client;
    }

    /**
     * 获取基金实时估值
     * 主源:新浪 fu_ 盘中估值接口;回退:东方财富 fundgz JSONP 接口
     *
     * @param fundCode 基金代码
     * @return Observable<FundValuation>
     */
    public Observable<FundValuation> fetchValuation(String fundCode) {
        return Observable.create(emitter -> {
            try {
                FundValuation valuation = fetchValuationFromSina(fundCode);
                if (valuation == null) {
                    // 新浪无数据(空串)时回退到东财旧接口
                    valuation = fetchValuationFromFundgz(fundCode);
                }
                if (valuation != null) {
                    emitter.onNext(valuation);
                    emitter.onComplete();
                } else {
                    emitter.onError(new RuntimeException("无法解析估值数据: " + fundCode));
                }
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    /**
     * 新浪盘中估值:返回 null 表示该基金无估值数据(如 QDII 停估/未收录)
     */
    private FundValuation fetchValuationFromSina(String fundCode) throws Exception {
        Request request = new Request.Builder()
                .url(String.format(SINA_GZ_URL, fundCode))
                .header("User-Agent", "Mozilla/5.0")
                .header("Referer", "https://finance.sina.com.cn/")
                .build();
        Response response = client.newCall(request).execute();
        String body = response.body() != null ? response.body().string() : "";

        Matcher matcher = SINA_GZ_PATTERN.matcher(body.trim());
        if (!matcher.find()) {
            return null;
        }
        String[] fields = matcher.group(1).split(",", -1);
        // 预期至少 8 段:名称,时间,估算净值,昨净值,昨净值,0,涨跌幅,净值日期
        if (fields.length < 8 || fields[2].isEmpty()) {
            return null;
        }

        FundValuation valuation = new FundValuation();
        valuation.setFundCode(fundCode);
        valuation.setName(fields[0]);
        valuation.setGztime(fields[1]);
        valuation.setGsz(fields[2]);
        valuation.setDwjz(fields[3]);
        valuation.setGszzl(fields[6]);
        valuation.setJzrq(fields[7]);
        return valuation;
    }

    /**
     * 东方财富 fundgz JSONP 接口(旧数据源,已停服,保留作回退)
     */
    private FundValuation fetchValuationFromFundgz(String fundCode) throws Exception {
        Request request = new Request.Builder()
                .url(String.format(FUNDGZ_URL, fundCode))
                .build();
        Response response = client.newCall(request).execute();
        String body = response.body() != null ? response.body().string() : "";

        Matcher matcher = JSONP_PATTERN.matcher(body);
        if (!matcher.find()) {
            return null;
        }
        return Constants.GSON.fromJson(matcher.group(1), FundValuation.class);
    }

    /**
     * 获取单位净值增长率（用于计算今日收益）
     * 解析 lsjz 页面获取最近两条净值记录，计算增长
     *
     * @param fundCode 基金代码
     * @return Observable<Double> 今日净值增长率
     */
    public Observable<NavResult> fetchLatestNav(String fundCode) {
        return Observable.create(emitter -> {
            try {
                String url = String.format(LSJZ_URL, fundCode);
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                String html = response.body() != null ? response.body().string() : "";

                Document doc = Jsoup.parse(html);
                Elements rows = doc.select("table#jzzx tbody tr");

                if (rows.size() >= 2) {
                    Elements latestCols = rows.get(0).select("td");
                    Elements prevCols = rows.get(1).select("td");

                    if (latestCols.size() >= 3 && prevCols.size() >= 3) {
                        NavResult result = new NavResult();
                        result.setDate(latestCols.get(0).text().trim());

                        try {
                            double latestNav = Double.parseDouble(latestCols.get(1).text().trim());
                            double prevNav = Double.parseDouble(prevCols.get(1).text().trim());
                            result.setNav(latestNav);
                            result.setGrowth(Math.round((latestNav - prevNav) / prevNav * 10000.0) / 100.0);
                        } catch (NumberFormatException ignored) {}

                        emitter.onNext(result);
                        emitter.onComplete();
                        return;
                    }
                }

                emitter.onError(new RuntimeException("无净值数据: " + fundCode));
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    /**
     * 获取基金持仓股票列表
     *
     * @param fundCode 基金代码
     * @return Observable<HoldingsResult>
     */
    public Observable<HoldingsResult> fetchHoldings(String fundCode) {
        return Observable.create(emitter -> {
            try {
                String url = String.format(JJCC_URL, fundCode);
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                String html = response.body() != null ? response.body().string() : "";

                Document doc = Jsoup.parse(html);
                HoldingsResult result = new HoldingsResult();
                List<HoldingsResult.HoldingStock> stocks = new ArrayList<>();

                Elements rows = doc.select("table#ccmx tbody tr");
                for (int i = 0; i < rows.size(); i++) {
                    Elements cols = rows.get(i).select("td");
                    if (cols.size() >= 5) {
                        HoldingsResult.HoldingStock stock = new HoldingsResult.HoldingStock();
                        stock.setCode(cols.get(1).text().trim());
                        stock.setName(cols.get(2).text().trim());
                        try {
                            stock.setPercent(Double.parseDouble(cols.get(3).text().replace("%", "").trim()));
                        } catch (NumberFormatException ignored) {}
                        try {
                            stock.setMarketValue(Double.parseDouble(cols.get(4).text().trim()));
                        } catch (NumberFormatException ignored) {}
                        stocks.add(stock);
                    }
                }

                result.setStocks(stocks);

                // 尝试获取报告日期
                Elements header = doc.select("div#ccmx_tablediv .left");
                if (!header.isEmpty()) {
                    String headerText = header.first().text();
                    Matcher dateMatcher = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})").matcher(headerText);
                    if (dateMatcher.find()) {
                        result.setReportDate(dateMatcher.group(1));
                    }
                }

                emitter.onNext(result);
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    /**
     * 获取基金历史估值数据（用于图表）
     *
     * @param fundCode 基金代码
     * @return Observable<List<NavResult>>
     */
    public Observable<List<NavResult>> fetchFundHistory(String fundCode) {
        return Observable.create(emitter -> {
            try {
                String url = String.format(LSJZ_URL, fundCode);
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                String html = response.body() != null ? response.body().string() : "";

                Document doc = Jsoup.parse(html);
                Elements rows = doc.select("table#jzzx tbody tr");

                List<NavResult> history = new ArrayList<>();
                for (int i = 0; i < rows.size(); i++) {
                    Elements cols = rows.get(i).select("td");
                    if (cols.size() >= 3) {
                        NavResult point = new NavResult();
                        point.setDate(cols.get(0).text().trim());
                        try {
                            point.setNav(Double.parseDouble(cols.get(1).text().trim()));
                        } catch (NumberFormatException ignored) {}
                        history.add(point);
                    }
                }

                emitter.onNext(history);
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }
}
