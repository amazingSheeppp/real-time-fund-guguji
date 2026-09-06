package com.fund.guguji.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.fund.guguji.data.db.entity.FundEntity;
import com.fund.guguji.data.db.entity.GroupEntity;
import com.fund.guguji.data.db.entity.GroupFundCrossRef;

import java.util.List;

@Dao
public interface GroupDao {

    // ── 分组操作 ──

    @Query("SELECT * FROM fund_groups ORDER BY name ASC")
    LiveData<List<GroupEntity>> getAllGroups();

    @Query("SELECT * FROM fund_groups ORDER BY name ASC")
    List<GroupEntity> getAllGroupsSync();

    @Query("SELECT * FROM fund_groups WHERE id = :groupId LIMIT 1")
    GroupEntity getGroupById(String groupId);

    @Query("SELECT COUNT(*) FROM fund_groups WHERE name = :name")
    int countGroupByName(String name);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertGroup(GroupEntity group);

    @Query("UPDATE fund_groups SET name = :name WHERE id = :groupId")
    void updateGroupName(String groupId, String name);

    @Delete
    void deleteGroup(GroupEntity group);

    @Query("DELETE FROM fund_groups WHERE id = :groupId")
    void deleteGroupById(String groupId);

    @Query("DELETE FROM fund_groups")
    void deleteAllGroups();

    @Query("DELETE FROM group_fund_cross_ref")
    void deleteAllCrossRefs();

    // ── 分组-基金关联操作 ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addFundToGroup(GroupFundCrossRef ref);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addFundsToGroup(List<GroupFundCrossRef> refs);

    @Query("DELETE FROM group_fund_cross_ref WHERE fundCode = :fundCode AND groupId = :groupId")
    void removeFundFromGroup(String fundCode, String groupId);

    @Query("DELETE FROM group_fund_cross_ref WHERE groupId = :groupId")
    void clearGroup(String groupId);

    @Query("DELETE FROM group_fund_cross_ref WHERE fundCode = :fundCode")
    void removeFundFromAllGroups(String fundCode);

    @Query("SELECT fundCode FROM group_fund_cross_ref WHERE groupId = :groupId")
    List<String> getFundCodesInGroup(String groupId);

    @Query("SELECT groupId FROM group_fund_cross_ref WHERE fundCode = :fundCode")
    List<String> getGroupIdsByFundSync(String fundCode);

    @Query("SELECT f.* FROM funds f INNER JOIN group_fund_cross_ref r ON f.code = r.fundCode WHERE r.groupId = :groupId ORDER BY f.orderIndex ASC")
    LiveData<List<FundEntity>> getFundsByGroup(String groupId);
}
