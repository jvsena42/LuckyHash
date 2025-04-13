package com.github.luckyhash.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DifficultyCard(
    difficultyTarget: Int,
    difficultySliderValue: Float,
    onDifficultySliderChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Lottery Difficulty",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Target difficulty: $difficultyTarget bits",
                style = MaterialTheme.typography.bodyLarge
            )

            Slider(
                value = difficultySliderValue,
                onValueChange = onDifficultySliderChange,
                valueRange = 1f..20f,
                steps = 18,
                modifier = Modifier.fillMaxWidth()
            )

            // Show difficulty explanation
            Text(
                text = getDifficultyExplanation(difficultyTarget),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun getDifficultyExplanation(difficultyTarget: Int): String {
    return when (difficultyTarget) {
        in 1..3 -> "Very easy - For testing purposes"
        in 4..8 -> "Easy - Should find matches frequently"
        in 9..12 -> "Medium - Could take hours to find a match"
        in 13..16 -> "Hard - Could take days to find a match"
        else -> "Extremely difficult - Could take years to find a match"
    }
}