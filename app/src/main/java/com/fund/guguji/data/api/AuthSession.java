package com.fund.guguji.data.api;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 登录会话持久化
 * 使用 SharedPreferences 保存服务端签发的双 Token 与当前用户信息。
 */
public class AuthSession {

    private static final String PREFS_NAME = "guguji_auth";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_NICKNAME = "nickname";

    private final SharedPreferences prefs;

    private AuthSession(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public static AuthSession newInstance(Context context) {
        return new AuthSession(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE));
    }

    /** 是否已登录(存在 access token) */
    public boolean isLoggedIn() {
        return prefs.contains(KEY_ACCESS_TOKEN);
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public long getUserId() {
        return prefs.getLong(KEY_USER_ID, -1);
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public String getNickname() {
        return prefs.getString(KEY_NICKNAME, "");
    }

    /** 保存登录会话(access/refresh token 不可为空的校验交由调用方) */
    public void saveSession(String accessToken, String refreshToken,
                            long userId, String email, String nickname) {
        SharedPreferences.Editor editor = prefs.edit();
        if (accessToken != null) {
            editor.putString(KEY_ACCESS_TOKEN, accessToken);
        }
        if (refreshToken != null) {
            editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        }
        editor.putLong(KEY_USER_ID, userId);
        editor.putString(KEY_EMAIL, email == null ? "" : email);
        editor.putString(KEY_NICKNAME, nickname == null ? "" : nickname);
        editor.apply();
    }

    /** 仅覆盖 access token(用于刷新令牌后) */
    public void updateAccessToken(String accessToken) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, accessToken).apply();
    }

    /** 清除登录态 */
    public void clear() {
        prefs.edit().clear().apply();
    }
}