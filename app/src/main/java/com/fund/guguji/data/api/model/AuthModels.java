package com.fund.guguji.data.api.model;

/**
 * 认证模块请求与响应模型
 * 对应后端 schemas/auth.py,字段名由 Gson 的 LOWER_CASE_WITH_UNDERSCORES 策略映射。
 */
public final class AuthModels {

    private AuthModels() {}

    // ── 请求 ──

    /** 发送验证码请求 */
    public static class SendCodeRequest {
        public String email;

        public SendCodeRequest(String email) {
            this.email = email;
        }
    }

    /** 验证码注册/登录请求(password 选填) */
    public static class LoginByCodeRequest {
        public String email;
        public String code;
        public String password;

        public LoginByCodeRequest(String email, String code, String password) {
            this.email = email;
            this.code = code;
            this.password = password;
        }
    }

    /** 账号密码登录请求 */
    public static class LoginByPasswordRequest {
        public String email;
        public String password;

        public LoginByPasswordRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    /** 刷新 Token 请求 */
    public static class RefreshTokenRequest {
        public String refreshToken;

        public RefreshTokenRequest(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }

    // ── 响应 ──

    /** 发送验证码响应 */
    public static class SendCodeResponse {
        private String email;
        private int expireSeconds;
        private int resendIntervalSeconds;

        public String getEmail() { return email; }
        public int getExpireSeconds() { return expireSeconds; }
        public int getResendIntervalSeconds() { return resendIntervalSeconds; }
    }

    /** Token 携带的用户精简信息 */
    public static class TokenUser {
        private long id;
        private String email;
        private String nickname;
        private boolean hasPassword;
        private String createdAt;

        public long getId() { return id; }
        public String getEmail() { return email; }
        public String getNickname() { return nickname; }
        public boolean isHasPassword() { return hasPassword; }
        public String getCreatedAt() { return createdAt; }
    }

    /** 双 Token 响应 */
    public static class TokenResponse {
        private String tokenType;
        private String accessToken;
        private int expiresIn;
        private String refreshToken;
        private TokenUser user;

        public String getTokenType() { return tokenType; }
        public String getAccessToken() { return accessToken; }
        public int getExpiresIn() { return expiresIn; }
        public String getRefreshToken() { return refreshToken; }
        public TokenUser getUser() { return user; }
    }
}