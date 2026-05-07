package com.fund.guguji.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.fund.guguji.data.db.entity.ValuationPointEntity;

import java.util.List;

@Dao
public interface ValuationSeriesDao {

    @Query("SELECT * FROM valuation_timeseries WHERE fundCode = :fundCode ORDER BY date ASC, time ASC")
    List<ValuationPointEntity> getSeries(String fundCode);

    @Insert
    void insertPoint(ValuationPointEntity point);

    @Query("DELETE FROM valuation_timeseries WHERE fundCode = :fundCode")
    void deleteSeries(String fundCode);

    @Query("DELETE FROM valuation_timeseries WHERE fundCode = :fundCode AND date < :date")
    void deleteOldData(String fundCode, String date);
}
