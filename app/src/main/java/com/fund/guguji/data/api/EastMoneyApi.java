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
    // F10 持仓数据端点(ccmx 页面表格已改为异步加载,不再随 HTML 输出)
    private static final String JJCC_URL = "https://fundf10.eastmoney.com/FundArchivesDatas.aspx?type=jjcc&code=%s&topline=10";
    // 官方历史净值 JSON 接口(收盘后同步官方净值用,pageSize=2 取最近两条)
    private static final String LSJZ_API_URL = "https://api.fund.eastmoney.com/f10/lsjz?fundCode=%s&pageIndex=1&pageSize=2";
    // 全量历史净值 JS 文件(数据点: x=毫秒时间戳, y=单位净值)
    private static final String PINGZHONG_DATA_URL = "https://fund.eastmoney.com/pingzhongdata/%s.js";

    private static final Pattern JSONP_PATTERN = Pattern.compile("jsonpgz\\((.+?)\\);?");

    // 新浪返回形如:var hq_str_fu_005827="名称,16:04:00,估算净值,昨净值,昨净值,0,涨跌幅%,净值日期,...";
    private static final Pattern SINA_GZ_PATTERN = Pattern.compile("\"([^\"]*)\"\\s*;?\\s*$");

    // pingzhongdata 中单位净值趋势数组
    private static final Pattern NET_WORTH_TREND_PATTERN =
            Pattern.compile("Data_netWorthTrend\\s*=\\s*(\\[.*?\\]);");

    // apidata 响应中持仓明细的 HTML 片段(content 内引号均为单引号,双引号只出现在首尾)
    private static final Pattern APIDATA_CONTENT_PATTERN =
            Pattern.compile("content:\"(.*?)\",\\s*arryear", Pattern.DOTALL);

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
     * 获取官方最新净值及日涨幅(收盘后同步用)
     * 来自 lsjz JSON 接口,取最近两条净值;JZZZL 为官方日涨跌幅
     *
     * @param fundCode 基金代码
     * @return Observable<NavResult> date=净值日期 nav=单位净值 growth=日涨跌幅%
     */
    public Observable<NavResult> fetchOfficialNav(String fundCode) {
        return Observable.create(emitter -> {
            try {
                String url = String.format(LSJZ_API_URL, fundCode);
                Request request = new Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0")
                        .header("Referer", "http://fundf10.eastmoney.com/jjjz_" + fundCode + ".html")
                        .build();
                Response response = client.newCall(request).execute();
                String body = response.body() != null ? response.body().string() : "";

                com.google.gson.JsonObject root = Constants.GSON.fromJson(body, com.google.gson.JsonObject.class);
                com.google.gson.JsonObject data = root == null ? null : root.getAsJsonObject("Data");
                com.google.gson.JsonArray list = data == null ? null : data.getAsJsonArray("LSJZList");
                if (list == null || list.size() == 0) {
                    emitter.onError(new RuntimeException("无官方净值数据: " + fundCode));
                    return;
                }

                com.google.gson.JsonObject latest = list.get(0).getAsJsonObject();
                NavResult result = new NavResult();
                result.setDate(latest.get("FSRQ").getAsString());
                try {
                    result.setNav(Double.parseDouble(latest.get("DWJZ").getAsString()));
                } catch (Exception ignored) {}
                // 官方日涨幅字段缺失时,用最近两条净值自行计算
                try {
                    result.setGrowth(Double.parseDouble(latest.get("JZZZL").getAsString()));
                } catch (Exception ignored) {
                    if (list.size() >= 2 && result.getNav() > 0) {
                        try {
                            double prev = Double.parseDouble(
                                    list.get(1).getAsJsonObject().get("DWJZ").getAsString());
                            if (prev > 0) {
                                result.setGrowth(Math.round((result.getNav() - prev) / prev * 10000.0) / 100.0);
                            }
                        } catch (Exception ignored2) {}
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
     * 获取基金持仓股票列表(前十大重仓股)
     * 数据来自 F10 jjcc 数据端点,返回 apidata={content:"<table...>"},最新报告期在前
     *
     * @param fundCode 基金代码
     * @return Observable<HoldingsResult>
     */
    public Observable<HoldingsResult> fetchHoldings(String fundCode) {
        return Observable.create(emitter -> {
            try {
                String url = String.format(JJCC_URL, fundCode);
                Request request = new Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0")
                        .header("Referer", "https://fundf10.eastmoney.com/ccmx_" + fundCode + ".html")
                        .build();
                Response response = client.newCall(request).execute();
                String body = response.body() != null ? response.body().string() : "";

                Matcher contentMatcher = APIDATA_CONTENT_PATTERN.matcher(body);
                if (!contentMatcher.find()) {
                    emitter.onError(new RuntimeException("无持仓数据: " + fundCode));
                    return;
                }
                Document doc = Jsoup.parse(contentMatcher.group(1));

                HoldingsResult result = new HoldingsResult();
                List<HoldingsResult.HoldingStock> stocks = new ArrayList<>();

                // 最新报告期在第一个 boxitem 中
                Elements boxItems = doc.select("div.boxitem");
                if (boxItems.isEmpty()) {
                    emitter.onError(new RuntimeException("无持仓数据: " + fundCode));
                    return;
                }
                Elements rows = boxItems.first().select("table tbody tr");
                for (org.jsoup.nodes.Element row : rows) {
                    Elements cols = row.select("td");
                    if (cols.size() >= 9) {
                        HoldingsResult.HoldingStock stock = new HoldingsResult.HoldingStock();
                        // 列: 序号,代码,名称,,,(链接),占净值比%,持股数(万股),持仓市值(万元)
                        stock.setCode(cols.get(1).text().trim());
                        stock.setName(cols.get(2).text().trim());
                        try {
                            stock.setPercent(Double.parseDouble(
                                    cols.get(6).text().replace("%", "").trim()));
                        } catch (NumberFormatException ignored) {}
                        try {
                            stock.setShares(Double.parseDouble(
                                    cols.get(7).text().replace(",", "").trim()));
                        } catch (NumberFormatException ignored) {}
                        try {
                            stock.setMarketValue(Double.parseDouble(
                                    cols.get(8).text().replace(",", "").trim()));
                        } catch (NumberFormatException ignored) {}
                        stocks.add(stock);
                    }
                }

                if (stocks.isEmpty()) {
                    emitter.onError(new RuntimeException("无持仓数据: " + fundCode));
                    return;
                }

                result.setStocks(stocks);

                // 报告日期:"截止至:2026-06-30"
                Matcher dateMatcher = Pattern.compile("截止至：<font[^>]*>(\\d{4}-\\d{2}-\\d{2})</font>")
                        .matcher(contentMatcher.group(1));
                if (dateMatcher.find()) {
                    result.setReportDate(dateMatcher.group(1));
                }

                emitter.onNext(result);
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    /**
     * 获取基金全量历史净值(来自 pingzhongdata JS 文件)
     * 单次请求包含基金成立以来全部净值点,覆盖近1年图表绰绰有余
     *
     * @param fundCode 基金代码
     * @return Observable<List<NavResult>> 按日期升序
     */
    public Observable<List<NavResult>> fetchNavHistory(String fundCode) {
        return Observable.create(emitter -> {
            try {
                String url = String.format(PINGZHONG_DATA_URL, fundCode);
                Request request = new Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0")
                        .header("Referer", "https://fund.eastmoney.com/")
                        .build();
                Response response = client.newCall(request).execute();
                String body = response.body() != null ? response.body().string() : "";

                Matcher matcher = NET_WORTH_TREND_PATTERN.matcher(body);
                if (!matcher.find()) {
                    emitter.onError(new RuntimeException("无历史净值数据: " + fundCode));
                    return;
                }

                // 每个元素: {"x":1536...毫秒时间戳,"y":1.0单位净值,...}
                com.google.gson.JsonArray arr = Constants.GSON.fromJson(matcher.group(1),
                        com.google.gson.JsonArray.class);
                List<NavResult> history = new ArrayList<>(arr.size());
                java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
                for (com.google.gson.JsonElement el : arr) {
                    com.google.gson.JsonObject obj = el.getAsJsonObject();
                    NavResult point = new NavResult();
                    point.setDate(fmt.format(new java.util.Date(obj.get("x").getAsLong())));
                    point.setNav(obj.get("y").getAsDouble());
                    history.add(point);
                }

                if (history.isEmpty()) {
                    emitter.onError(new RuntimeException("无历史净值数据: " + fundCode));
                    return;
                }

                emitter.onNext(history);
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    /**
     * 获取基金历史净值数据（用于图表）(旧 jjjz 页面,仅含近期数据)
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
