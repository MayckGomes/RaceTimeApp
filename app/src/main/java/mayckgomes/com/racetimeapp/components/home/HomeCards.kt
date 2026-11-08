package mayckgomes.com.racetimeapp.components.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import mayckgomes.com.racetimeapp.R
import mayckgomes.com.racetimeapp.domain.model.sessionInstant
import mayckgomes.com.racetimeapp.domain.models.*

@Composable
fun RacingMark(modifier: Modifier = Modifier) {
    val red = MaterialTheme.colorScheme.primary
    Canvas(modifier.size(28.dp, 22.dp)) {
        for (i in 0..1) {
            val x = size.width * i * .43f
            drawPath(
                Path().apply {
                    moveTo(x, size.height)
                    lineTo(x + size.width * .4f, 0f)
                    lineTo(x + size.width * .65f, 0f)
                    lineTo(x + size.width * .25f, size.height)
                    close()
                },
                red,
            )
        }
    }
}

@Composable
fun RacingCard(accent: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    val color = MaterialTheme.colorScheme.primary
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(Modifier.fillMaxWidth()) {
            if (accent)
                Canvas(Modifier.matchParentSize()) {
                    drawPath(
                        Path().apply {
                            moveTo(12.dp.toPx(), 0f)
                            lineTo(2.dp.toPx(), 12.dp.toPx())
                            lineTo(2.dp.toPx(), size.height - 12.dp.toPx())
                            lineTo(12.dp.toPx(), size.height)
                        },
                        color,
                        style = Stroke(3.dp.toPx()),
                    )
                }
            Column(Modifier.fillMaxWidth().padding(18.dp), content = content)
        }
    }
}

@Composable
fun SectionLabel(text: String, accent: Boolean = false) {
    Text(
        text.uppercase(Locale.getDefault()),
        style = MaterialTheme.typography.labelLarge,
        color =
            if (accent) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun CachedNotice(stale: Boolean) {
    if (stale)
        Text(
            stringResource(R.string.home_cached),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
}

@Composable
fun NextRaceCard(race: RacesCalendar, totalRounds: Int, countdown: Long?, isStale: Boolean) {
    RacingCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                SectionLabel(stringResource(R.string.home_next_race), true)
                Spacer(Modifier.height(10.dp))
                Text(
                    race.raceName.uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.displayLarge,
                )
                Spacer(Modifier.height(8.dp))
                race.Circuit?.let {
                    Text(it.circuitName, style = MaterialTheme.typography.bodyMedium)
                    it.Location?.let { location ->
                        Text(
                            listOf(location.locality, location.country)
                                .filter(String::isNotBlank)
                                .joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                SectionLabel(stringResource(R.string.home_round))
                Text(race.round.padStart(2, '0'), style = MaterialTheme.typography.displayLarge)
                Text(
                    "/ $totalRounds",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(12.dp))
        val start = sessionInstant(race.date, race.time)
        Text(
            start
                ?.atZone(ZoneId.systemDefault())
                ?.format(
                    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                ) ?: race.date,
            style = MaterialTheme.typography.bodySmall,
        )
        if (countdown != null) {
            Spacer(Modifier.height(12.dp))
            val values =
                listOf(
                    countdown / 86400,
                    countdown % 86400 / 3600,
                    countdown % 3600 / 60,
                    countdown % 60,
                )
            val labels =
                listOf(
                    R.string.home_days,
                    R.string.home_hours,
                    R.string.home_minutes,
                    R.string.home_seconds,
                )
            Row(Modifier.fillMaxWidth()) {
                values.forEachIndexed { i, value ->
                    if (i > 0)
                        VerticalDivider(
                            Modifier.height(42.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = .45f),
                        )
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            value.toString().padStart(2, '0'),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            stringResource(labels[i]),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else
            Text(
                stringResource(R.string.home_time_unknown),
                style = MaterialTheme.typography.bodySmall,
            )
        CachedNotice(isStale)
    }
}

data class StandingRow(
    val position: String,
    val name: String,
    val detail: String,
    val points: String,
    val color: Color,
)

@Composable
fun StandingsCard(title: String, rows: List<StandingRow>, stale: Boolean, onClick: () -> Unit) {
    RacingCard(accent = false) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RacingMark(Modifier)
            Spacer(Modifier.width(8.dp))
            Text(
                title.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                "TOP 3",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        if (rows.isEmpty()) Text(stringResource(R.string.home_no_standings))
        rows.forEach { row ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(row.position, style = MaterialTheme.typography.headlineMedium)
                VerticalDivider(Modifier.height(34.dp).width(3.dp), color = row.color)
                Column(Modifier.weight(1f)) {
                    Text(
                        row.name,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (row.detail.isNotBlank())
                        Text(
                            row.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(row.points, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        stringResource(R.string.home_points),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
        CachedNotice(stale)
        RaceActionButton(
            stringResource(R.string.home_view_all),
            onClick,
            Modifier.align(Alignment.End).padding(top = 8.dp),
        )
    }
}
