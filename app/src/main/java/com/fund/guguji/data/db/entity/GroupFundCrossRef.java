package com.fund.guguji.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;

/**
 * 分组-基金多对多关联实体
 */
@Entity(
    tableName = "group_fund_cross_ref",
    primaryKeys = {"groupId", "fundCode"}
)
public class GroupFundCrossRef {
    @NonNull
    private String groupId;
    @NonNull
    private String fundCode;

    public GroupFundCrossRef(@NonNull String groupId, @NonNull String fundCode) {
        this.groupId = groupId;
        this.fundCode = fundCode;
    }

    @NonNull
    public String getGroupId() { return groupId; }
    public void setGroupId(@NonNull String groupId) { this.groupId = groupId; }

    @NonNull
    public String getFundCode() { return fundCode; }
    public void setFundCode(@NonNull String fundCode) { this.fundCode = fundCode; }
}
