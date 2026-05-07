package com.fund.guguji.data.model;

import java.util.List;

/**
 * 基金持仓股票数据
 */
public class HoldingsResult {
    private List<HoldingStock> stocks;
    private String reportDate;       // 报告日期 YYYY-MM-DD
    private boolean isLastQuarter;   // 是否最新季度

    public List<HoldingStock> getStocks() { return stocks; }
    public void setStocks(List<HoldingStock> stocks) { this.stocks = stocks; }
    public String getReportDate() { return reportDate; }
    public void setReportDate(String reportDate) { this.reportDate = reportDate; }
    public boolean isLastQuarter() { return isLastQuarter; }
    public void setLastQuarter(boolean lastQuarter) { isLastQuarter = lastQuarter; }

    public static class HoldingStock {
        private String code;         // 股票代码
        private String name;         // 股票名称
        private double percent;      // 持仓占比
        private double marketValue;  // 持仓市值(万元)
        private double shares;       // 持股数(万股)

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public double getPercent() { return percent; }
        public void setPercent(double percent) { this.percent = percent; }
        public double getMarketValue() { return marketValue; }
        public void setMarketValue(double marketValue) { this.marketValue = marketValue; }
        public double getShares() { return shares; }
        public void setShares(double shares) { this.shares = shares; }
    }
}
