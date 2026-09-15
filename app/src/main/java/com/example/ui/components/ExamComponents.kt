package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.StudentResponse
import com.example.ui.theme.ExamGray
import com.example.ui.theme.ExamGrayContainer
import com.example.ui.theme.ExamGreen
import com.example.ui.theme.ExamGreenContainer
import com.example.ui.theme.ExamOrange
import com.example.ui.theme.ExamOrangeContainer
import com.example.ui.theme.ExamPurple
import com.example.ui.theme.ExamPurpleContainer
import com.example.ui.theme.ExamRed
import com.example.ui.theme.ExamRedContainer

@Composable
fun TimerBadge(
    remainingSeconds: Int,
    modifier: Modifier = Modifier
) {
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    val isUrgent = remainingSeconds < 60
    val isWarning = remainingSeconds < 180

    val infiniteTransition = rememberInfiniteTransition(label = "timer_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isUrgent) 0.4f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "timer_alpha"
    )

    val backgroundColor = when {
        isUrgent -> ExamRedContainer
        isWarning -> ExamOrangeContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    val contentColor = when {
        isUrgent -> ExamRed
        isWarning -> ExamOrange
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor.copy(alpha = if (isUrgent) alpha else 1f),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.5f)),
        modifier = modifier.testTag("countdown_timer_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Timer",
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = timeFormatted,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = contentColor
            )
        }
    }
}

@Composable
fun QuestionOptionCard(
    optionIndex: Int,
    optionLetter: String,
    optionText: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        label = "option_border"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
        label = "option_bg"
    )

    OutlinedCard(
        onClick = onSelect,
        modifier = modifier
            .fillMaxWidth()
            .testTag("option_card_$optionIndex"),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = optionLetter,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = optionText,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuestionPaletteBottomSheet(
    sheetState: SheetState,
    totalQuestions: Int,
    currentIndex: Int,
    responses: Map<String, StudentResponse>,
    questionIds: List<String>,
    onSelectQuestion: (Int) -> Unit,
    onSubmitClick: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("question_palette_sheet")
        ) {
            Text(
                text = "Question Palette",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Palette Legend & Counters
            val (answeredCount, reviewCount, unansweredCount) = remember(responses, questionIds) {
                var answered = 0
                var review = 0
                var unanswered = 0
                questionIds.forEach { id ->
                    val resp = responses[id]
                    val isAns = resp?.selectedOptionIndex != null
                    val isRev = resp?.isMarkedForReview ?: false
                    if (isRev) {
                        review++
                    } else if (isAns) {
                        answered++
                    } else {
                        unanswered++
                    }
                }
                Triple(answered, review, unanswered)
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LegendBadge(label = "Answered ($answeredCount)", color = ExamGreen, containerColor = ExamGreenContainer)
                LegendBadge(label = "For Review ($reviewCount)", color = ExamPurple, containerColor = ExamPurpleContainer)
                LegendBadge(label = "Not Answered ($unansweredCount)", color = ExamGray, containerColor = ExamGrayContainer)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tap any question to navigate directly:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Grid of question buttons
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                items(
                    count = totalQuestions,
                    key = { index -> questionIds.getOrNull(index) ?: index.toString() }
                ) { index ->
                    val qId = questionIds.getOrNull(index) ?: ""
                    val resp = responses[qId]
                    val isAnswered = resp?.selectedOptionIndex != null
                    val isReview = resp?.isMarkedForReview ?: false
                    val isCurrent = index == currentIndex

                    val (bgColor, txtColor) = when {
                        isReview && isAnswered -> Pair(ExamPurple, Color.White)
                        isReview -> Pair(ExamOrange, Color.White)
                        isAnswered -> Pair(ExamGreen, Color.White)
                        else -> Pair(ExamGrayContainer, ExamGray)
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgColor)
                            .then(
                                if (isCurrent) {
                                    Modifier.background(bgColor).padding(2.dp)
                                } else Modifier
                            )
                            .clickable {
                                onSelectQuestion(index)
                                onDismiss()
                            }
                            .testTag("palette_item_$index"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium
                            ),
                            color = txtColor
                        )

                        // Small badge icon for review
                        if (isReview) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = "Review",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(10.dp)
                                    .align(Alignment.TopEnd)
                                    .padding(1.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onDismiss()
                    onSubmitClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("palette_submit_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Submit Test Now", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun LegendBadge(label: String, color: Color, containerColor: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        modifier = Modifier.padding(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = color
            )
        }
    }
}

@Composable
fun SubmitConfirmationDialog(
    totalQuestions: Int,
    answeredCount: Int,
    unansweredCount: Int,
    reviewCount: Int,
    onConfirmSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Submit Test?",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Are you sure you want to finish and submit your test? Once submitted, your score will be calculated and answers cannot be changed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SummaryRow(label = "Total Questions:", value = "$totalQuestions", color = MaterialTheme.colorScheme.onSurface)
                        SummaryRow(label = "Answered:", value = "$answeredCount", color = ExamGreen)
                        SummaryRow(label = "Unanswered:", value = "$unansweredCount", color = ExamGray)
                        SummaryRow(label = "Marked for Review:", value = "$reviewCount", color = ExamPurple)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmSubmit,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("dialog_confirm_submit_btn")
            ) {
                Text("Submit Test", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_resume_btn")
            ) {
                Text("Resume Test")
            }
        }
    )
}

@Composable
private fun SummaryRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = color)
    }
}
