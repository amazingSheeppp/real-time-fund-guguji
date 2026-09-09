package com.fund.guguji.data.api.model;

/**
 * App 版本检测与强制更新模型
 * 对应后端 schemas/app_version.py。
 */
public final class VersionModels {

    private VersionModels() {}

    /** 版本检测响应 */
    public static class VersionCheckResponse {
        private boolean hasUpdate;
        private boolean isForceUpdate;
        private int versionCode;
        private String versionName;
        private String title;
        private String updateLog;
        private String apkUrl;
        private long apkSize;
        private String apkSha256;
        private String releaseTime;

        public boolean isHasUpdate() { return hasUpdate; }
        public boolean isForceUpdate() { return isForceUpdate; }
        public int getVersionCode() { return versionCode; }
        public String getVersionName() { return versionName; }
        public String getTitle() { return title; }
        public String getUpdateLog() { return updateLog; }
        public String getApkUrl() { return apkUrl; }
        public long getApkSize() { return apkSize; }
        public String getApkSha256() { return apkSha256; }
        public String getReleaseTime() { return releaseTime; }
    }
}