package com.fund.guguji.data.repository;

import androidx.lifecycle.LiveData;

import com.fund.guguji.data.db.dao.FundDao;
import com.fund.guguji.data.db.dao.GroupDao;
import com.fund.guguji.data.db.entity.FundEntity;
import com.fund.guguji.data.db.entity.GroupEntity;
import com.fund.guguji.data.db.entity.GroupFundCrossRef;

import java.util.List;

/**
 * 本地数据库操作仓库
 * 处理增删改查及分组管理等纯本地操作
 */
public class LocalFundRepository {

    private final FundDao fundDao;
    private final GroupDao groupDao;

    public LocalFundRepository(FundDao fundDao, GroupDao groupDao) {
        this.fundDao = fundDao;
        this.groupDao = groupDao;
    }

    // ── 基金 ──

    public LiveData<List<FundEntity>> getAllFunds() {
        return fundDao.getAllFunds();
    }

    public List<FundEntity> getAllFundsSync() {
        return fundDao.getAllFundsSync();
    }

    public LiveData<FundEntity> getFundByCodeLive(String code) {
        return fundDao.getFundByCodeLive(code);
    }

    public FundEntity getFundByCode(String code) {
        return fundDao.getFundByCode(code);
    }

    public void insertFund(FundEntity fund) {
        // 按添加时间排序需要递增的 orderIndex,取当前最大值 +1,删除基金后仍保持相对顺序
        Integer maxOrder = fundDao.getMaxOrderIndex();
        fund.setOrderIndex(maxOrder == null ? 1 : maxOrder + 1);
        fundDao.insertFund(fund);
    }

    public void updateFund(FundEntity fund) {
        fundDao.updateFund(fund);
    }

    public void deleteFundByCode(String code) {
        fundDao.deleteFundByCode(code);
    }

    public LiveData<Integer> getFundCount() {
        return fundDao.getFundCount();
    }

    // ── 分组 ──

    public LiveData<List<GroupEntity>> getAllGroups() {
        return groupDao.getAllGroups();
    }

    public List<GroupEntity> getAllGroupsSync() {
        return groupDao.getAllGroupsSync();
    }

    public LiveData<List<FundEntity>> getFundsByGroup(String groupId) {
        return groupDao.getFundsByGroup(groupId);
    }

    public void insertGroup(GroupEntity group) {
        groupDao.insertGroup(group);
    }

    public void updateGroupName(String groupId, String name) {
        groupDao.updateGroupName(groupId, name);
    }

    public void deleteGroupById(String groupId) {
        groupDao.deleteGroupById(groupId);
        groupDao.clearGroup(groupId);
    }

    public boolean isGroupNameExists(String name) {
        return groupDao.countGroupByName(name) > 0;
    }

    public void addFundToGroup(String fundCode, String groupId) {
        groupDao.addFundToGroup(new GroupFundCrossRef(groupId, fundCode));
    }

    public void addFundsToGroup(List<String> fundCodes, String groupId) {
        if (fundCodes == null || fundCodes.isEmpty()) return;
        List<GroupFundCrossRef> refs = new java.util.ArrayList<>();
        for (String code : fundCodes) {
            refs.add(new GroupFundCrossRef(groupId, code));
        }
        groupDao.addFundsToGroup(refs);
    }

    public void removeFundFromGroup(String fundCode, String groupId) {
        groupDao.removeFundFromGroup(fundCode, groupId);
    }

    public List<String> getFundCodesInGroup(String groupId) {
        return groupDao.getFundCodesInGroup(groupId);
    }

    public List<String> getGroupIdsByFundSync(String fundCode) {
        return groupDao.getGroupIdsByFundSync(fundCode);
    }

    public void setFundGroups(String fundCode, List<String> groupIds) {
        groupDao.removeFundFromAllGroups(fundCode);
        if (groupIds != null && !groupIds.isEmpty()) {
            List<GroupFundCrossRef> refs = new java.util.ArrayList<>();
            for (String gId : groupIds) {
                refs.add(new GroupFundCrossRef(gId, fundCode));
            }
            groupDao.addFundsToGroup(refs);
        }
    }

    public void clearGroup(String groupId) {
        groupDao.clearGroup(groupId);
    }

    public void removeFundFromAllGroups(String fundCode) {
        groupDao.removeFundFromAllGroups(fundCode);
    }
}
