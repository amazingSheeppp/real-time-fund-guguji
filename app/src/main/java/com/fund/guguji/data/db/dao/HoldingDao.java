package com.fund.guguji.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.fund.guguji.data.db.entity.HoldingEntity;

import java.util.List;

@Dao
public interface HoldingDao {

    @Query("SELECT * FROM holdings")
    LiveData<List<HoldingEntity>> getAllHoldings();

    @Query("SELECT * FROM holdings WHERE fundCode = :fundCode")
    LiveData<HoldingEntity> getHoldingByCodeLive(String fundCode);

    @Query("SELECT * FROM holdings WHERE fundCode = :fundCode")
    HoldingEntity getHoldingByCode(String fundCode);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(HoldingEntity holding);

    @Query("DELETE FROM holdings WHERE fundCode = :fundCode")
    void deleteByCode(String fundCode);
}
