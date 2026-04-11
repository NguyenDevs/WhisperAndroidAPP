package com.nguyendevs.stkh.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room Database cho ứng dụng NguyenDevs STT.
 * Singleton pattern đảm bảo chỉ có một instance trong toàn app.
 *
 * Migration từ version 1 (SQLite thủ công) sang Room:
 * Vì schema (content TEXT, date TEXT, lang TEXT) không đổi,
 * chỉ cần thêm fallbackToDestructiveMigration() hoặc migration rõ ràng.
 * Tuy nhiên date column đổi kiểu TEXT → INTEGER (epoch ms).
 */
@Database(
    entities = [HistoryEntity::class],
    version = 2,          // Tăng version vì date column đổi từ TEXT → INTEGER
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun historyDao(): HistoryDao

    companion object {
        private const val DATABASE_NAME = "SpeechHistory.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration từ v1 (SQLite thủ công, date TEXT) → v2 (Room, date INTEGER)
         * Chuyển đổi date column để tránh mất data cũ.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Tạo bảng mới với date INTEGER
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS history_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        content TEXT NOT NULL,
                        date INTEGER NOT NULL,
                        lang TEXT NOT NULL
                    )
                """.trimIndent())
                // Copy data, convert date TEXT → INTEGER (cast hoặc dùng giá trị mặc định)
                database.execSQL("""
                    INSERT INTO history_new (id, content, date, lang)
                    SELECT id, content,
                        CASE WHEN date GLOB '[0-9]*' THEN CAST(date AS INTEGER)
                             ELSE strftime('%s', 'now') * 1000
                        END,
                        COALESCE(lang, 'unknown')
                    FROM history
                """.trimIndent())
                // Xóa bảng cũ và đổi tên bảng mới
                database.execSQL("DROP TABLE IF EXISTS history")
                database.execSQL("ALTER TABLE history_new RENAME TO history")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
