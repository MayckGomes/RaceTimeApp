package mayckgomes.com.racetimeapp.components.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mayckgomes.com.racetimeapp.R
import mayckgomes.com.racetimeapp.components.home.*
import mayckgomes.com.racetimeapp.domain.weekend.*

@Composable
fun FavoriteCard(
    summary: FavoriteSummary,
    year: Int,
    standingsLoading: Boolean,
    standingsFailed: Boolean,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    onRetry: () -> Unit,
    onResult: (PublishedResult) -> Unit,
) {
    val details = summary.details
    RacingCard(accent = false) {
        Text(summary.name, style = MaterialTheme.typography.headlineMedium)
        val number =
            summary.standing?.number?.takeIf(String::isNotBlank)
                ?: summary.profile?.number.orEmpty()
        if (!summary.constructor && number.isNotBlank())
            Text(
                stringResource(R.string.champ_number, number),
                style = MaterialTheme.typography.labelLarge,
            )
        summary.profile?.nationality?.takeIf(String::isNotBlank)?.let {
            Text(it, style = MaterialTheme.typography.bodySmall)
        }
        val standing = summary.standing
        if (standing != null) {
            if (standing.team.isNotBlank())
                Text(standing.team, style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(
                    R.string.v2_favorite_standing,
                    standing.position.ifBlank { "—" },
                    standing.points.ifBlank { "—" },
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (standing.wins.isNotBlank())
                Text(
                    stringResource(R.string.champ_wins, standing.wins),
                    style = MaterialTheme.typography.bodySmall,
                )
        } else
            Text(
                stringResource(
                    when {
                        standingsLoading -> R.string.fav_loading_standings
                        standingsFailed -> R.string.fav_standings_unavailable
                        else -> R.string.fav_not_ranked
                    },
                    year,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        if (details?.loading == true)
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 10.dp))
        val result = details?.recentRace?.data
        if (result != null) {
            HorizontalDivider(
                Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline,
            )
            SectionLabel(stringResource(R.string.fav_latest_race))
            Text(result.raceName, style = MaterialTheme.typography.titleMedium)
            Text(
                listOf(result.season, localDate(result.date)).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
            )
            val entry = favoriteBestEntry(result, summary.id, summary.constructor)
            if (entry != null) {
                if (!summary.constructor) {
                    Text(
                        stringResource(
                            R.string.fav_race_position,
                            entry.position.ifBlank { entry.positionText.ifBlank { "—" } },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (entry.team.isNotBlank())
                        Text(entry.team, style = MaterialTheme.typography.bodySmall)
                    if (entry.points.isNotBlank())
                        Text(
                            stringResource(R.string.v2_points, entry.points),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    val status =
                        listOf(entry.time, entry.status)
                            .filter(String::isNotBlank)
                            .joinToString(" · ")
                    if (status.isNotBlank())
                        Text(status, style = MaterialTheme.typography.bodySmall)
                } else if (entry.position.toIntOrNull()?.let { it > 0 } == true) {
                    Text(
                        stringResource(R.string.fav_best_team_position, entry.position),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            TextButton(onClick = { onResult(result) }) {
                Text(stringResource(R.string.v2_full_result))
            }
        } else if (details != null && !details.loading)
            Text(
                stringResource(
                    if (details.recentRace.failed) R.string.fav_result_unavailable
                    else R.string.fav_no_recent_result
                ),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        details?.profile?.let { DataNotice(it.stale, it.failed, it.updatedAt, onRetry) }
        details?.recentRace?.let { DataNotice(it.stale, it.failed, it.updatedAt, onRetry) }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onOpen) { Text(stringResource(R.string.fav_details)) }
            TextButton(onClick = onRemove) {
                Text(stringResource(R.string.fav_remove), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
