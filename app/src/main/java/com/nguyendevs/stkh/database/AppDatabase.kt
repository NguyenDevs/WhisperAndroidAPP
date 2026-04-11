package com.nguyendevs.stkh.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [HistoryEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun historyDao(): HistoryDao

    companion object {
        private const val DATABASE_NAME = "SpeechHistory.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS history_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        content TEXT NOT NULL,
                        date INTEGER NOT NULL,
                        lang TEXT NOT NULL
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT INTO history_new (id, content, date, lang)
                    SELECT id, content,
                        CASE WHEN date GLOB '[0-9]*' THEN CAST(date AS INTEGER)
                             ELSE strftime('%s', 'now') * 1000
                        END,
                        COALESCE(lang, 'unknown')
                    FROM history
                """.trimIndent())
                database.execSQL("DROP TABLE IF EXISTS history")
                database.execSQL("ALTER TABLE history_new RENAME TO history")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, DATABASE_NAME
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
    }
}
