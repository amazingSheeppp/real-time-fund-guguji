package com.fund.guguji.data.model;

/**
 * 收益计算结果
 */
public class ProfitResult {
    private double amount;         // 持仓金额 (份额 x 估值)
    private double costAmount;     // 成本金额 (份额 x 成本价)
    private double profitToday;    // 今日收益
    private double profitTotal;    // 总收益 (持仓金额 - 成本金额)
    private double profitTodayPercent;  // 今日收益率
    private double profitTotalPercent;  // 总收益率

    public ProfitResult() {}

    public ProfitResult(double amount, double costAmount, double profitToday, double profitTotal,
                        double profitTodayPercent, double profitTotalPercent) {
        this.amount = amount;
        this.costAmount = costAmount;
        this.profitToday = profitToday;
        this.profitTotal = profitTotal;
        this.profitTodayPercent = profitTodayPercent;
        this.profitTotalPercent = profitTotalPercent;
    }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public double getCostAmount() { return costAmount; }
    public void setCostAmount(double costAmount) { this.costAmount = costAmount; }
    public double getProfitToday() { return profitToday; }
    public void setProfitToday(double profitToday) { this.profitToday = profitToday; }
    public double getProfitTotal() { return profitTotal; }
    public void setProfitTotal(double profitTotal) { this.profitTotal = profitTotal; }
    public double getProfitTodayPercent() { return profitTodayPercent; }
    public void setProfitTodayPercent(double profitTodayPercent) { this.profitTodayPercent = profitTodayPercent; }
    public double getProfitTotalPercent() { return profitTotalPercent; }
    public void setProfitTotalPercent(double profitTotalPercent) { this.profitTotalPercent = profitTotalPercent; }
}
