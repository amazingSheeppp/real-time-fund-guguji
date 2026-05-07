package com.fund.guguji.util;

import com.fund.guguji.data.model.ProfitResult;

/**
 * 收益计算工具类
 * 对应 Web 项目中的收益计算逻辑
 */
public class ProfitCalculator {

    private ProfitCalculator() {}

    /**
     * 计算持仓收益
     *
     * @param shares      持有份额
     * @param costPerShare 成本价（单位净值）
     * @param currentNav  当前估值/净值
     * @param todayGrowth 今日涨跌幅（如 1.5 表示涨 1.5%）
     * @return ProfitResult
     */
    public static ProfitResult calculate(double shares, double costPerShare,
                                          double currentNav, double todayGrowth) {
        ProfitResult result = new ProfitResult();

        double amount = shares * currentNav;             // 持仓金额
        double costAmount = shares * costPerShare;       // 成本金额

        result.setAmount(amount);
        result.setCostAmount(costAmount);

        // 总收益
        double profitTotal = amount - costAmount;
        result.setProfitTotal(profitTotal);

        // 总收益率
        if (costAmount > 0) {
            result.setProfitTotalPercent(profitTotal / costAmount * 100.0);
        } else {
            result.setProfitTotalPercent(0);
        }

        // 今日收益 = 持有份额 * 当前净值 * 今日涨跌幅 / (1 + 今日涨跌幅)
        // 推导: 昨日净值 = 当前净值 / (1 + todayGrowth/100)
        // 今日收益 = 份额 * (当前净值 - 昨日净值)
        if (Math.abs(todayGrowth) > 0.001) {
            double yesterdayNav = currentNav / (1 + todayGrowth / 100.0);
            double profitToday = shares * (currentNav - yesterdayNav);
            result.setProfitToday(profitToday);

            if (costAmount > 0) {
                result.setProfitTodayPercent(profitToday / costAmount * 100.0);
            } else {
                result.setProfitTodayPercent(0);
            }
        } else {
            result.setProfitToday(0);
            result.setProfitTodayPercent(0);
        }

        return result;
    }
}
