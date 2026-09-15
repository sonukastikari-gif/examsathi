package com.example.ui.result

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.TestRepository
import com.example.domain.model.StudentResponseSnapshot
import com.example.domain.model.TestResult
import com.example.ui.theme.ExamGray
import com.example.ui.theme.ExamGrayContainer
import com.example.ui.theme.ExamGreen
import com.example.ui.theme.ExamGreenContainer
import com.example.ui.theme.ExamOrange
import com.example.ui.theme.ExamOrangeContainer
import com.example.ui.theme.ExamRed
import com.example.ui.theme.ExamRedContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SolutionsScreen(
    resultId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var testResult by remember { mutableStateOf<TestResult?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("All") }

    LaunchedEffect(resultId) {
        withContext(Dispatchers.IO) {
            val repo = TestRepository.getInstance(context)
            testResult = repo.getResultById(resultId)
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Solutions & Explanations", fontWeight = FontWeight.Bold)
                        testResult?.let {
                            Text(
                                text = "${it.testTitle} • Score: ${it.score}/${it.maxScore.toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (testResult == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Solutions not found.")
            }
        } else {
            val result = testResult!!
            val filteredResponses = remember(result.responses, selectedFilter) {
                when (selectedFilter) {
                    "Correct" -> result.responses.filter { it.isCorrect }
                    "Incorrect" -> result.responses.filter { it.selectedOptionIndex != null && !it.isCorrect }
                    "Skipped" -> result.responses.filter { it.selectedOptionIndex == null }
                    else -> result.responses
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Filter Chips
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "All",
                        onClick = { selectedFilter = "All" },
                        label = { Text("All (${result.totalQuestions})") },
                        modifier = Modifier.testTag("filter_all")
                    )
                    FilterChip(
                        selected = selectedFilter == "Correct",
                        onClick = { selectedFilter = "Correct" },
                        label = { Text("Correct (${result.correctAnswers})") },
                        modifier = Modifier.testTag("filter_correct")
                    )
                    FilterChip(
                        selected = selectedFilter == "Incorrect",
                        onClick = { selectedFilter = "Incorrect" },
                        label = { Text("Incorrect (${result.wrongAnswers})") },
                        modifier = Modifier.testTag("filter_incorrect")
                    )
                    FilterChip(
                        selected = selectedFilter == "Skipped",
                        onClick = { selectedFilter = "Skipped" },
                        label = { Text("Skipped (${result.skippedAnswers})") },
                        modifier = Modifier.testTag("filter_skipped")
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(filteredResponses) { index, item ->
                        SolutionItemCard(item = item, displayIndex = index + 1)
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SolutionItemCard(
    item: StudentResponseSnapshot,
    displayIndex: Int
) {
    val optionLetters = listOf("A", "B", "C", "D")

    val statusBadge = when {
        item.isCorrect -> StatusBadgeInfo(
            text = "Correct (+${item.marksEarned})",
            color = ExamGreen,
            containerColor = ExamGreenContainer
        )
        item.selectedOptionIndex != null -> StatusBadgeInfo(
            text = "Incorrect (${item.marksEarned})",
            color = ExamRed,
            containerColor = ExamRedContainer
        )
        else -> StatusBadgeInfo(
            text = "Unanswered (0.0)",
            color = ExamGray,
            containerColor = ExamGrayContainer
        )
    }

    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("solution_card_${item.questionId}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Question number, subject, and status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Q${item.questionNumber}.",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "${item.subject} • ${item.topic}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusBadge.containerColor
                ) {
                    Text(
                        text = statusBadge.text,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusBadge.color,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Question Text
            Text(
                text = item.questionText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    lineHeight = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Options Display
            item.options.forEachIndexed { optIdx, optText ->
                val isStudentSelected = item.selectedOptionIndex == optIdx
                val isCorrectAnswer = item.correctOptionIndex == optIdx

                val (bgColor, borderColor, textColor) = when {
                    isCorrectAnswer -> Triple(
                        ExamGreenContainer.copy(alpha = 0.6f),
                        ExamGreen,
                        MaterialTheme.colorScheme.onSurface
                    )
                    isStudentSelected && !isCorrectAnswer -> Triple(
                        ExamRedContainer.copy(alpha = 0.6f),
                        ExamRed,
                        MaterialTheme.colorScheme.onSurface
                    )
                    else -> Triple(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = bgColor,
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isCorrectAnswer -> ExamGreen
                                        isStudentSelected -> ExamRed
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = optionLetters.getOrElse(optIdx) { "${optIdx + 1}" },
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isCorrectAnswer || isStudentSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = optText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )

                        // Tags for Correct / Student Answer
                        if (isCorrectAnswer) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ExamGreen
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Correct", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = Color.White))
                                }
                            }
                        } else if (isStudentSelected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ExamRed
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Your Answer", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = Color.White))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Explanation / Solution Box
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Explanation",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Detailed Explanation & Takeaways",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = item.explanation,
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private data class StatusBadgeInfo(
    val text: String,
    val color: Color,
    val containerColor: Color
)
