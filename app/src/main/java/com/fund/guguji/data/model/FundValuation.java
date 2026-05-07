package com.fund.guguji.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 基金实时估值数据 (对应 fundgz JSONP 接口)
 */
public class FundValuation {
    @SerializedName("fundcode")
    private String fundCode;

    @SerializedName("name")
    private String name;

    @SerializedName("jzrq")
    private String jzrq;         // 净值日期 YYYY-MM-DD

    @SerializedName("dwjz")
    private String dwjz;         // 单位净值

    @SerializedName("gsz")
    private String gsz;          // 估算净值

    @SerializedName("gszzl")
    private String gszzl;        // 估算涨跌幅 %

    @SerializedName("gztime")
    private String gztime;       // 估值时间

    public String getFundCode() { return fundCode; }
    public String getName() { return name; }
    public String getJzrq() { return jzrq; }
    public String getDwjz() { return dwjz; }
    public String getGsz() { return gsz; }
    public String getGszzl() { return gszzl; }
    public String getGztime() { return gztime; }
}
