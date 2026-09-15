package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.MockTestEntity
import com.example.data.local.entity.QuestionEntity
import com.example.data.local.entity.TestResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MockTestDao {
    @Query("SELECT * FROM mock_tests")
    fun getAllMockTests(): Flow<List<MockTestEntity>>

    @Query("SELECT * FROM mock_tests WHERE id = :testId LIMIT 1")
    suspend fun getMockTestById(testId: String): MockTestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMockTests(tests: List<MockTestEntity>)

    @Query("SELECT COUNT(*) FROM mock_tests")
    suspend fun getMockTestCount(): Int
}

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions WHERE testId = :testId ORDER BY questionNumber ASC")
    suspend fun getQuestionsForTest(testId: String): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE subject = :subject AND isActive = 1")
    suspend fun getQuestionsBySubject(subject: String): List<QuestionEntity>

    @Query("SELECT * FROM questions ORDER BY questionNumber ASC")
    suspend fun getAllQuestions(): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE isActive = 1 ORDER BY questionNumber ASC")
    suspend fun getActiveQuestions(): List<QuestionEntity>

    @Query("SELECT * FROM questions ORDER BY id DESC")
    fun getAllQuestionsFlow(): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE id = :id LIMIT 1")
    suspend fun getQuestionById(id: String): QuestionEntity?

    @Query("SELECT DISTINCT subject FROM questions")
    suspend fun getDistinctSubjects(): List<String>

    @Query("SELECT DISTINCT topic FROM questions WHERE subject = :subject")
    suspend fun getDistinctTopicsForSubject(subject: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Update
    suspend fun updateQuestion(question: QuestionEntity)

    @Query("UPDATE questions SET isActive = :isActive WHERE id = :id")
    suspend fun updateQuestionStatus(id: String, isActive: Boolean)

    @Query("DELETE FROM questions WHERE id = :id")
    suspend fun deleteQuestionById(id: String)

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun getQuestionCount(): Int

    @Query("SELECT COUNT(*) FROM questions WHERE isActive = 1")
    suspend fun getActiveQuestionCount(): Int
}

@Dao
interface TestResultDao {
    @Query("SELECT * FROM test_results ORDER BY timestamp DESC")
    fun getAllResults(): Flow<List<TestResultEntity>>

    @Query("SELECT * FROM test_results WHERE id = :id LIMIT 1")
    suspend fun getResultById(id: String): TestResultEntity?

    @Query("SELECT * FROM test_results WHERE testId = :testId ORDER BY timestamp DESC")
    fun getResultsForTest(testId: String): Flow<List<TestResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: TestResultEntity)

    @Query("DELETE FROM test_results WHERE id = :id")
    suspend fun deleteResult(id: String)

    @Query("DELETE FROM test_results")
    suspend fun clearAllResults()

    @Query("SELECT COUNT(*) FROM test_results")
    suspend fun getResultCount(): Int
}
