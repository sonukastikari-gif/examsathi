package com.example.ui.admin

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.remote.gemini.ReviewableGeneratedQuestion
import com.example.ui.theme.ExamGreen
import com.example.ui.theme.ExamGreenContainer
import com.example.ui.theme.ExamOrange
import com.example.ui.theme.ExamOrangeContainer
import com.example.ui.theme.ExamPurple
import com.example.ui.theme.ExamPurpleContainer
import com.example.ui.theme.ExamRed
import com.example.ui.theme.ExamRedContainer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GeminiGeneratorScreen(
    viewModel: GeminiGeneratorViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val difficulties = listOf("Easy", "Medium", "Hard")
    val questionCounts = listOf(1, 3, 5, 10)

    val topicSuggestions = when (state.selectedSubject) {
        "General Studies" -> listOf("Fundamental Rights", "Directive Principles", "Preamble", "Judiciary", "Parliament")
        "Quantitative Aptitude" -> listOf("Percentage", "Profit & Loss", "Simple Interest", "Time & Work", "Ratio & Proportion")
        "Logical Reasoning" -> listOf("Syllogism", "Blood Relations", "Seating Arrangement", "Coding-Decoding")
        "General Science" -> listOf("Thermodynamics", "Cell Biology", "Periodic Table", "Optics", "Human Anatomy")
        "Indian History" -> listOf("Indus Valley Civilization", "Mughal Empire", "1857 Revolt", "Freedom Struggle")
        "Geography" -> listOf("Himalayan Rivers", "Monsoon System", "Soil Types", "Major Minerals")
        else -> emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ExamPurpleContainer,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = ExamPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "GEMINI AI",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ExamPurple
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "AI Question Generator",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Powered by Gemini 2.5 Flash",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("gemini_back_btn")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (state.generatedQuestions.isNotEmpty()) {
                Surface(
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = { viewModel.importSelectedQuestions() },
                            enabled = !state.isLoading && state.selectedCount > 0,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ExamPurple
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("import_selected_btn")
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Import (${state.selectedCount}) to Question Bank",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier.testTag("gemini_generator_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // API key notice card if not configured
            if (!state.isApiKeyConfigured) {
                item {
                    OutlinedCard(
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = ExamOrangeContainer.copy(alpha = 0.4f)
                        ),
                        border = BorderStroke(1.dp, ExamOrange.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = ExamOrange,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Gemini API Key Setup",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ExamOrange
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "To generate questions with live AI, add GEMINI_API_KEY in your AI Studio Secrets panel. The offline question bank & tests remain 100% operational.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Error Banner
            state.errorMessage?.let { errorMsg ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ExamRedContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = ExamRed,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = errorMsg,
                                color = ExamRed,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.clearMessages() }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = ExamRed)
                            }
                        }
                    }
                }
            }

            // Success Banner
            state.importSuccessMessage?.let { successMsg ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ExamGreenContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = ExamGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = successMsg,
                                color = ExamGreen,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.clearMessages() }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = ExamGreen)
                            }
                        }
                    }
                }
            }

            // Generator Parameters Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Generation Parameters",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        // 1. Subject Selection
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Subject",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                state.availableSubjects.forEach { subj ->
                                    val isSelected = state.selectedSubject == subj
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.selectSubject(subj) },
                                        label = { Text(subj, fontSize = 13.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                                val isCustom = state.selectedSubject == "Custom"
                                FilterChip(
                                    selected = isCustom,
                                    onClick = { viewModel.selectSubject("Custom") },
                                    label = { Text("+ Custom", fontSize = 13.sp) }
                                )
                            }

                            if (state.selectedSubject == "Custom") {
                                OutlinedTextField(
                                    value = state.customSubject,
                                    onValueChange = { viewModel.setCustomSubject(it) },
                                    label = { Text("Custom Subject Name") },
                                    placeholder = { Text("e.g., International Relations") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("custom_subject_input")
                                )
                            }
                        }

                        // 2. Topic Selection
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Topic / Syllabus Focus",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            OutlinedTextField(
                                value = state.selectedTopic,
                                onValueChange = { viewModel.setTopic(it) },
                                placeholder = { Text("e.g., Fundamental Rights & Writs") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("topic_input")
                            )

                            if (topicSuggestions.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    topicSuggestions.forEach { suggestion ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            onClick = { viewModel.setTopic(suggestion) }
                                        ) {
                                            Text(
                                                text = suggestion,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Difficulty Level
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Difficulty Level",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                difficulties.forEach { diff ->
                                    val isSelected = state.selectedDifficulty == diff
                                    val chipColor = when (diff) {
                                        "Easy" -> ExamGreen
                                        "Medium" -> ExamOrange
                                        else -> ExamRed
                                    }
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setDifficulty(diff) },
                                        label = { Text(diff, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = chipColor.copy(alpha = 0.2f),
                                            selectedLabelColor = chipColor
                                        )
                                    )
                                }
                            }
                        }

                        // 4. Question Count
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Number of Questions",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                questionCounts.forEach { count ->
                                    val isSelected = state.questionCount == count
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setQuestionCount(count) },
                                        label = { Text("$count Qs") }
                                    )
                                }
                            }
                        }

                        // Generate Button
                        Button(
                            onClick = { viewModel.generateQuestions() },
                            enabled = !state.isLoading,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("generate_questions_btn")
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = state.statusMessage ?: "Generating with Gemini...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Generate ${state.questionCount} Questions",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            // Results Section Header
            if (state.generatedQuestions.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Generated Questions (${state.generatedQuestions.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${state.totalImportableCount} valid & ready to import",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row {
                            TextButton(onClick = { viewModel.selectAll(true) }) {
                                Text("Select All")
                            }
                            TextButton(onClick = { viewModel.selectAll(false) }) {
                                Text("Deselect")
                            }
                        }
                    }
                }

                // Generated Questions Items
                items(state.generatedQuestions, key = { it.id }) { item ->
                    GeneratedQuestionCard(
                        item = item,
                        onToggleSelect = { viewModel.toggleQuestionSelection(item.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun GeneratedQuestionCard(
    item: ReviewableGeneratedQuestion,
    onToggleSelect: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val question = item.question

    OutlinedCard(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            when {
                item.isDuplicate -> ExamOrange.copy(alpha = 0.7f)
                !item.validationResult.isValid -> ExamRed.copy(alpha = 0.7f)
                item.isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("generated_q_${item.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Badges and Checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status Badge
                    when {
                        item.isDuplicate -> {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ExamOrangeContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = ExamOrange,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Duplicate",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = ExamOrange
                                    )
                                }
                            }
                        }
                        !item.validationResult.isValid -> {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ExamRedContainer
                            ) {
                                Text(
                                    text = "Invalid",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ExamRed,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        else -> {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ExamGreenContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = ExamGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Valid",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = ExamGreen
                                    )
                                }
                            }
                        }
                    }

                    // Subject Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = question.subject,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Difficulty Badge
                    val diffColor = when (question.difficulty) {
                        "Easy" -> ExamGreen
                        "Medium" -> ExamOrange
                        else -> ExamRed
                    }
                    Text(
                        text = question.difficulty,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = diffColor
                    )
                }

                // Checkbox for selection (disabled if not importable)
                Checkbox(
                    checked = item.isSelected && item.isImportable,
                    onCheckedChange = { onToggleSelect() },
                    enabled = item.isImportable,
                    colors = CheckboxDefaults.colors(
                        checkedColor = ExamPurple
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Validation error reason if invalid
            if (!item.validationResult.isValid) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.validationResult.errorMessage ?: "Validation failed",
                    style = MaterialTheme.typography.bodySmall,
                    color = ExamRed
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Question Statement
            Text(
                text = question.questionText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 4 Options preview
            val prefixes = listOf("A", "B", "C", "D")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                question.options.forEachIndexed { optIndex, optText ->
                    val isCorrect = optIndex == question.correctOptionIndex
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isCorrect) ExamGreenContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = if (isCorrect) BorderStroke(1.dp, ExamGreen.copy(alpha = 0.6f)) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(if (isCorrect) ExamGreen else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = prefixes.getOrElse(optIndex) { "?" },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isCorrect) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = optText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isCorrect) ExamGreen else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (isCorrect) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Correct",
                                    tint = ExamGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Expandable Solution Explanation
            if (question.explanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = ExamPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isExpanded) "Hide Solution" else "View Solution",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = ExamPurple
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = ExamPurple,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = isExpanded) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ExamPurpleContainer.copy(alpha = 0.35f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Text(
                            text = question.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}
