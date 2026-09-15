package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ExamSathiDatabase
import com.example.data.repository.QuestionRepository
import com.example.domain.model.Question
import com.example.domain.model.QuestionValidationError
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuestionBankAdminTest {

    private lateinit var database: ExamSathiDatabase
    private lateinit var repository: QuestionRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ExamSathiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = QuestionRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testValidationCatchesEmptyFields() = runBlocking {
        val malformedQuestion = Question(
            id = "",
            testId = "test_1",
            questionNumber = 1,
            subject = "",
            topic = "",
            questionText = "",
            options = listOf("", "", "", ""),
            correctOptionIndex = 5,
            explanation = ""
        )

        val validation = repository.validateQuestion(malformedQuestion)
        assertFalse(validation.isValid)
        assertTrue(validation.errors.contains(QuestionValidationError.BlankQuestionText))
        assertTrue(validation.errors.contains(QuestionValidationError.BlankSubject))
        assertTrue(validation.errors.contains(QuestionValidationError.BlankTopic))
        assertTrue(validation.errors.contains(QuestionValidationError.InvalidCorrectOption))
        assertTrue(validation.errors.contains(QuestionValidationError.BlankExplanation))
    }

    @Test
    fun testValidQuestionPassesAndSaves() = runBlocking {
        val validQuestion = Question(
            id = "q_test_101",
            testId = "bank",
            questionNumber = 1,
            subject = "Indian Polity",
            topic = "Fundamental Rights",
            questionText = "Which Article of the Constitution guarantees the Right to Equality?",
            options = listOf("Article 14", "Article 19", "Article 21", "Article 32"),
            correctOptionIndex = 0,
            explanation = "Article 14 guarantees equality before the law and equal protection of laws.",
            difficulty = "Easy",
            isActive = true
        )

        val validation = repository.validateQuestion(validQuestion)
        assertTrue(validation.isValid)

        val saveResult = repository.saveQuestion(validQuestion)
        assertTrue(saveResult.isSuccess)

        val saved = repository.getQuestionById("q_test_101")
        assertNotNull(saved)
        assertEquals("Indian Polity", saved?.subject)
        assertEquals("Article 14", saved?.options?.get(0))
        assertTrue(saved?.isActive == true)
    }

    @Test
    fun testDuplicateQuestionPrevention() = runBlocking {
        val q1 = Question(
            id = "q_unique_1",
            testId = "bank",
            questionNumber = 1,
            subject = "General Studies",
            topic = "Geography",
            questionText = "What is the capital of India?",
            options = listOf("Mumbai", "New Delhi", "Kolkata", "Chennai"),
            correctOptionIndex = 1,
            explanation = "New Delhi is the national capital.",
            difficulty = "Easy"
        )
        repository.saveQuestion(q1)

        val duplicate = Question(
            id = "q_unique_2",
            testId = "bank",
            questionNumber = 2,
            subject = "General Studies",
            topic = "Geography",
            questionText = "What is the capital of India?",
            options = listOf("A", "B", "C", "D"),
            correctOptionIndex = 0,
            explanation = "Duplicate test",
            difficulty = "Easy"
        )

        val validation = repository.validateQuestion(duplicate)
        assertFalse(validation.isValid)
        assertTrue(validation.errors.contains(QuestionValidationError.DuplicateQuestion))
    }

    @Test
    fun testActivateAndDeactivateQuestion() = runBlocking {
        val question = Question(
            id = "q_status_test",
            testId = "bank",
            questionNumber = 1,
            subject = "Science",
            topic = "Physics",
            questionText = "What is the SI unit of electric current?",
            options = listOf("Volt", "Ampere", "Ohm", "Watt"),
            correctOptionIndex = 1,
            explanation = "Ampere is the SI base unit of electric current.",
            difficulty = "Easy",
            isActive = true
        )
        repository.saveQuestion(question)

        // Verify initially active
        var q = repository.getQuestionById("q_status_test")
        assertTrue(q?.isActive == true)

        // Deactivate
        repository.setQuestionActive("q_status_test", false)
        q = repository.getQuestionById("q_status_test")
        assertFalse(q?.isActive == true)

        // Active questions query should not return this question
        val activeQuestions = database.questionDao().getActiveQuestions()
        assertFalse(activeQuestions.any { it.id == "q_status_test" })

        // Reactivate
        repository.setQuestionActive("q_status_test", true)
        q = repository.getQuestionById("q_status_test")
        assertTrue(q?.isActive == true)
    }

    @Test
    fun testUpdateAndPermanentDeleteQuestion() = runBlocking {
        val question = Question(
            id = "q_delete_test",
            testId = "bank",
            questionNumber = 1,
            subject = "Math",
            topic = "Algebra",
            questionText = "Solve: 2x + 4 = 10",
            options = listOf("2", "3", "4", "5"),
            correctOptionIndex = 1,
            explanation = "2x = 6, so x = 3.",
            difficulty = "Easy",
            isActive = true
        )
        repository.saveQuestion(question)

        // Update
        val updated = question.copy(explanation = "Updated explanation: 2x = 6 implies x = 3.")
        val updateRes = repository.updateQuestion(updated)
        assertTrue(updateRes.isSuccess)
        assertEquals("Updated explanation: 2x = 6 implies x = 3.", repository.getQuestionById("q_delete_test")?.explanation)

        // Delete
        val deleteRes = repository.deleteQuestion("q_delete_test")
        assertTrue(deleteRes.isSuccess)
        assertNull(repository.getQuestionById("q_delete_test"))
    }
}
