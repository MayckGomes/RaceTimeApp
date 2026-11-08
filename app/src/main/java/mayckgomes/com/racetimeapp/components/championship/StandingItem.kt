package mayckgomes.com.racetimeapp.components.championship

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mayckgomes.com.racetimeapp.R
import mayckgomes.com.racetimeapp.domain.weekend.Standing
import mayckgomes.com.racetimeapp.ui.theme.constructorColor

@Composable
fun StandingItem(
    row: Standing,
    delta: Int?,
    favorite: Boolean,
    constructor: Boolean,
    onOpen: () -> Unit,
) {
    val podium = row.position.toIntOrNull()?.let { it in 1..3 } == true
    val teamColor =
        constructorColor(
            if (constructor) row.id else row.constructorId,
            MaterialTheme.colorScheme.outline,
        )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border =
            BorderStroke(
                1.dp,
                if (podium) MaterialTheme.colorScheme.primary.copy(alpha = .45f)
                else MaterialTheme.colorScheme.outline,
            ),
    ) {
        Column(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        row.position.ifBlank { "—" },
                        style = MaterialTheme.typography.headlineMedium,
                        color =
                            if (podium) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                    )
                    if (delta != null) MovementIndicator(delta)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        row.name.ifBlank { row.id },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (row.team.isNotBlank())
                        Text(
                            row.team,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    if (!constructor && row.number.isNotBlank())
                        Text(
                            stringResource(R.string.champ_number, row.number),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        row.points.ifBlank { "—" },
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        stringResource(R.string.home_points),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    if (favorite)
                        Icon(
                            Icons.Outlined.Star,
                            contentDescription = stringResource(R.string.champ_favorite),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 6.dp).size(18.dp),
                        )
                }
            }
            HorizontalDivider(
                Modifier.padding(top = 12.dp),
                thickness = 2.dp,
                color = teamColor.copy(alpha = .65f),
            )
            if (row.wins.isNotBlank())
                Text(
                    stringResource(R.string.champ_wins, row.wins),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
        }
    }
}

@Composable
fun MovementIndicator(delta: Int) {
    val description =
        stringResource(
            when {
                delta > 0 -> R.string.champ_moved_up
                delta < 0 -> R.string.champ_moved_down
                else -> R.string.champ_unchanged
            },
            kotlin.math.abs(delta),
        )
    val dark = MaterialTheme.colorScheme.background.red < .5f
    val color =
        when {
            delta > 0 -> if (dark) Color(0xFF81C995) else Color(0xFF146C2E)
            delta < 0 -> if (dark) Color(0xFFFFB4AB) else Color(0xFFBA1A1A)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    Text(
        when {
            delta > 0 -> "↑ $delta"
            delta < 0 -> "↓ ${-delta}"
            else -> "="
        },
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier.semantics { contentDescription = description },
    )
}
