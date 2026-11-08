package mayckgomes.com.racetimeapp.domain.model

import java.time.Instant
import java.time.ZoneOffset
import mayckgomes.com.racetimeapp.domain.models.RacesCalendar
import mayckgomes.com.racetimeapp.domain.weekend.sessions

fun sessionInstant(date: String, time: String?): Instant? =
    time?.let { runCatching { Instant.parse("${date}T$it") }.getOrNull() }

fun nextRace(races: List<RacesCalendar>, now: Instant): RacesCalendar? =
    mayckgomes.com.racetimeapp.domain.weekend
        .WeekendResolver(java.time.Clock.fixed(now, ZoneOffset.UTC))
        .resolve(
            mayckgomes.com.racetimeapp.domain.weekend.Season(now.atZone(ZoneOffset.UTC).year, races)
        )
        .nextRace

data class ScheduledSession(val label: String, val start: Instant)

fun raceSessions(race: RacesCalendar): List<ScheduledSession> =
    race.sessions().mapNotNull { session ->
        session.start?.let { ScheduledSession(session.type.key, it) }
    }

fun upcomingSession(race: RacesCalendar, now: Instant): ScheduledSession? =
    raceSessions(race).firstOrNull { it.start > now }
