package com.pitchpulse.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pitchpulse.core.ui.Dimens
import com.pitchpulse.ui.theme.AppSurface
import com.pitchpulse.ui.theme.AppCard

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.CardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = AppCard
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = Dimens.CardElevation
        ),
        content = content
    )
}
