package com.fund.guguji.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 基金搜索接口响应包装
 * 对应 fundsuggest.eastmoney.com 搜索接口
 */
public class FundSearchResult {

    @SerializedName("Datas")
    private List<FundSearchItem> datas;

    @SerializedName("ErrCode")
    private int errCode;

    public List<FundSearchItem> getDatas() { return datas; }
    public int getErrCode() { return errCode; }

    public static class FundSearchItem {
        @SerializedName("CODE")
        private String code;

        @SerializedName("NAME")
        private String name;  // 可能含 HTML 高亮标签

        @SerializedName("CATEGORYDESC")
        private String categoryDesc;

        @SerializedName("FundBaseInfo")
        private FundBaseInfo fundBaseInfo;

        public String getCode() { return code; }
        public String getName() {
            // 优先用 FundBaseInfo 中的短名称，不含 HTML 标签
            if (fundBaseInfo != null && fundBaseInfo.shortName != null) {
                return fundBaseInfo.shortName;
            }
            return name != null ? name.replaceAll("<[^>]+>", "") : "";
        }
        public String getFoundType() {
            if (fundBaseInfo != null && fundBaseInfo.ftype != null) {
                return fundBaseInfo.ftype;
            }
            return categoryDesc != null ? categoryDesc : "";
        }
    }

    private static class FundBaseInfo {
        @SerializedName("SHORTNAME")
        private String shortName;

        @SerializedName("FTYPE")
        private String ftype;
    }
}
