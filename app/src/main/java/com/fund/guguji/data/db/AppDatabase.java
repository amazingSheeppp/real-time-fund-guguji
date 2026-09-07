package com.fund.guguji.data.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

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
    version = 2,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract FundDao fundDao();
    public abstract GroupDao groupDao();
    public abstract ValuationSeriesDao seriesDao();

    private static volatile AppDatabase INSTANCE;

    /** v1 → v2: funds 表新增官方净值三列(收盘后覆盖估值展示) */
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE funds ADD COLUMN officialNav TEXT");
            database.execSQL("ALTER TABLE funds ADD COLUMN officialNavDate TEXT");
            database.execSQL("ALTER TABLE funds ADD COLUMN officialNavChange REAL");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "guguji_db"
                    )
                    .addMigrations(MIGRATION_1_2)
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
