package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.MockTestDao
import com.example.data.local.dao.QuestionDao
import com.example.data.local.dao.TestResultDao
import com.example.data.local.entity.MockTestEntity
import com.example.data.local.entity.QuestionEntity
import com.example.data.local.entity.TestResultEntity

@Database(
    entities = [
        MockTestEntity::class,
        QuestionEntity::class,
        TestResultEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class ExamSathiDatabase : RoomDatabase() {
    abstract fun mockTestDao(): MockTestDao
    abstract fun questionDao(): QuestionDao
    abstract fun testResultDao(): TestResultDao

    companion object {
        @Volatile
        private var INSTANCE: ExamSathiDatabase? = null

        fun getDatabase(context: Context): ExamSathiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExamSathiDatabase::class.java,
                    "examsathi_database.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
