package com.example.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.InstallmentPayment
import com.example.data.model.Loan
import com.example.data.model.Member
import com.example.data.model.SavingsTransaction
import com.example.data.model.User
import com.example.data.model.UserRole

@Database(
    entities = [
        User::class,
        Member::class,
        SavingsTransaction::class,
        Loan::class,
        InstallmentPayment::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun memberDao(): MemberDao
    abstract fun savingsDao(): SavingsDao
    abstract fun loanDao(): LoanDao
    abstract fun installmentDao(): InstallmentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE loans ADD COLUMN interestRate REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "UPDATE users SET email = 'Nitai', passwordHash = 'Nemai' WHERE role = 'ADMIN' AND (email = 'admin@ektasamiti.com' OR email = 'Nitai')"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ekta_samiti_app.db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            try {
                                prepopulateDatabaseSync(db)
                            } catch (_: Exception) {
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private fun prepopulateDatabaseSync(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()
            // Default Admin - Nitai Chandra Roy (Director)
            db.insert(
                "users",
                SQLiteDatabase.CONFLICT_REPLACE,
                ContentValues().apply {
                    put("email", "Nitai")
                    put("passwordHash", "Nemai")
                    put("fullName", "নিতাই চন্দ্র রায় (পরিচালক)")
                    put("mobile", "01711002233")
                    put("role", UserRole.ADMIN.name)
                    put("isActive", 1)
                    put("createdAt", now)
                }
            )

            // Default Collector
            db.insert(
                "users",
                SQLiteDatabase.CONFLICT_REPLACE,
                ContentValues().apply {
                    put("email", "collector@ektasamiti.com")
                    put("passwordHash", "123456")
                    put("fullName", "মো: রহিম মিয়া (মাঠকর্মী)")
                    put("mobile", "01822334455")
                    put("role", UserRole.COLLECTOR.name)
                    put("isActive", 1)
                    put("createdAt", now)
                }
            )
        }
    }
}
