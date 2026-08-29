package com.fund.guguji.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.fund.guguji.data.db.dao.FundDao;
import com.fund.guguji.data.db.dao.GroupDao;
import com.fund.guguji.data.db.dao.ValuationSeriesDao;
import com.fund.guguji.data.db.entity.FundEntity;
import com.fund.guguji.data.db.entity.GroupEntity;
import com.fund.guguji.data.db.entity.GroupFundCrossRef;
import com.fund.guguji.data.db.entity.ValuationPointEntity;

@Database(
    entities = {
        FundEntity.class,
        GroupEntity.class,
        GroupFundCrossRef.class,
        ValuationPointEntity.class
    },
    version = 1,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract FundDao fundDao();
    public abstract GroupDao groupDao();
    public abstract ValuationSeriesDao seriesDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "guguji_db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
