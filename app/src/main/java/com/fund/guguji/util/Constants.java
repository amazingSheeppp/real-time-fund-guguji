package com.fund.guguji.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 应用常量配置
 */
public class Constants {

    private Constants() {}

    public static final Gson GSON = new GsonBuilder().create();

    // 东方财富基金估值接口域名(fundgz 已停服,仅作回退)
    public static final String EAST_MONEY_HOST = "fundgz.1234567.com.cn";
    public static final String EAST_MONEY_FUND_HOST = "fund.eastmoney.com";

    // 新浪盘中估值接口域名(当前估值主源)
    public static final String SINA_HQ_HOST = "hq.sinajs.cn";

    // 腾讯行情接口域名
    public static final String TENCENT_QUOTE_HOST = "qt.gtimg.cn";

    // 网络请求超时
    public static final int CONNECT_TIMEOUT = 15;
    public static final int READ_TIMEOUT = 15;

    // 数据库
    public static final String DB_NAME = "guguji_db";

    // 估值刷新间隔（毫秒）
    public static final long REFRESH_INTERVAL_MS = 30_000;

    // 图表时间范围
    public static final int CHART_DAYS_1M = 30;
    public static final int CHART_DAYS_3M = 90;
    public static final int CHART_DAYS_6M = 180;
    public static final int CHART_DAYS_1Y = 365;

    // 股票市场代码前缀
    public static final String SZ_PREFIX = "sz";
    public static final String SH_PREFIX = "sh";

    // 收益计算默认值
    public static final double DEFAULT_COST = 0.0;
    public static final int SCALE_PERCENT = 100;
}
