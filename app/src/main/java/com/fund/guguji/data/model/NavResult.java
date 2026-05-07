package com.fund.guguji.data.model;

/**
 * 历史净值数据（解析自 lsjz 页面 HTML 或 JSON）
 */
public class NavResult {
    private String date;       // YYYY-MM-DD
    private double nav;        // 单位净值
    private double accNav;     // 累计净值
    private double growth;     // 日涨跌幅 %

    public NavResult() {}

    public NavResult(String date, double nav, double accNav, double growth) {
        this.date = date;
        this.nav = nav;
        this.accNav = accNav;
        this.growth = growth;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public double getNav() { return nav; }
    public void setNav(double nav) { this.nav = nav; }
    public double getAccNav() { return accNav; }
    public void setAccNav(double accNav) { this.accNav = accNav; }
    public double getGrowth() { return growth; }
    public void setGrowth(double growth) { this.growth = growth; }
}
