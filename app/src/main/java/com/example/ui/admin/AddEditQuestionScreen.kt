package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.ExamGreen
import com.example.ui.theme.ExamGreenContainer
import com.example.ui.theme.ExamOrange
import com.example.ui.theme.ExamRed
import com.example.ui.theme.ExamRedContainer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditQuestionScreen(
    questionId: String?,
    viewModel: AddEditQuestionViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isEditing = !state.questionId.isNullOrBlank()

    LaunchedEffect(questionId) {
        viewModel.loadQuestion(questionId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Edit Question" else "Add New Question",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("add_edit_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            viewModel.saveQuestion(onSuccess = onNavigateBack)
                        },
                        enabled = !state.isSaving,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_question_button")
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving Question...")
                        } else {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEditing) "Update Question" else "Save to Question Bank",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Error banner if validation failed
                state.errorMessage?.let { errorMsg ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ExamRedContainer),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Error",
                                tint = ExamRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMsg,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = ExamRed
                            )
                        }
                    }
                }

                // Section 1: Classification (Subject, Topic, Difficulty)
                Text(
                    text = "1. Question Classification",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                // Subject TextField with quick pick chips
                Column {
                    OutlinedTextField(
                        value = state.subject,
                        onValueChange = { viewModel.onSubjectChange(it) },
                        label = { Text("Subject *") },
                        placeholder = { Text("e.g. Indian Polity, Quantitative Aptitude") },
                        isError = state.fieldErrors.containsKey("subject"),
                        supportingText = state.fieldErrors["subject"]?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("subject_input")
                    )

                    // Quick suggestion chips for subject
                    if (state.availableSubjects.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            state.availableSubjects.take(5).forEach { sub ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (state.subject == sub) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable { viewModel.onSubjectChange(sub) }
                                ) {
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = if (state.subject == sub) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Topic TextField
                OutlinedTextField(
                    value = state.topic,
                    onValueChange = { viewModel.onTopicChange(it) },
                    label = { Text("Topic *") },
                    placeholder = { Text("e.g. Fundamental Rights, Time & Work, Geometry") },
                    isError = state.fieldErrors.containsKey("topic"),
                    supportingText = state.fieldErrors["topic"]?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("topic_input")
                )

                // Difficulty selector
                Column {
                    Text(
                        text = "Difficulty Level:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("Easy", "Medium", "Hard").forEach { diff ->
                            val isSelected = state.difficulty.equals(diff, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onDifficultyChange(diff) },
                                label = { Text(diff, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when (diff) {
                                        "Easy" -> ExamGreenContainer
                                        "Hard" -> ExamRedContainer
                                        else -> ExamOrange.copy(alpha = 0.2f)
                                    },
                                    selectedLabelColor = when (diff) {
                                        "Easy" -> ExamGreen
                                        "Hard" -> ExamRed
                                        else -> ExamOrange
                                    }
                                ),
                                modifier = Modifier.testTag("difficulty_chip_$diff")
                            )
                        }
                    }
                }

                // Section 2: Question Content
                Text(
                    text = "2. Question Statement",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = state.questionText,
                    onValueChange = { viewModel.onQuestionTextChange(it) },
                    label = { Text("Question Text *") },
                    placeholder = { Text("Type complete question statement...") },
                    isError = state.fieldErrors.containsKey("questionText"),
                    supportingText = state.fieldErrors["questionText"]?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("question_text_input")
                )

                // Section 3: 4 Options with RadioButton for Correct Answer
                Text(
                    text = "3. Answer Options (Select Correct)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Fill all 4 options and tap the radio button next to the correct answer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val optionLetters = listOf("A", "B", "C", "D")
                val optionValues = listOf(state.optionA, state.optionB, state.optionC, state.optionD)
                val optionErrorKeys = listOf("optionA", "optionB", "optionC", "optionD")
                val optionChangeHandlers: List<(String) -> Unit> = listOf(
                    { viewModel.onOptionAChange(it) },
                    { viewModel.onOptionBChange(it) },
                    { viewModel.onOptionCChange(it) },
                    { viewModel.onOptionDChange(it) }
                )

                for (i in 0..3) {
                    val isCorrect = state.correctOptionIndex == i
                    OutlinedCard(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (isCorrect) ExamGreenContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            if (isCorrect) 2.dp else 1.dp,
                            if (isCorrect) ExamGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isCorrect,
                                onClick = { viewModel.onCorrectOptionSelect(i) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = ExamGreen
                                ),
                                modifier = Modifier.testTag("option_radio_$i")
                            )

                            Text(
                                text = "${optionLetters[i]}.",
                                fontWeight = FontWeight.Bold,
                                color = if (isCorrect) ExamGreen else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(end = 8.dp)
                            )

                            OutlinedTextField(
                                value = optionValues[i],
                                onValueChange = optionChangeHandlers[i],
                                placeholder = { Text("Option ${optionLetters[i]}") },
                                isError = state.fieldErrors.containsKey(optionErrorKeys[i]),
                                supportingText = state.fieldErrors[optionErrorKeys[i]]?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                                singleLine = false,
                                maxLines = 3,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("option_input_$i")
                            )
                        }
                    }
                }

                // Section 4: Detailed Solution / Explanation
                Text(
                    text = "4. Detailed Explanation",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = state.explanation,
                    onValueChange = { viewModel.onExplanationChange(it) },
                    label = { Text("Explanation / Solution *") },
                    placeholder = { Text("Explain why the correct option is right and provide concept background...") },
                    isError = state.fieldErrors.containsKey("explanation"),
                    supportingText = state.fieldErrors["explanation"]?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("explanation_input")
                )

                // Section 5: Question Status (Active/Inactive)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Active Status",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (state.isActive)
                                    "Question is active and will appear in mock tests & practice drills."
                                else
                                    "Question is inactive (hidden from student practice).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = state.isActive,
                            onCheckedChange = { viewModel.onActiveChange(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ExamGreen,
                                checkedTrackColor = ExamGreenContainer
                            ),
                            modifier = Modifier.testTag("active_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
