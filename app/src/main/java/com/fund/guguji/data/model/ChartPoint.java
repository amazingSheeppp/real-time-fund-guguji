package com.fund.guguji.data.model;

/**
 * 估值走势图表数据点
 */
public class ChartPoint {
    private String date;     // "YYYY-MM-DD HH:mm" 或 "YYYY-MM-DD"
    private double value;    // 估值/净值

    public ChartPoint() {}

    public ChartPoint(String date, double value) {
        this.date = date;
        this.value = value;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
}
