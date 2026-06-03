package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Client::class,
        LegalCase::class,
        CaseSession::class,
        LegalTask::class,
        CaseFile::class,
        ClientInteraction::class,
        LegalTemplate::class,
        GeneratedDocument::class,
        FeeRecord::class,
        CustomCaseCategory::class,
        LicenseCache::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun caseDao(): CaseDao
    abstract fun sessionDao(): SessionDao
    abstract fun taskDao(): TaskDao
    abstract fun fileDao(): FileDao
    abstract fun clientInteractionDao(): ClientInteractionDao
    abstract fun templateDao(): TemplateDao
    abstract fun generatedDocDao(): GeneratedDocDao
    abstract fun feeDao(): FeeDao
    abstract fun customCaseCategoryDao(): CustomCaseCategoryDao
    abstract fun licenseDao(): LicenseDao

    companion object {
        const val DATABASE_NAME = "mohamy_phone_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS custom_case_categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        normalizedName TEXT NOT NULL,
                        createdDate INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_custom_case_categories_normalizedName ON custom_case_categories(normalizedName)"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE files ADD COLUMN linkedSessionId INTEGER")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS client_interactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        clientId INTEGER NOT NULL,
                        clientName TEXT NOT NULL,
                        interactionType TEXT NOT NULL,
                        title TEXT NOT NULL,
                        details TEXT NOT NULL,
                        relatedCaseId INTEGER,
                        relatedCaseTitle TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS fee_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        clientId INTEGER NOT NULL,
                        clientName TEXT NOT NULL,
                        caseId INTEGER,
                        caseTitle TEXT,
                        title TEXT NOT NULL,
                        totalAmount REAL NOT NULL,
                        paidAmount REAL NOT NULL,
                        currency TEXT NOT NULL,
                        dueDate TEXT NOT NULL,
                        status TEXT NOT NULL,
                        paymentMethod TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE files ADD COLUMN accentColorHex TEXT NOT NULL DEFAULT '#E8EEF8'")
                db.execSQL("ALTER TABLE files ADD COLUMN cardStyle TEXT NOT NULL DEFAULT 'rounded'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun closeInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
