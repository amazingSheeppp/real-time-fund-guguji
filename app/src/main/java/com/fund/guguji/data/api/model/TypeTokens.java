package com.fund.guguji.data.api.model;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

/**
 * 类型字面量工具
 * 为 Gson 反序列化后端返回的 List&lt;T&gt; 提供具体元素类型。
 */
public final class TypeTokens {

    private TypeTokens() {}

    public static Type listOfMyCircle() {
        return new TypeToken<List<CircleModels.MyCircleItem>>() {}.getType();
    }

    public static Type listOfCircleAudit() {
        return new TypeToken<List<CircleModels.CircleAuditItem>>() {}.getType();
    }

    public static Type listOfMyHolding() {
        return new TypeToken<List<HoldingModels.MyHoldingItem>>() {}.getType();
    }
}