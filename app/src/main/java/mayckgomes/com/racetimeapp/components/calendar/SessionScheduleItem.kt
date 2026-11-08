package mayckgomes.com.racetimeapp.components.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mayckgomes.com.racetimeapp.R
import mayckgomes.com.racetimeapp.components.home.*
import mayckgomes.com.racetimeapp.domain.weekend.*

@Composable
fun SessionScheduleItem(session: Session, status: SessionStatus, onResult: (() -> Unit)? = null) {
    RacingCard(accent = status == SessionStatus.SCHEDULED_IN_PROGRESS) {
        SectionLabel(stringResource(sessionLabel(session.type)), true)
        Text(
            session.start?.let(::localDateTime)
                ?: stringResource(R.string.gp_date_utc, localDate(session.date)),
            style = MaterialTheme.typography.bodyMedium,
        )
        val label =
            when (status) {
                SessionStatus.RESULT_PUBLISHED -> R.string.v2_result_published
                SessionStatus.TIME_UNKNOWN -> R.string.home_time_unknown
                SessionStatus.SCHEDULED -> R.string.v2_upcoming
                SessionStatus.SCHEDULED_IN_PROGRESS -> R.string.v2_scheduled_progress
                SessionStatus.TEMPORALLY_PAST -> R.string.v2_scheduled_elapsed
            }
        Text(
            stringResource(label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (status == SessionStatus.SCHEDULED_IN_PROGRESS)
            Text(
                stringResource(R.string.v2_scheduled_caution),
                style = MaterialTheme.typography.bodySmall,
            )
        if (session.type.endpoint == null)
            Text(
                stringResource(R.string.v2_no_results_endpoint),
                style = MaterialTheme.typography.bodySmall,
            )
        if (onResult != null)
            RaceActionButton(label = stringResource(R.string.v2_full_result), onClick = onResult)
    }
}
