package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "mock_tests",
    indices = [
        Index(value = ["category"])
    ]
)
data class MockTestEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: String,
    val durationMinutes: Int,
    val totalQuestions: Int,
    val totalMarks: Double,
    val passingMarks: Double,
    val positiveMarksPerQuestion: Double,
    val negativeMarksPerQuestion: Double,
    val difficulty: String,
    val subjectsCovered: String, // Comma separated or JSON
    val isAvailableOffline: Boolean = true
)

@Entity(
    tableName = "questions",
    indices = [
        Index(value = ["testId"]),
        Index(value = ["subject"]),
        Index(value = ["isActive"]),
        Index(value = ["subject", "isActive"])
    ]
)
data class QuestionEntity(
    @PrimaryKey val id: String,
    val testId: String,
    val questionNumber: Int,
    val subject: String,
    val topic: String,
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctOptionIndex: Int,
    val marks: Double,
    val negativeMarks: Double,
    val explanation: String,
    val difficulty: String,
    val isActive: Boolean = true
)

@Entity(
    tableName = "test_results",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["testId"])
    ]
)
data class TestResultEntity(
    @PrimaryKey val id: String,
    val testId: String,
    val testTitle: String,
    val testCategory: String,
    val timestamp: Long,
    val durationMinutes: Int,
    val timeTakenSeconds: Int,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val wrongAnswers: Int,
    val skippedAnswers: Int,
    val score: Double,
    val maxScore: Double,
    val accuracyPercentage: Double,
    val responsesJson: String,
    val subjectBreakdownJson: String
)
