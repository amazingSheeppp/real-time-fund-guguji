package com.fund.guguji.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 基金信息实体
 * 对应 Web 版 fetchFundData 返回的基金对象
 */
@Entity(tableName = "funds")
public class FundEntity {
    @PrimaryKey
    @NonNull
    private String code;                // 基金代码
    private String name;                // 基金名称
    private String dwjz;                // 单位净值
    private String gsz;                 // 估算净值（实时估值）
    private String gztime;              // 估值时间 (YYYY-MM-DD HH:mm)
    private String jzrq;                // 净值日期 (YYYY-MM-DD)
    private Double gszzl;               // 估算涨跌幅百分比
    private Double zzl;                 // 真实涨跌幅百分比（来自历史净值）
    private boolean noValuation;        // true 表示无实时估值
    private String holdingsJson;        // 前10重仓持仓 JSON 序列化
    private String holdingsReportDate;  // 持仓报告日期
    private boolean holdingsIsLastQuarter; // 是否为最近一个季度末
    private int orderIndex;             // 排序序号

    public FundEntity(@NonNull String code, String name) {
        this.code = code;
        this.name = name;
        this.noValuation = false;
        this.holdingsIsLastQuarter = false;
    }

    @NonNull
    public String getCode() { return code; }
    public void setCode(@NonNull String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDwjz() { return dwjz; }
    public void setDwjz(String dwjz) { this.dwjz = dwjz; }

    public String getGsz() { return gsz; }
    public void setGsz(String gsz) { this.gsz = gsz; }

    public String getGztime() { return gztime; }
    public void setGztime(String gztime) { this.gztime = gztime; }

    public String getJzrq() { return jzrq; }
    public void setJzrq(String jzrq) { this.jzrq = jzrq; }

    public Double getGszzl() { return gszzl; }
    public void setGszzl(Double gszzl) { this.gszzl = gszzl; }

    public Double getZzl() { return zzl; }
    public void setZzl(Double zzl) { this.zzl = zzl; }

    public boolean isNoValuation() { return noValuation; }
    public void setNoValuation(boolean noValuation) { this.noValuation = noValuation; }

    public String getHoldingsJson() { return holdingsJson; }
    public void setHoldingsJson(String holdingsJson) { this.holdingsJson = holdingsJson; }

    public String getHoldingsReportDate() { return holdingsReportDate; }
    public void setHoldingsReportDate(String holdingsReportDate) { this.holdingsReportDate = holdingsReportDate; }

    public boolean isHoldingsIsLastQuarter() { return holdingsIsLastQuarter; }
    public void setHoldingsIsLastQuarter(boolean holdingsIsLastQuarter) { this.holdingsIsLastQuarter = holdingsIsLastQuarter; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
}
