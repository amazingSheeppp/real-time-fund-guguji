package com.fund.guguji.data.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 估值分时数据实体（用于分时图）
 * 对应 Web 版 valuationTimeseries.js
 */
@Entity(tableName = "valuation_timeseries")
public class ValuationPointEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String fundCode;
    private String time;     // "HH:mm"
    private double value;    // 估值净值
    private String date;     // "YYYY-MM-DD"

    public ValuationPointEntity(String fundCode, String time, double value, String date) {
        this.fundCode = fundCode;
        this.time = time;
        this.value = value;
        this.date = date;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getFundCode() { return fundCode; }
    public void setFundCode(String fundCode) { this.fundCode = fundCode; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}
