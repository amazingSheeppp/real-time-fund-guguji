package com.fund.guguji.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.fund.guguji.data.db.entity.GroupEntity;
import com.fund.guguji.data.db.entity.GroupFundCrossRef;

import java.util.List;

@Dao
public interface GroupDao {

    // ── 分组操作 ──

    @Query("SELECT * FROM fund_groups ORDER BY name ASC")
    LiveData<List<GroupEntity>> getAllGroups();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertGroup(GroupEntity group);

    @Delete
    void deleteGroup(GroupEntity group);

    @Query("DELETE FROM fund_groups WHERE id = :groupId")
    void deleteGroupById(String groupId);

    // ── 分组-基金关联操作 ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addFundToGroup(GroupFundCrossRef ref);

    @Query("DELETE FROM group_fund_cross_ref WHERE fundCode = :fundCode AND groupId = :groupId")
    void removeFundFromGroup(String fundCode, String groupId);

    @Query("DELETE FROM group_fund_cross_ref WHERE groupId = :groupId")
    void clearGroup(String groupId);

    @Query("DELETE FROM group_fund_cross_ref WHERE fundCode = :fundCode")
    void removeFundFromAllGroups(String fundCode);

    @Query("SELECT fundCode FROM group_fund_cross_ref WHERE groupId = :groupId")
    List<String> getFundCodesInGroup(String groupId);
}
