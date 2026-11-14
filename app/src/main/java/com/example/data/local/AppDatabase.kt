package com.example.data.local

import android.content.Context
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        User::class,
        Member::class,
        SavingsTransaction::class,
        Loan::class,
        InstallmentPayment::class
    ],
    version = 3,
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

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ekta_samiti_app.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate only default admin and collector accounts. NO auto members or collections.
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getInstance(context)
                                prepopulateDatabase(database)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun prepopulateDatabase(db: AppDatabase) {
            // Default Admin - Nitai Chandra Roy (Director)
            db.userDao().insertUser(
                User(
                    email = "admin@ektasamiti.com",
                    passwordHash = "admin123",
                    fullName = "নিতাই চন্দ্র রায় (পরিচালক)",
                    mobile = "01711002233",
                    role = UserRole.ADMIN
                )
            )

            // Default Collector
            db.userDao().insertUser(
                User(
                    email = "collector@ektasamiti.com",
                    passwordHash = "123456",
                    fullName = "মো: রহিম মিয়া (মাঠকর্মী)",
                    mobile = "01822334455",
                    role = UserRole.COLLECTOR
                )
            )
        }
    }
}
