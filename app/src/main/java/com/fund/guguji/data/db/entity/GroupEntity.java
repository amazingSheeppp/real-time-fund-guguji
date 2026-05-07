package com.fund.guguji.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 基金分组实体
 */
@Entity(tableName = "fund_groups")
public class GroupEntity {
    @PrimaryKey
    @NonNull
    private String id;      // 分组唯一 ID (UUID)
    private String name;    // 分组名称

    public GroupEntity(@NonNull String id, String name) {
        this.id = id;
        this.name = name;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
