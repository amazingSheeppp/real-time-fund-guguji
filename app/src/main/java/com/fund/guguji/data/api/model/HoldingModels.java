package com.fund.guguji.data.api.model;

import java.util.List;

/**
 * 持仓同步与朋友聚合模型
 * 对应后端 schemas/holding.py。
 */
public final class HoldingModels {

    private HoldingModels() {}

    // ── 请求 ──

    /** 单只持仓基金基础信息 */
    public static class HoldingItem {
        public String fundCode;
        public String fundName;

        public HoldingItem(String fundCode, String fundName) {
            this.fundCode = fundCode;
            this.fundName = fundName;
        }
    }

    /** 全量覆盖批量同步持仓请求 */
    public static class HoldingBatchSyncRequest {
        public List<HoldingItem> holdings;

        public HoldingBatchSyncRequest(List<HoldingItem> holdings) {
            this.holdings = holdings;
        }
    }

    // ── 响应 ──

    /** 我的持仓项(带更新时间) */
    public static class MyHoldingItem {
        private String fundCode;
        private String fundName;
        private String updatedAt;

        public String getFundCode() { return fundCode; }
        public String getFundName() { return fundName; }
        public String getUpdatedAt() { return updatedAt; }
    }

    /** 批量同步响应 */
    public static class HoldingBatchSyncResponse {
        private int totalSynced;

        public int getTotalSynced() { return totalSynced; }
    }

    /** 朋友信息及其持仓基金 */
    public static class FriendHoldingItem {
        private long userId;
        private String nickname;
        private boolean isOwner;
        private List<HoldingItem> holdings;

        public long getUserId() { return userId; }
        public String getNickname() { return nickname; }
        public boolean isOwner() { return isOwner; }
        public List<HoldingItem> getHoldings() { return holdings; }
    }

    /** 圈子内朋友持仓聚合响应 */
    public static class FriendHoldingsResponse {
        private long circleId;
        private String circleName;
        private List<FriendHoldingItem> friends;

        public long getCircleId() { return circleId; }
        public String getCircleName() { return circleName; }
        public List<FriendHoldingItem> getFriends() { return friends; }
    }
}