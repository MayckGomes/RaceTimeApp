package mayckgomes.com.racetimeapp.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingsActionButton(
    label: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
    content: @Composable RowScope.() -> Unit = {
        Text(label.orEmpty(), fontWeight = FontWeight.SemiBold)
    },
) {
    val shape = RoundedCornerShape(12.dp)
    val buttonModifier = modifier.heightIn(min = 48.dp)

    if (primary) {
        Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            shape = shape,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            content = content,
        )
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            shape = shape,
            border =
                BorderStroke(
                    1.dp,
                    if (enabled) MaterialTheme.colorScheme.outline
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                ),
            colors =
                ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            content = content,
        )
    }
}

@Composable
internal fun SettingsChoiceChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors =
        FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )

    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp),
        enabled = enabled,
        label = {
            Text(label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        },
        colors = colors,
        border =
            BorderStroke(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
            ),
        shape = RoundedCornerShape(10.dp),
    )
}
