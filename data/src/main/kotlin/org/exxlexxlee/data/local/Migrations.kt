package org.exxlexxlee.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS places (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, position INTEGER NOT NULL, json TEXT NOT NULL)")
        db.execSQL("INSERT INTO places (position, json) SELECT 0, json FROM selection ORDER BY id")
        db.execSQL("DROP TABLE selection")
        db.execSQL("ALTER TABLE schedule_cache ADD COLUMN todayPublished INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE schedule_cache ADD COLUMN tomorrowPublished INTEGER")
        db.execSQL("ALTER TABLE schedule_cache ADD COLUMN schedulesSuspended INTEGER NOT NULL DEFAULT 0")
    }
}
