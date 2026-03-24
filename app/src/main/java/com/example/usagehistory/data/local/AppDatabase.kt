package com.example.usagehistory.data.local

import android.content.Context
import androidx.room.migration.Migration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [UsageSessionEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usageSessionDao(): UsageSessionDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "usage-history.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
        }

        private val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        ALTER TABLE usage_sessions
                        ADD COLUMN sessionSource TEXT NOT NULL DEFAULT '${UsageSessionEntity.SESSION_SOURCE_USAGE_STATS}'
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        ALTER TABLE usage_sessions
                        ADD COLUMN contentType TEXT NOT NULL DEFAULT '${UsageSessionEntity.CONTENT_TYPE_APP}'
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        ALTER TABLE usage_sessions
                        ADD COLUMN contentTitle TEXT
                        """.trimIndent(),
                    )
                }
            }

        private val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        ALTER TABLE usage_sessions
                        ADD COLUMN contentKey TEXT
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        ALTER TABLE usage_sessions
                        ADD COLUMN notificationPostedAtEpochMillis INTEGER
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        ALTER TABLE usage_sessions
                        ADD COLUMN readInferredAtEpochMillis INTEGER
                        """.trimIndent(),
                    )
                }
            }
    }
}
