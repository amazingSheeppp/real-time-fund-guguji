package com.fund.guguji.data.api.model;

import com.google.gson.JsonElement;

/**
 * 服务端统一响应包装
 * 对应后端 ResponseModel:{ code, message, data, timestamp }
 * data 保留为 JsonElement,由调用方按目标类型二次反序列化。
 */
public class ApiResponse {

    private int code;
    private String message;
    private JsonElement data;
    private long timestamp;

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public JsonElement getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }
}