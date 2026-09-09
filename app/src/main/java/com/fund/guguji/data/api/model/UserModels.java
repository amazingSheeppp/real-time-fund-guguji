package com.fund.guguji.data.api.model;

/**
 * 用户模块请求与响应模型
 * 对应后端 schemas/user.py。
 */
public final class UserModels {

    private UserModels() {}

    /** 更新昵称请求 */
    public static class UserUpdateRequest {
        public String nickname;

        public UserUpdateRequest(String nickname) {
            this.nickname = nickname;
        }
    }

    /** 修改/设置密码请求(old_password 首次设置可为空) */
    public static class UserPasswordUpdateRequest {
        public String oldPassword;
        public String newPassword;

        public UserPasswordUpdateRequest(String oldPassword, String newPassword) {
            this.oldPassword = oldPassword;
            this.newPassword = newPassword;
        }
    }

    /** 个人资料详情 */
    public static class UserProfile {
        private long id;
        private String email;
        private String nickname;
        private boolean hasPassword;
        private int joinedCirclesCount;
        private int createdCirclesCount;
        private int holdingFundsCount;
        private String createdAt;

        public long getId() { return id; }
        public String getEmail() { return email; }
        public String getNickname() { return nickname; }
        public boolean isHasPassword() { return hasPassword; }
        public int getJoinedCirclesCount() { return joinedCirclesCount; }
        public int getCreatedCirclesCount() { return createdCirclesCount; }
        public int getHoldingFundsCount() { return holdingFundsCount; }
        public String getCreatedAt() { return createdAt; }
    }
}