package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object MockTestList : Screen("mock_test_list")
    object TestInstructions : Screen("test_instructions/{testId}") {
        fun createRoute(testId: String) = "test_instructions/$testId"
    }
    object ActiveTest : Screen("active_test/{testId}") {
        fun createRoute(testId: String) = "active_test/$testId"
    }
    object Result : Screen("result/{resultId}") {
        fun createRoute(resultId: String) = "result/$resultId"
    }
    object Solutions : Screen("solutions/{resultId}") {
        fun createRoute(resultId: String) = "solutions/$resultId"
    }
    object AdaptivePractice : Screen("adaptive_practice")
    object PracticeSession : Screen("practice_session?subject={subject}&topic={topic}&difficulty={difficulty}") {
        fun createRoute(subject: String, topic: String, difficulty: String) =
            "practice_session?subject=$subject&topic=$topic&difficulty=$difficulty"
    }
    object ResultHistory : Screen("result_history")
    object AdminQuestionList : Screen("admin_question_list")
    object GeminiQuestionGenerator : Screen("admin_gemini_generator")
    object AdminBranding : Screen("admin_branding")
    object AddEditQuestion : Screen("admin_add_edit_question?questionId={questionId}") {
        fun createRoute(questionId: String? = null) =
            if (questionId.isNullOrBlank()) "admin_add_edit_question?questionId=new"
            else "admin_add_edit_question?questionId=$questionId"
    }
}
