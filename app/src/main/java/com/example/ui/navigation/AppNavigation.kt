package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.admin.AddEditQuestionScreen
import com.example.ui.admin.AddEditQuestionViewModel
import com.example.ui.admin.AdminBrandingScreen
import com.example.ui.admin.AdminBrandingViewModel
import com.example.ui.admin.AdminQuestionListScreen
import com.example.ui.admin.AdminViewModel
import com.example.ui.admin.GeminiGeneratorScreen
import com.example.ui.admin.GeminiGeneratorViewModel
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.history.HistoryViewModel
import com.example.ui.history.ResultHistoryScreen
import com.example.ui.mocktest.ActiveTestScreen
import com.example.ui.mocktest.MockTestListScreen
import com.example.ui.mocktest.MockTestViewModel
import com.example.ui.mocktest.TestInstructionsScreen
import com.example.ui.practice.AdaptivePracticeScreen
import com.example.ui.practice.AdaptiveViewModel
import com.example.ui.practice.PracticeSessionScreen
import com.example.ui.result.ResultScreen
import com.example.ui.result.SolutionsScreen

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val dashboardViewModel: DashboardViewModel = viewModel()
    val adaptiveViewModel: AdaptiveViewModel = viewModel()
    val historyViewModel: HistoryViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                viewModel = dashboardViewModel,
                onNavigateToMockTests = {
                    navController.navigate(Screen.MockTestList.route)
                },
                onStartTest = { testId ->
                    navController.navigate(Screen.TestInstructions.createRoute(testId))
                },
                onNavigateToPractice = {
                    navController.navigate(Screen.AdaptivePractice.route)
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.ResultHistory.route)
                },
                onNavigateToAdmin = {
                    navController.navigate(Screen.AdminQuestionList.route)
                }
            )
        }

        composable(Screen.MockTestList.route) {
            MockTestListScreen(
                viewModel = dashboardViewModel,
                onSelectTest = { testId ->
                    navController.navigate(Screen.TestInstructions.createRoute(testId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.TestInstructions.route,
            arguments = listOf(navArgument("testId") { type = NavType.StringType })
        ) { backStackEntry ->
            val testId = backStackEntry.arguments?.getString("testId") ?: ""
            TestInstructionsScreen(
                testId = testId,
                onStartTest = { id ->
                    navController.navigate(Screen.ActiveTest.createRoute(id)) {
                        popUpTo(Screen.TestInstructions.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.ActiveTest.route,
            arguments = listOf(navArgument("testId") { type = NavType.StringType })
        ) { backStackEntry ->
            val testId = backStackEntry.arguments?.getString("testId") ?: ""
            val testViewModel: MockTestViewModel = viewModel()
            ActiveTestScreen(
                testId = testId,
                viewModel = testViewModel,
                onNavigateToResult = { resultId ->
                    navController.navigate(Screen.Result.createRoute(resultId)) {
                        popUpTo(Screen.Dashboard.route)
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Result.route,
            arguments = listOf(navArgument("resultId") { type = NavType.StringType })
        ) { backStackEntry ->
            val resultId = backStackEntry.arguments?.getString("resultId") ?: ""
            ResultScreen(
                resultId = resultId,
                onViewSolutions = { id ->
                    navController.navigate(Screen.Solutions.createRoute(id))
                },
                onRetakeTest = { testId ->
                    navController.navigate(Screen.TestInstructions.createRoute(testId))
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Solutions.route,
            arguments = listOf(navArgument("resultId") { type = NavType.StringType })
        ) { backStackEntry ->
            val resultId = backStackEntry.arguments?.getString("resultId") ?: ""
            SolutionsScreen(
                resultId = resultId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.AdaptivePractice.route) {
            AdaptivePracticeScreen(
                viewModel = adaptiveViewModel,
                onStartPracticeSession = { subject, topic, difficulty ->
                    navController.navigate(Screen.PracticeSession.createRoute(subject, topic, difficulty))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.PracticeSession.route,
            arguments = listOf(
                navArgument("subject") { type = NavType.StringType; defaultValue = "All Subjects" },
                navArgument("topic") { type = NavType.StringType; defaultValue = "All Topics" },
                navArgument("difficulty") { type = NavType.StringType; defaultValue = "Adaptive" }
            )
        ) { backStackEntry ->
            val subject = backStackEntry.arguments?.getString("subject") ?: "All Subjects"
            val topic = backStackEntry.arguments?.getString("topic") ?: "All Topics"
            val difficulty = backStackEntry.arguments?.getString("difficulty") ?: "Adaptive"

            PracticeSessionScreen(
                subject = subject,
                topic = topic,
                difficulty = difficulty,
                viewModel = adaptiveViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ResultHistory.route) {
            ResultHistoryScreen(
                viewModel = historyViewModel,
                onViewResult = { resultId ->
                    navController.navigate(Screen.Result.createRoute(resultId))
                },
                onRetakeTest = { testId ->
                    navController.navigate(Screen.TestInstructions.createRoute(testId))
                },
                onStartMockTest = {
                    navController.navigate(Screen.MockTestList.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.AdminQuestionList.route) {
            val adminViewModel: AdminViewModel = viewModel()
            AdminQuestionListScreen(
                viewModel = adminViewModel,
                onNavigateToAddQuestion = {
                    navController.navigate(Screen.AddEditQuestion.createRoute(null))
                },
                onNavigateToEditQuestion = { questionId ->
                    navController.navigate(Screen.AddEditQuestion.createRoute(questionId))
                },
                onNavigateToGeminiGenerator = {
                    navController.navigate(Screen.GeminiQuestionGenerator.route)
                },
                onNavigateToBranding = {
                    navController.navigate(Screen.AdminBranding.route)
                },
                onExitAdminMode = {
                    adminViewModel.logoutAdmin()
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.AdminBranding.route) {
            val context = LocalContext.current
            val brandingViewModel: AdminBrandingViewModel = viewModel(
                factory = AdminBrandingViewModel.Factory(context)
            )
            AdminBrandingScreen(
                viewModel = brandingViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.GeminiQuestionGenerator.route) {
            val generatorViewModel: GeminiGeneratorViewModel = viewModel()
            GeminiGeneratorScreen(
                viewModel = generatorViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.AddEditQuestion.route,
            arguments = listOf(
                navArgument("questionId") {
                    type = NavType.StringType
                    defaultValue = "new"
                }
            )
        ) { backStackEntry ->
            val questionId = backStackEntry.arguments?.getString("questionId")
            val addEditViewModel: AddEditQuestionViewModel = viewModel()
            AddEditQuestionScreen(
                questionId = questionId,
                viewModel = addEditViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
