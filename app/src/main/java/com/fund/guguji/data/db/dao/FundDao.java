package com.fund.guguji.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.fund.guguji.data.db.entity.FundEntity;

import java.util.List;

@Dao
public interface FundDao {

    @Query("SELECT * FROM funds ORDER BY orderIndex ASC")
    LiveData<List<FundEntity>> getAllFunds();

    @Query("SELECT * FROM funds WHERE code = :code")
    LiveData<FundEntity> getFundByCodeLive(String code);

    @Query("SELECT * FROM funds ORDER BY orderIndex ASC")
    List<FundEntity> getAllFundsSync();

    @Query("SELECT * FROM funds WHERE code = :code")
    FundEntity getFundByCode(String code);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFund(FundEntity fund);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFunds(List<FundEntity> funds);

    @Update
    void updateFund(FundEntity fund);

    @Delete
    void deleteFund(FundEntity fund);

    @Query("DELETE FROM funds WHERE code = :code")
    void deleteFundByCode(String code);

    @Query("DELETE FROM funds")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM funds")
    LiveData<Integer> getFundCount();

    @Query("SELECT MAX(orderIndex) FROM funds")
    Integer getMaxOrderIndex();
}
