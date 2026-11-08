package mayckgomes.com.racetimeapp.domain.weekend

import java.net.URI
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

enum class CalendarEventStatus {
    RESULT_PUBLISHED,
    CURRENT,
    NEXT,
    PAST_SCHEDULE,
    UPCOMING,
    TIME_UNKNOWN,
}

enum class ResultAvailability {
    AVAILABLE,
    LOADING,
    REQUEST_FAILED,
    NOT_SCHEDULED,
    SCHEDULED,
    AWAITING_PUBLICATION,
    UNKNOWN_TIME,
    NOT_CHECKED,
    UNSUPPORTED,
}

fun calendarEventStatus(
    race: RaceWeekend,
    context: WeekendContext,
    now: Instant,
): CalendarEventStatus {
    if ("${race.season}/${race.round}/RACE" in context.published)
        return CalendarEventStatus.RESULT_PUBLISHED
    fun matches(other: RaceWeekend?) =
        other?.let { it.season == race.season && it.round == race.round } == true
    if (
        context.state in setOf(WeekendState.ACTIVE_WEEKEND, WeekendState.SESSION_IN_PROGRESS) &&
            matches(context.weekend)
    )
        return CalendarEventStatus.CURRENT
    if (matches(context.nextRace)) return CalendarEventStatus.NEXT
    val start = race.sessions().firstOrNull { it.type == SessionType.RACE }?.start
    if (start != null)
        return if (start > now) CalendarEventStatus.UPCOMING else CalendarEventStatus.PAST_SCHEDULE
    val date =
        runCatching { LocalDate.parse(race.date) }.getOrNull()
            ?: return CalendarEventStatus.TIME_UNKNOWN
    return if (date < now.atZone(ZoneOffset.UTC).toLocalDate()) CalendarEventStatus.PAST_SCHEDULE
    else CalendarEventStatus.TIME_UNKNOWN
}

fun supportedResultTypes(
    race: RaceWeekend?,
    known: Set<SessionType>,
    scheduleAvailable: Boolean,
): List<SessionType> =
    (if (scheduleAvailable) race?.sessions()?.map { it.type }.orEmpty().toSet() else emptySet())
        .plus(known)
        .filter { it.endpoint != null }
        .sortedBy {
            listOf(SessionType.QUALIFYING, SessionType.SPRINT, SessionType.RACE).indexOf(it)
        }

fun canQueryResult(session: Session, now: Instant): Boolean =
    session.type.endpoint != null &&
        (session.start?.let { it <= now }
            ?: runCatching {
                    LocalDate.parse(session.date) <= now.atZone(ZoneOffset.UTC).toLocalDate()
                }
                .getOrDefault(false))

fun resultAvailability(
    type: SessionType,
    session: Session?,
    block: DataBlock<List<PublishedResult>>?,
    loading: Boolean,
    scheduleAvailable: Boolean,
    now: Instant,
): ResultAvailability =
    when {
        type.endpoint == null -> ResultAvailability.UNSUPPORTED
        block?.data?.any { it.entries.isNotEmpty() } == true -> ResultAvailability.AVAILABLE
        loading -> ResultAvailability.LOADING
        block?.failed == true -> ResultAvailability.REQUEST_FAILED
        session == null && scheduleAvailable -> ResultAvailability.NOT_SCHEDULED
        session?.start?.let { it > now } == true -> ResultAvailability.SCHEDULED
        session != null &&
            runCatching { LocalDate.parse(session.date) > now.atZone(ZoneOffset.UTC).toLocalDate() }
                .getOrDefault(false) -> ResultAvailability.SCHEDULED
        block != null -> ResultAvailability.AWAITING_PUBLICATION
        session?.start == null -> ResultAvailability.UNKNOWN_TIME
        else -> ResultAvailability.NOT_CHECKED
    }

fun wikipediaUrl(url: String?): String? =
    url?.takeIf { value ->
        runCatching {
                val uri = URI(value)
                uri.scheme in setOf("https", "http") &&
                    uri.userInfo == null &&
                    (uri.host == "wikipedia.org" || uri.host?.endsWith(".wikipedia.org") == true)
            }
            .getOrDefault(false)
    }
