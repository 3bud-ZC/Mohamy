package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Client::class,
        LegalCase::class,
        CaseSession::class,
        LegalTask::class,
        CaseFile::class,
        LegalTemplate::class,
        GeneratedDocument::class,
        LicenseCache::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun caseDao(): CaseDao
    abstract fun sessionDao(): SessionDao
    abstract fun taskDao(): TaskDao
    abstract fun fileDao(): FileDao
    abstract fun templateDao(): TemplateDao
    abstract fun generatedDocDao(): GeneratedDocDao
    abstract fun licenseDao(): LicenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mohamy_phone_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
