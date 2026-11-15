@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package mayckgomes.com.racetimeapp.components.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import mayckgomes.com.racetimeapp.R
import mayckgomes.com.racetimeapp.domain.weekend.*

@Composable
fun RaceTimeHeader(year: Int, onSettings: () -> Unit) {
    Surface(
        color = Color(0xFF080808),
        contentColor = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(50.dp)
                    .drawBehind{
                        drawCircle(
                            color = Color.Red,
                            radius = 18.dp.toPx(),
                        )
                    }
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.app_name),
                    fontFamily = mayckgomes.com.racetimeapp.ui.theme.BarlowCondensed,
                    fontSize = 26.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = .5.sp,
                )
                Text(
                    stringResource(R.string.v2_season, year),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFA0A0A0),
                )
            }
            IconButton(onClick = onSettings) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.settings_tab),
                    tint = Color.White,
                )
            }
        }
    }
}

fun sessionLabel(type: SessionType): Int =
    when (type) {
        SessionType.FP1 -> R.string.home_fp1
        SessionType.FP2 -> R.string.home_fp2
        SessionType.FP3 -> R.string.home_fp3
        SessionType.SPRINT_QUALIFYING -> R.string.home_sprint_qualifying
        SessionType.SPRINT -> R.string.home_sprint
        SessionType.QUALIFYING -> R.string.home_qualifying
        SessionType.RACE -> R.string.home_race
    }

fun weekendLabel(state: WeekendState): Int =
    when (state) {
        WeekendState.PRE_SEASON -> R.string.v2_preseason
        WeekendState.BETWEEN_WEEKENDS -> R.string.v2_between
        WeekendState.UPCOMING_WEEKEND -> R.string.v2_upcoming
        WeekendState.ACTIVE_WEEKEND -> R.string.v2_active
        WeekendState.SESSION_IN_PROGRESS -> R.string.v2_scheduled_progress
        WeekendState.AWAITING_RESULTS -> R.string.v2_awaiting
        WeekendState.WEEKEND_COMPLETED -> R.string.v2_completed
        WeekendState.SEASON_COMPLETED -> R.string.v2_season_completed
    }

fun localDate(date: String): String =
    runCatching {
            LocalDate.parse(date).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        }
        .getOrDefault(date)

fun localDateTime(instant: Instant): String =
    instant
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))

@Composable
fun SessionCountdown(seconds: Long) {
    val days = seconds / 86400
    val hours = seconds % 86400 / 3600
    val minutes = seconds % 3600 / 60
    val values =
        when {
            days > 0 ->
                listOf(
                    days to R.string.home_days,
                    hours to R.string.home_hours,
                    minutes to R.string.home_minutes,
                )
            hours > 0 ->
                listOf(
                    hours to R.string.home_hours,
                    minutes to R.string.home_minutes,
                    seconds % 60 to R.string.home_seconds,
                )
            else -> listOf(minutes to R.string.home_minutes, seconds % 60 to R.string.home_seconds)
        }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        values.forEach { (value, unit) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    value.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.displayLarge,
                )
                Text(
                    stringResource(unit),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun WeekendHeroCard(context: WeekendContext, now: Instant, stale: Boolean, onOpen: () -> Unit) {
    val race = context.weekend ?: return
    RacingCard {
        SectionLabel(stringResource(weekendLabel(context.state)), accent = true)
        Spacer(Modifier.height(12.dp))
        Text(race.raceName, style = MaterialTheme.typography.displayLarge)
        Text(
            listOf(stringResource(R.string.v2_round, race.round), race.season)
                .filter(String::isNotBlank)
                .joinToString(" · "),
            style = MaterialTheme.typography.labelLarge,
        )
        race.Circuit?.let { circuit ->
            Text(
                circuit.circuitName,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            circuit.Location?.let { location ->
                val text =
                    listOf(location.locality, location.country)
                        .filter(String::isNotBlank)
                        .joinToString(" · ")
                if (text.isNotBlank())
                    Text(
                        text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
            }
        }
        val raceStart = race.sessions().firstOrNull { it.type == SessionType.RACE }?.start
        Text(
            if (raceStart != null) stringResource(R.string.v2_race_date, localDateTime(raceStart))
            else stringResource(R.string.gp_date_utc, localDate(race.date)),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 6.dp),
        )
        HorizontalDivider(
            Modifier.padding(vertical = 14.dp),
            color = MaterialTheme.colorScheme.outline,
        )
        context.currentSession?.let { session ->
            Text(
                stringResource(sessionLabel(session.type)),
                style = MaterialTheme.typography.headlineMedium,
            )
            session.start?.let {
                Text(localDateTime(it), style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                stringResource(R.string.v2_scheduled_caution),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        val next =
            context.nextSession?.takeIf { it.season == race.season && it.round == race.round }
        if (next != null) {
            SectionLabel(stringResource(R.string.home_next_session))
            Text(
                stringResource(sessionLabel(next.type)),
                style = MaterialTheme.typography.headlineMedium,
            )
            next.start?.let { target ->
                Text(localDateTime(target), style = MaterialTheme.typography.bodyMedium)
                secondsUntil(target, now)?.let { SessionCountdown(it) }
            }
        } else if (
            context.currentSession == null &&
                context.state !in
                    setOf(WeekendState.WEEKEND_COMPLETED, WeekendState.SEASON_COMPLETED)
        ) {
            Text(
                stringResource(
                    if (race.sessions().any { it.start == null }) R.string.home_time_unknown
                    else R.string.v2_no_upcoming_session
                )
            )
        }
        Text(
            stringResource(R.string.home_local_time),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CachedNotice(stale)
        RaceActionButton(
            stringResource(R.string.v2_gp_details),
            onOpen,
            Modifier.align(Alignment.End).padding(top = 8.dp),
        )
    }
}

@Composable
fun DashboardResultCard(
    snapshot: ResultSnapshot,
    sessionDate: String?,
    types: List<SessionType>,
    onSelect: (SessionType) -> Unit,
    onRetry: () -> Unit,
    onOpen: () -> Unit,
) {
    val result = snapshot.result
    Box(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        RacingCard {
            SectionLabel(stringResource(R.string.v2_latest_result))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                types.forEach { type ->
                    FilterChip(
                        selected = result.type == type,
                        onClick = { onSelect(type) },
                        label = { Text(stringResource(sessionLabel(type))) },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        border =
                            BorderStroke(
                                1.dp,
                                if (result.type == type) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                            ),
                    )
                }
            }
            Text(result.raceName, style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(
                    if (sessionDate != null) R.string.v2_session_date else R.string.v2_race_date,
                    localDate(sessionDate ?: result.date),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            result.entries.take(3).forEach { ResultEntryRow(it, result.type) }
            DataNotice(snapshot.stale, snapshot.failed, snapshot.updatedAt, onRetry)
            RaceActionButton(
                stringResource(R.string.v2_full_result),
                onOpen,
                Modifier.align(Alignment.End).padding(top = 8.dp),
            )
        }
    }
}

@Composable
fun ResultEntryRow(entry: ResultEntry, type: SessionType, onDriver: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxWidth()
            .then(if (onDriver != null) Modifier.clickable(onClick = onDriver) else Modifier)
            .padding(vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                entry.position.ifBlank { entry.positionText.ifBlank { "—" } },
                style = MaterialTheme.typography.headlineMedium,
            )
            Column(Modifier.weight(1f)) {
                Text(entry.name, fontWeight = FontWeight.SemiBold)
                if (entry.team.isNotBlank())
                    Text(
                        entry.team,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
            }
            if (entry.points.isNotBlank())
                Text(
                    stringResource(R.string.v2_points, entry.points),
                    style = MaterialTheme.typography.labelLarge,
                )
        }
        val timing =
            if (type == SessionType.QUALIFYING)
                listOf("Q1" to entry.q1, "Q2" to entry.q2, "Q3" to entry.q3)
                    .filter { it.second.isNotBlank() }
                    .joinToString("   ") { "${it.first} ${it.second}" }
            else listOf(entry.time, entry.status).filter { it.isNotBlank() }.joinToString(" · ")
        if (timing.isNotBlank())
            Text(
                timing,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        HorizontalDivider(Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun DataNotice(stale: Boolean, failed: Boolean, updatedAt: Long, onRetry: () -> Unit) {
    if (stale || failed) {
        Text(
            stringResource(
                if (!failed) R.string.home_cached
                else if (updatedAt > 0) R.string.v2_cached_error else R.string.home_load_error
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RaceActionButton(stringResource(R.string.home_retry), onRetry)
    }
    if (updatedAt > 0 && (stale || failed))
        Text(
            stringResource(R.string.v2_updated, localDateTime(Instant.ofEpochMilli(updatedAt))),
            style = MaterialTheme.typography.bodySmall,
        )
}
