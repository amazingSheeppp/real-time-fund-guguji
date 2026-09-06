package com.fund.guguji.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 基金实时估值数据
 * 原对应 fundgz JSONP 接口,现主源为新浪 fu_ 盘中估值接口(字段一致)
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

    public void setFundCode(String fundCode) { this.fundCode = fundCode; }
    public void setName(String name) { this.name = name; }
    public void setJzrq(String jzrq) { this.jzrq = jzrq; }
    public void setDwjz(String dwjz) { this.dwjz = dwjz; }
    public void setGsz(String gsz) { this.gsz = gsz; }
    public void setGszzl(String gszzl) { this.gszzl = gszzl; }
    public void setGztime(String gztime) { this.gztime = gztime; }
}
