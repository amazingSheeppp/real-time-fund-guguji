package com.fund.guguji.util;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * 市场交易时间工具类
 * 判定当前是否处于 A 股/公募基金盘中交易时间
 */
public class MarketUtils {

    private MarketUtils() {}

    /**
     * 判断当前时间是否属于 A 股连续竞价交易时段 (北京时间)
     * 规则:
     * 1. 周一至周五 (排除周末)
     * 2. 早盘时段: 09:30 ~ 11:30
     * 3. 午盘时段: 13:00 ~ 15:00
     *
     * @return true 表示盘中开盘交易时间，false 表示非交易时间
     */
    public static boolean isTradingTime() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        // 周末(周六、周日)为非交易时间
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            return false;
        }

        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int currentMinuteOfDay = hour * 60 + minute;

        // 早盘: 09:30 ~ 11:30 (570 分钟 ~ 690 分钟)
        boolean morningSession = currentMinuteOfDay >= (9 * 60 + 30) && currentMinuteOfDay <= (11 * 60 + 30);
        // 午盘: 13:00 ~ 15:00 (780 分钟 ~ 900 分钟)
        boolean afternoonSession = currentMinuteOfDay >= (13 * 60) && currentMinuteOfDay <= (15 * 60);

        return morningSession || afternoonSession;
    }
}
