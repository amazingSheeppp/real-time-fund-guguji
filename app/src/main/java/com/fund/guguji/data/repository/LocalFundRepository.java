package com.fund.guguji.data.repository;

import androidx.lifecycle.LiveData;

import com.fund.guguji.data.db.dao.FundDao;
import com.fund.guguji.data.db.dao.GroupDao;
import com.fund.guguji.data.db.dao.HoldingDao;
import com.fund.guguji.data.db.entity.FundEntity;
import com.fund.guguji.data.db.entity.GroupEntity;
import com.fund.guguji.data.db.entity.GroupFundCrossRef;
import com.fund.guguji.data.db.entity.HoldingEntity;

import java.util.List;

/**
 * 本地数据库操作仓库
 * 处理增删改查及分组管理等纯本地操作
 */
public class LocalFundRepository {

    private final FundDao fundDao;
    private final HoldingDao holdingDao;
    private final GroupDao groupDao;

    public LocalFundRepository(FundDao fundDao, HoldingDao holdingDao, GroupDao groupDao) {
        this.fundDao = fundDao;
        this.holdingDao = holdingDao;
        this.groupDao = groupDao;
    }

    // ── 基金 ──

    public LiveData<List<FundEntity>> getAllFunds() {
        return fundDao.getAllFunds();
    }

    public LiveData<FundEntity> getFundByCodeLive(String code) {
        return fundDao.getFundByCodeLive(code);
    }

    public FundEntity getFundByCode(String code) {
        return fundDao.getFundByCode(code);
    }

    public void insertFund(FundEntity fund) {
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

    // ── 持仓 ──

    public LiveData<List<HoldingEntity>> getAllHoldings() {
        return holdingDao.getAllHoldings();
    }

    public LiveData<HoldingEntity> getHoldingByCodeLive(String fundCode) {
        return holdingDao.getHoldingByCodeLive(fundCode);
    }

    public HoldingEntity getHoldingByCode(String fundCode) {
        return holdingDao.getHoldingByCode(fundCode);
    }

    public void saveHolding(HoldingEntity holding) {
        holdingDao.insertOrUpdate(holding);
    }

    public void deleteHolding(String fundCode) {
        holdingDao.deleteByCode(fundCode);
    }

    // ── 分组 ──

    public LiveData<List<GroupEntity>> getAllGroups() {
        return groupDao.getAllGroups();
    }

    public void insertGroup(GroupEntity group) {
        groupDao.insertGroup(group);
    }

    public void deleteGroupById(String groupId) {
        groupDao.deleteGroupById(groupId);
    }

    public void addFundToGroup(String fundCode, String groupId) {
        groupDao.addFundToGroup(new GroupFundCrossRef(groupId, fundCode));
    }

    public void removeFundFromGroup(String fundCode, String groupId) {
        groupDao.removeFundFromGroup(fundCode, groupId);
    }

    public List<String> getFundCodesInGroup(String groupId) {
        return groupDao.getFundCodesInGroup(groupId);
    }

    public void clearGroup(String groupId) {
        groupDao.clearGroup(groupId);
    }

    public void removeFundFromAllGroups(String fundCode) {
        groupDao.removeFundFromAllGroups(fundCode);
    }
}
