package com.pitchpulse.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pitchpulse.data.model.FootballQuote
import com.pitchpulse.data.model.LeagueTodaySummary
import com.pitchpulse.data.model.QuizQuestion
import com.pitchpulse.ui.theme.*

@Composable
fun LeagueTodayCard(summary: LeagueTodaySummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🏆 ${summary.name}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            val label = if (summary.matchCount == 1) "match" else "matches"
            Text(
                text = "${summary.matchCount} $label today",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun FootballQuoteCard(quote: FootballQuote) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quote of the day",
                style = MaterialTheme.typography.labelMedium,
                color = AppAccentMuted
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "\"${quote.text}\"",
                style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "— ${quote.author}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun FootballFactCard(fact: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppAccentSoft)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Did you know?",
                style = MaterialTheme.typography.labelMedium,
                color = AppAccent
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = fact,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun FootballQuizCard(
    quiz: QuizQuestion,
    selectedOptionIndex: Int?,
    onOptionSelected: (Int) -> Unit,
    onNextQuestion: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Football Quiz",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = quiz.question,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            quiz.options.forEachIndexed { index, option ->
                val answered = selectedOptionIndex != null
                val isSelected = selectedOptionIndex == index
                val isCorrect = index == quiz.correctIndex
                val containerColor = when {
                    !answered -> AppSurface
                    isCorrect -> AppAccentSoft
                    isSelected -> AppError.copy(alpha = 0.2f)
                    else -> AppSurface
                }
                val borderColor = when {
                    !answered -> TextMuted
                    isCorrect -> AppAccent
                    isSelected -> AppError
                    else -> TextMuted.copy(alpha = 0.4f)
                }
                OutlinedButton(
                    onClick = { if (!answered) onOptionSelected(index) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    enabled = !answered,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = containerColor,
                        contentColor = TextPrimary,
                        disabledContainerColor = containerColor,
                        disabledContentColor = TextPrimary
                    ),
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Text(
                        text = option,
                        modifier = Modifier.padding(vertical = 4.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            if (selectedOptionIndex != null) {
                Spacer(modifier = Modifier.height(12.dp))
                val correct = selectedOptionIndex == quiz.correctIndex
                Text(
                    text = if (correct) "Correct! Great knowledge." else "Not quite — the answer is ${quiz.options[quiz.correctIndex]}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (correct) AppAccent else AppError
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onNextQuestion) {
                    Text("Next question", color = AppAccent)
                }
            }
        }
    }
}

@Composable
fun HomeSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = TextPrimary,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun HomeExtrasLoadingRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = AppAccent
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text("Loading quiz & facts…", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}
