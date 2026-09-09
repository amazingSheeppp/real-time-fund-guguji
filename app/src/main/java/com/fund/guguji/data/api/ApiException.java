package com.fund.guguji.data.api;

/**
 * 自建后端接口业务异常
 * 服务端统一返回 HTTP 200,业务错误码存放在响应体 code 字段内,
 * 此处将其包装为可被 RxJava onError 捕获的异常。
 */
public class ApiException extends RuntimeException {

    /** 服务端业务错误码(见 ErrorCode),本地包装的通用错误统一使用 -1 */
    private final int code;

    public ApiException(int code, String message) {
        super(message == null || message.isEmpty() ? "请求失败" : message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}