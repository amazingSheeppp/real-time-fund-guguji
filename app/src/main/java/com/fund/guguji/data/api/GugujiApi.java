package com.fund.guguji.data.api;

import android.text.TextUtils;

import com.fund.guguji.data.api.model.ApiResponse;
import com.fund.guguji.data.api.model.AuthModels;
import com.fund.guguji.data.api.model.CircleModels;
import com.fund.guguji.data.api.model.HoldingModels;
import com.fund.guguji.data.api.model.TypeTokens;
import com.fund.guguji.data.api.model.UserModels;
import com.fund.guguji.data.api.model.VersionModels;
import com.fund.guguji.util.Constants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 咕咕鸡自建后端接口客户端
 * 覆盖 GugujiServer 的全部端点:认证、用户、圈子、持仓、版本检测。
 *
 * 服务端统一返回 HTTP 200,业务码在响应体 code 字段内:
 *   code == 200 视为成功,否则包装为 {@link ApiException} 抛给 onError。
 *
 * 说明:本层只负责请求与数据解析,登录态由调用方在拿到 Token 后写入 {@link AuthSession};
 * 需鉴权的请求由 {@link #execute} 自动从 AuthSession 附加 Bearer Token。
 */
public class GugujiApi {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final AuthSession authSession;
    private final Gson gson;

    public GugujiApi(OkHttpClient client, AuthSession authSession) {
        this.client = client;
        this.authSession = authSession;
        // 服务端字段为 snake_case(如 fund_code),客户端模型为 camelCase,统一由命名策略映射
        this.gson = new GsonBuilder()
                .setFieldNamingPolicy(com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
    }

    // ── 认证模块 ──

    /** 发送邮箱验证码 */
    public Observable<AuthModels.SendCodeResponse> sendCode(String email) {
        return execute("POST", "/auth/send-code", false,
                new AuthModels.SendCodeRequest(email), AuthModels.SendCodeResponse.class);
    }

    /** 邮箱验证码注册与登录 */
    public Observable<AuthModels.TokenResponse> loginByCode(String email, String code, String password) {
        return execute("POST", "/auth/login-by-code", false,
                new AuthModels.LoginByCodeRequest(email, code, password), AuthModels.TokenResponse.class);
    }

    /** 账号密码直接登录 */
    public Observable<AuthModels.TokenResponse> loginByPassword(String email, String password) {
        return execute("POST", "/auth/login-by-password", false,
                new AuthModels.LoginByPasswordRequest(email, password), AuthModels.TokenResponse.class);
    }

    /** 刷新 Access Token */
    public Observable<AuthModels.TokenResponse> refreshToken(String refreshToken) {
        return execute("POST", "/auth/refresh-token", false,
                new AuthModels.RefreshTokenRequest(refreshToken), AuthModels.TokenResponse.class);
    }

    // ── 用户模块 ──

    /** 获取当前登录用户信息 */
    public Observable<UserModels.UserProfile> getMe() {
        return execute("GET", "/user/me", true, null, UserModels.UserProfile.class);
    }

    /** 更新用户昵称 */
    public Observable<UserModels.UserProfile> updateNickname(String nickname) {
        return execute("PUT", "/user/me", true,
                new UserModels.UserUpdateRequest(nickname), UserModels.UserProfile.class);
    }

    /** 修改/设置密码(oldPassword 首次设置为 null) */
    public Observable<Void> updatePassword(String oldPassword, String newPassword) {
        return execute("PUT", "/user/password", true,
                new UserModels.UserPasswordUpdateRequest(oldPassword, newPassword), Void.class);
    }

    // ── 圈子模块 ──

    /** 创建圈子 */
    public Observable<CircleModels.CircleCreateResponse> createCircle(String name, String description) {
        return execute("POST", "/circles", true,
                new CircleModels.CircleCreateRequest(name, description),
                CircleModels.CircleCreateResponse.class);
    }

    /** 获取我加入的圈子列表 */
    public Observable<List<CircleModels.MyCircleItem>> getMyCircles() {
        return execute("GET", "/circles/my", true, null, TypeTokens.listOfMyCircle());
    }

    /** 按圈子码搜索圈子 */
    public Observable<CircleModels.CircleSearchResponse> searchCircle(String code) {
        String encoded = encodeParam(code);
        return execute("GET", "/circles/search?code=" + encoded, true, null,
                CircleModels.CircleSearchResponse.class);
    }

    /** 申请加入圈子 */
    public Observable<CircleModels.CircleApplyResponse> applyCircle(long circleId) {
        return execute("POST", "/circles/" + circleId + "/apply", true, null,
                CircleModels.CircleApplyResponse.class);
    }

    /** 获取待审批申请列表 */
    public Observable<List<CircleModels.CircleAuditItem>> getAuditList(long circleId) {
        return execute("GET", "/circles/" + circleId + "/audit-list", true, null,
                TypeTokens.listOfCircleAudit());
    }

    /** 审批入圈申请(action: approve / reject) */
    public Observable<CircleModels.CircleAuditResponse> auditCircle(long circleId,
                                                                    int applicationId, String action) {
        return execute("POST", "/circles/" + circleId + "/audit", true,
                new CircleModels.CircleAuditRequest(applicationId, action),
                CircleModels.CircleAuditResponse.class);
    }

    /** 获取圈子成员列表 */
    public Observable<CircleModels.CircleMembersResponse> getCircleMembers(long circleId) {
        return execute("GET", "/circles/" + circleId + "/members", true, null,
                CircleModels.CircleMembersResponse.class);
    }

    /** 退出圈子 */
    public Observable<Void> leaveCircle(long circleId) {
        return execute("POST", "/circles/" + circleId + "/leave", true, null, Void.class);
    }

    /** 踢出成员(仅圈主) */
    public Observable<Void> kickMember(long circleId, long targetUserId) {
        return execute("DELETE", "/circles/" + circleId + "/members/" + targetUserId,
                true, null, Void.class);
    }

    /** 解散圈子(仅圈主) */
    public Observable<Void> dissolveCircle(long circleId) {
        return execute("DELETE", "/circles/" + circleId, true, null, Void.class);
    }

    // ── 持仓模块 ──

    /** 获取我的持仓列表 */
    public Observable<List<HoldingModels.MyHoldingItem>> getMyHoldings() {
        return execute("GET", "/holdings/me", true, null, TypeTokens.listOfMyHolding());
    }

    /** 新增或更新单只持仓 */
    public Observable<HoldingModels.MyHoldingItem> saveHolding(String fundCode, String fundName) {
        return execute("POST", "/holdings/me", true,
                new HoldingModels.HoldingItem(fundCode, fundName),
                HoldingModels.MyHoldingItem.class);
    }

    /** 全量覆盖批量同步持仓 */
    public Observable<HoldingModels.HoldingBatchSyncResponse> batchSyncHoldings(
            List<HoldingModels.HoldingItem> holdings) {
        return execute("PUT", "/holdings/me/batch", true,
                new HoldingModels.HoldingBatchSyncRequest(holdings),
                HoldingModels.HoldingBatchSyncResponse.class);
    }

    /** 删除单只持仓 */
    public Observable<Void> deleteHolding(String fundCode) {
        return execute("DELETE", "/holdings/me/" + encodeParam(fundCode), true, null, Void.class);
    }

    /** 圈子内朋友的真实持仓聚合(核心接口) */
    public Observable<HoldingModels.FriendHoldingsResponse> getCircleFriendHoldings(long circleId) {
        return execute("GET", "/circles/" + circleId + "/friend-holdings", true, null,
                HoldingModels.FriendHoldingsResponse.class);
    }

    // ── 版本模块 ──

    /** 检查最新版本与强更状态 */
    public Observable<VersionModels.VersionCheckResponse> checkVersion(int versionCode) {
        return execute("GET", "/app/version/latest?version_code=" + versionCode, false, null,
                VersionModels.VersionCheckResponse.class);
    }

    // ── 通用请求执行 ──

    /**
     * 统一执行请求并解析后端统一响应包装
     *
     * @param method HTTP 方法(GET/POST/PUT/DELETE)
     * @param path   相对路径(不含 /api/v1 前缀),可含 query
     * @param auth   是否需要附加 Authorization: Bearer <access_token>
     * @param body   请求体对象(POST/PUT 时序列化为 JSON),GET/DELETE 传 null
     * @param type   响应 data 的目标类型;Void 类时 data 直接为 null
     */
    private <T> Observable<T> execute(String method, String path, boolean auth,
                                      Object body, Type type) {
        return Observable.create(emitter -> {
            try {
                Request.Builder builder = new Request.Builder()
                        .url(Constants.SERVER_BASE_URL + path);

                if (auth) {
                    String token = authSession.getAccessToken();
                    if (TextUtils.isEmpty(token)) {
                        emitter.onError(new ApiException(40101, "未登录,请先完成邮箱登录"));
                        return;
                    }
                    builder.header("Authorization", "Bearer " + token);
                }

                if (body != null && ("POST".equals(method) || "PUT".equals(method))) {
                    RequestBody requestBody = RequestBody.create(gson.toJson(body), JSON);
                    builder.method(method, requestBody);
                } else {
                    builder.method(method, null);
                }

                Response response = client.newCall(builder.build()).execute();
                String json = response.body() != null ? response.body().string() : "";

                ApiResponse apiResponse = null;
                try {
                    apiResponse = gson.fromJson(json, ApiResponse.class);
                } catch (JsonSyntaxException ignored) {
                    // 非标准 JSON(如网关错误页)时按网络异常处理
                }

                if (apiResponse == null) {
                    emitter.onError(new ApiException(-1,
                            "服务响应异常(HTTP " + response.code() + ")"));
                    return;
                }

                if (apiResponse.getCode() != 200) {
                    emitter.onError(new ApiException(apiResponse.getCode(), apiResponse.getMessage()));
                    return;
                }

                if (type == Void.class || apiResponse.getData() == null) {
                    emitter.onNext(null);
                } else {
                    emitter.onNext(gson.<T>fromJson(apiResponse.getData(), type));
                }
                emitter.onComplete();
            } catch (Exception e) {
                if (e instanceof ApiException) {
                    emitter.onError(e);
                } else {
                    emitter.onError(new ApiException(-1, "网络请求失败: " + e.getMessage()));
                }
            }
        });
    }

    /** URL 编码查询/路径参数 */
    private String encodeParam(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }
}