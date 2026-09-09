package com.fund.guguji.data.api.model;

import java.util.List;

/**
 * 圈子模块请求与响应模型
 * 对应后端 schemas/circle.py。
 */
public final class CircleModels {

    private CircleModels() {}

    // ── 请求 ──

    /** 创建圈子请求 */
    public static class CircleCreateRequest {
        public String name;
        public String description;

        public CircleCreateRequest(String name, String description) {
            this.name = name;
            this.description = description;
        }
    }

    /** 审批入圈申请请求 */
    public static class CircleAuditRequest {
        public int applicationId;
        public String action;

        public CircleAuditRequest(int applicationId, String action) {
            this.applicationId = applicationId;
            this.action = action;
        }
    }

    // ── 响应 ──

    /** 创建圈子响应 */
    public static class CircleCreateResponse {
        private long id;
        private String circleCode;
        private String name;
        private String description;
        private long ownerId;
        private int memberCount;
        private int maxMembers;
        private String createdAt;

        public long getId() { return id; }
        public String getCircleCode() { return circleCode; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public long getOwnerId() { return ownerId; }
        public int getMemberCount() { return memberCount; }
        public int getMaxMembers() { return maxMembers; }
        public String getCreatedAt() { return createdAt; }
    }

    /** 我加入的圈子条目 */
    public static class MyCircleItem {
        private long circleId;
        private String circleCode;
        private String name;
        private String description;
        private String myRole;
        private int memberCount;
        private int pendingAuditCount;
        private String createdAt;

        public long getCircleId() { return circleId; }
        public String getCircleCode() { return circleCode; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getMyRole() { return myRole; }
        public int getMemberCount() { return memberCount; }
        public int getPendingAuditCount() { return pendingAuditCount; }
        public String getCreatedAt() { return createdAt; }
    }

    /** 按圈子码搜索响应 */
    public static class CircleSearchResponse {
        private long circleId;
        private String circleCode;
        private String name;
        private String description;
        private String ownerNickname;
        private int memberCount;
        private int maxMembers;
        private String myStatus;

        public long getCircleId() { return circleId; }
        public String getCircleCode() { return circleCode; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getOwnerNickname() { return ownerNickname; }
        public int getMemberCount() { return memberCount; }
        public int getMaxMembers() { return maxMembers; }
        public String getMyStatus() { return myStatus; }
    }

    /** 申请入圈响应 */
    public static class CircleApplyResponse {
        private long circleId;
        private String status;
        private String appliedAt;

        public long getCircleId() { return circleId; }
        public String getStatus() { return status; }
        public String getAppliedAt() { return appliedAt; }
    }

    /** 待审批成员条目 */
    public static class CircleAuditItem {
        private long applicationId;
        private long userId;
        private String nickname;
        private String appliedAt;

        public long getApplicationId() { return applicationId; }
        public long getUserId() { return userId; }
        public String getNickname() { return nickname; }
        public String getAppliedAt() { return appliedAt; }
    }

    /** 审批响应 */
    public static class CircleAuditResponse {
        private long applicationId;
        private String action;
        private String status;

        public long getApplicationId() { return applicationId; }
        public String getAction() { return action; }
        public String getStatus() { return status; }
    }

    /** 圈子成员条目 */
    public static class CircleMemberItem {
        private long userId;
        private String nickname;
        private String role;
        private String joinedAt;

        public long getUserId() { return userId; }
        public String getNickname() { return nickname; }
        public String getRole() { return role; }
        public String getJoinedAt() { return joinedAt; }
    }

    /** 圈子成员列表响应 */
    public static class CircleMembersResponse {
        private long circleId;
        private String circleName;
        private List<CircleMemberItem> members;

        public long getCircleId() { return circleId; }
        public String getCircleName() { return circleName; }
        public List<CircleMemberItem> getMembers() { return members; }
    }
}