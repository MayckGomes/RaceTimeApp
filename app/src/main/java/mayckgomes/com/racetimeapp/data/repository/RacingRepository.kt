package mayckgomes.com.racetimeapp.data.repository

import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*
import mayckgomes.com.racetimeapp.data.remote.*
import mayckgomes.com.racetimeapp.domain.models.RacesCalendar
import mayckgomes.com.racetimeapp.domain.weekend.*

data class RacingData(
    val year: Int,
    val calendar: DataBlock<List<RacesCalendar>> = DataBlock(emptyList(), loading = true),
    val championship: DataBlock<ChampionshipState> = DataBlock(ChampionshipState(), loading = true),
    val results: Map<String, DataBlock<List<PublishedResult>>> = emptyMap(),
)

class RacingRepository(private val gateway: JolpicaGateway, private val clock: Clock) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    val activeYear: Int
        get() = LocalDate.now(clock.withZone(java.time.ZoneOffset.UTC)).year

    private val _data = MutableStateFlow(RacingData(activeYear))
    val data = _data.asStateFlow()
    private val dashboardLock = Mutex()
    private val championshipLock = Mutex()
    private val historySeasons = mutableSetOf<Int>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val resolver = WeekendResolver(clock)
    val context =
        combine(
                _data,
                flow {
                    while (true) {
                        emit(clock.instant())
                        delay(1000)
                    }
                },
            ) { value, _ ->
                val results = value.results.values.flatMap { it.data }.distinctBy { it.key }
                val history =
                    results
                        .filter { it.type == SessionType.RACE }
                        .map {
                            RacesCalendar(
                                it.round,
                                it.raceName,
                                it.date,
                                season = it.season,
                                Circuit = it.circuit,
                            )
                        }
                resolver.resolve(
                    Season(value.year, value.calendar.data),
                    results.map { it.key }.toSet(),
                    history,
                )
            }
            .stateIn(
                scope,
                SharingStarted.WhileSubscribed(5000),
                resolver.resolve(Season(_data.value.year, emptyList())),
            )

    suspend fun seasons(force: Boolean = false): DataBlock<List<Int>> =
        withContext(Dispatchers.IO) {
            val raw = gateway.query("seasons", "SeasonTable.Seasons", 24 * 3600_000L, force)
            DataBlock(
                raw.rows
                    .mapNotNull { it.text("season").toIntOrNull()?.takeIf { year -> year > 0 } }
                    .distinct()
                    .sortedDescending(),
                stale = raw.stale,
                failed = raw.failed,
                updatedAt = raw.updatedAt,
            )
        }

    suspend fun weekend(
        year: Int,
        round: String,
        force: Boolean = false,
    ): DataBlock<RacesCalendar?> =
        withContext(Dispatchers.IO) {
            require(year > 0 && round.toIntOrNull()?.let { it > 0 } == true)
            val fullCalendar = gateway.cached("$year/races", 6 * 3600_000L)
            val saved =
                fullCalendar?.let { raw ->
                    runCatching {
                            json
                                .decodeFromJsonElement(
                                    ListSerializer(RacesCalendar.serializer()),
                                    JsonArray(raw.rows),
                                )
                                .firstOrNull { it.season == year.toString() && it.round == round }
                        }
                        .getOrNull()
                }
            if (!force && saved != null && fullCalendar?.stale == false)
                return@withContext DataBlock<RacesCalendar?>(
                    saved,
                    updatedAt = fullCalendar.updatedAt,
                )
            val raw = gateway.query("$year/$round/races", "RaceTable.Races", 6 * 3600_000L, force)
            val parsed = runCatching {
                json
                    .decodeFromJsonElement(
                        ListSerializer(RacesCalendar.serializer()),
                        JsonArray(raw.rows),
                    )
                    .firstOrNull { it.season == year.toString() && it.round == round }
            }
            val failed =
                raw.failed ||
                    parsed.isFailure ||
                    (raw.rows.isNotEmpty() && parsed.getOrNull() == null)
            val result = parsed.getOrNull()
            if (!failed && !raw.stale) {
                val fresh =
                    raw.rows.firstOrNull {
                        it.text("season") == year.toString() && it.text("round") == round
                    }
                // Um GP confirmado pode ser atualizado/adicionado/removido sem renovar o cache da
                // temporada inteira.
                gateway.patchCached("$year/races", raw.updatedAt) { rows ->
                    val other =
                        rows.filterNot {
                            it.text("season") == year.toString() && it.text("round") == round
                        }
                    if (fresh == null) other else (other + fresh).sortedBy { it.text("date") }
                }
                _data.update { value ->
                    if (
                        value.year == year &&
                            value.calendar.updatedAt > 0 &&
                            value.calendar.updatedAt <= raw.updatedAt
                    ) {
                        val other =
                            value.calendar.data.filterNot {
                                it.season == year.toString() && it.round == round
                            }
                        value.copy(
                            calendar =
                                value.calendar.copy(
                                    data =
                                        if (result == null) other
                                        else (other + result).sortedBy { it.date }
                                )
                        )
                    } else value
                }
            }
            if (failed && result == null && saved != null)
                DataBlock(
                    saved,
                    stale = true,
                    failed = true,
                    updatedAt = fullCalendar?.updatedAt ?: 0,
                )
            else DataBlock(result, stale = raw.stale, failed = failed, updatedAt = raw.updatedAt)
        }

    suspend fun calendar(year: Int, force: Boolean = false): DataBlock<List<RacesCalendar>> =
        withContext(Dispatchers.IO) {
            val raw = gateway.query("$year/races", "RaceTable.Races", 6 * 3600_000L, force)
            val parsed = runCatching {
                json.decodeFromJsonElement(
                    ListSerializer(RacesCalendar.serializer()),
                    JsonArray(raw.rows),
                )
            }
            DataBlock(
                parsed.getOrDefault(emptyList()),
                stale = raw.stale,
                failed = raw.failed || parsed.isFailure,
                updatedAt = raw.updatedAt,
            )
        }

    // Apenas leitura local. A Home pode apresentar snapshots antes de aguardar HTTP/retries.
    suspend fun restoreHomeCache(year: Int = activeYear) =
        withContext(Dispatchers.IO) {
            if (_data.value.year != year) return@withContext
            val calendarRaw = gateway.cached("$year/races", 6 * 3600_000L)
            val savedCalendar =
                calendarRaw?.let { raw ->
                    runCatching {
                            json
                                .decodeFromJsonElement(
                                    ListSerializer(RacesCalendar.serializer()),
                                    JsonArray(raw.rows),
                                )
                                .takeIf { rows -> rows.all { it.season == year.toString() } }
                        }
                        .getOrNull()
                }
            val driversRaw =
                gateway.cached("$year/driverstandings", 10 * 60_000L)?.takeIf { raw ->
                    raw.rows.all { it.text("season") == year.toString() }
                }
            val teamsRaw =
                gateway.cached("$year/constructorstandings", 10 * 60_000L)?.takeIf { raw ->
                    raw.rows.all { it.text("season") == year.toString() }
                }
            fun info(raw: CachedJson) =
                ChampionshipStandingInfo(
                    round = raw.rows.map { it.text("round") }.distinct().singleOrNull().orEmpty(),
                    stale = raw.stale,
                    updatedAt = raw.updatedAt,
                )
            val priorCalendar =
                gateway
                    .cached("${year - 1}/races", 7 * 24 * 3600_000L)
                    ?.let { raw ->
                        runCatching {
                                json
                                    .decodeFromJsonElement(
                                        ListSerializer(RacesCalendar.serializer()),
                                        JsonArray(raw.rows),
                                    )
                                    .filter { it.season == (year - 1).toString() }
                            }
                            .getOrNull()
                    }
                    .orEmpty()
            val calendars = mapOf(year to savedCalendar.orEmpty(), year - 1 to priorCalendar)
            val savedResults = mutableMapOf<String, DataBlock<List<PublishedResult>>>()
            SessionType.entries
                .filter { it.endpoint != null }
                .forEach { type ->
                    calendars.forEach { (season, races) ->
                        (races.map { it.round }.distinct() + "last").forEach { round ->
                            val key = "$season/$round/${type.name}"
                            val ttl = if (season < year) 7 * 24 * 3600_000L else 5 * 60_000L
                            gateway.cached("$season/$round/${type.endpoint}", ttl)?.let { raw ->
                                val memory = _data.value.results[key]
                                if (memory == null || memory.updatedAt < raw.updatedAt)
                                    savedResults[key] =
                                        parseResults(raw, season.toString(), round, type)
                                else if (
                                    memory.updatedAt == raw.updatedAt && raw.stale && !memory.stale
                                )
                                    savedResults[key] = memory.copy(stale = true)
                            }
                        }
                    }
                }
            _data.update { current ->
                if (current.year != year) current
                else {
                    val old = current.championship.data
                    fun restoredInfo(
                        raw: CachedJson?,
                        previous: ChampionshipStandingInfo,
                    ): ChampionshipStandingInfo =
                        when {
                            raw == null || raw.updatedAt < previous.updatedAt -> previous
                            raw.updatedAt > previous.updatedAt -> info(raw)
                            raw.stale && !previous.stale -> previous.copy(stale = true)
                            else -> previous
                        }
                    val driversInfo = restoredInfo(driversRaw, old.driversInfo)
                    val teamsInfo = restoredInfo(teamsRaw, old.constructorsInfo)
                    val standings =
                        old.copy(
                            season = year.toString(),
                            round = driversInfo.round,
                            drivers =
                                if (driversInfo !== old.driversInfo && driversRaw != null)
                                    parseStandings(driversRaw, true)
                                else old.drivers,
                            constructors =
                                if (teamsInfo !== old.constructorsInfo && teamsRaw != null)
                                    parseStandings(teamsRaw, false)
                                else old.constructors,
                            driversInfo = driversInfo,
                            constructorsInfo = teamsInfo,
                        )
                    current.copy(
                        calendar =
                            if (
                                savedCalendar != null &&
                                    calendarRaw != null &&
                                    (calendarRaw.updatedAt > current.calendar.updatedAt ||
                                        calendarRaw.updatedAt == 0L &&
                                            current.calendar.updatedAt == 0L &&
                                            current.calendar.data.isEmpty())
                            )
                                DataBlock(
                                    savedCalendar,
                                    loading = current.calendar.loading,
                                    stale = calendarRaw.stale,
                                    updatedAt = calendarRaw.updatedAt,
                                )
                            else
                                current.calendar.copy(
                                    stale =
                                        current.calendar.stale ||
                                            (calendarRaw != null &&
                                                calendarRaw.updatedAt ==
                                                    current.calendar.updatedAt &&
                                                calendarRaw.stale)
                                ),
                        championship =
                            current.championship.copy(
                                data = standings,
                                stale = driversInfo.stale || teamsInfo.stale,
                                updatedAt = minOf(driversInfo.updatedAt, teamsInfo.updatedAt),
                            ),
                        results =
                            current.results +
                                savedResults.filter { (key, cached) ->
                                    val memory = current.results[key]
                                    memory == null ||
                                        memory.updatedAt < cached.updatedAt ||
                                        memory.updatedAt == cached.updatedAt &&
                                            cached.stale &&
                                            !memory.stale
                                },
                    )
                }
            }
        }

    suspend fun refresh(year: Int = activeYear, force: Boolean = false) =
        withContext(Dispatchers.IO) {
            dashboardLock.withLock {
                if (_data.value.year != year) {
                    _data.value = RacingData(year)
                    restoreHomeCache(year)
                }
                _data.update {
                    it.copy(
                        calendar = it.calendar.copy(loading = true),
                        championship = it.championship.copy(loading = true),
                    )
                }
                try {
                    val cal = calendar(year, force)
                    _data.update { it.copy(calendar = cal) }
                    refreshChampionship(year, force, includeHistory = false)
                    // Só sessões suportadas e com horário já alcançado são consultadas.
                    // Uma candidata recente + last por tipo evita baixar todas as corridas da
                    // temporada.
                    SessionType.entries
                        .filter { it.endpoint != null }
                        .forEach { type ->
                            val candidate =
                                cal.data
                                    .flatMap { it.sessions() }
                                    .filter {
                                        it.type == type &&
                                            it.start?.let { start -> start <= clock.instant() } ==
                                                true
                                    }
                                    .maxByOrNull { it.start!! }
                            val candidateResult =
                                candidate?.let { result(year.toString(), it.round, type, force) }
                            if (candidateResult == null || candidateResult.data.isEmpty())
                                result(year.toString(), "last", type, force)
                        }
                    // Cada tipo possui seu último resultado confirmado; uma qualificação do ano
                    // novo
                    // não pode fazer desaparecer a última corrida do ano anterior.
                    SessionType.entries
                        .filter { it.endpoint != null }
                        .forEach { type ->
                            if (
                                _data.value.results.values
                                    .flatMap { it.data }
                                    .none { it.type == type && it.season == year.toString() }
                            )
                                result((year - 1).toString(), "last", type, force)
                        }
                } finally {
                    _data.update {
                        it.copy(
                            calendar = it.calendar.copy(loading = false),
                            championship = it.championship.copy(loading = false),
                        )
                    }
                }
            }
        }

    suspend fun refreshChampionship(
        year: Int = _data.value.year,
        force: Boolean = false,
        includeHistory: Boolean = true,
    ) =
        withContext(Dispatchers.IO) {
            championshipLock.withLock {
                if (includeHistory) historySeasons.add(year)
                if (_data.value.year != year) return@withLock
                val old = _data.value.championship
                _data.update { it.copy(championship = it.championship.copy(loading = true)) }
                try {
                    val block = standings(year, force, year in historySeasons)
                    if (_data.value.year == year) _data.update { it.copy(championship = block) }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    if (_data.value.year == year)
                        _data.update {
                            it.copy(
                                championship =
                                    old.copy(
                                        loading = false,
                                        failed = true,
                                        stale = true,
                                        data =
                                            old.data.copy(
                                                driversInfo =
                                                    old.data.driversInfo.copy(
                                                        failed = true,
                                                        stale = true,
                                                    ),
                                                constructorsInfo =
                                                    old.data.constructorsInfo.copy(
                                                        failed = true,
                                                        stale = true,
                                                    ),
                                            ),
                                    )
                            )
                        }
                } finally {
                    if (_data.value.year == year)
                        _data.update {
                            it.copy(championship = it.championship.copy(loading = false))
                        }
                }
            }
        }

    private suspend fun standings(
        year: Int,
        force: Boolean,
        includeHistory: Boolean,
    ): DataBlock<ChampionshipState> {
        val driverRaw =
            gateway.query(
                "$year/driverstandings",
                "StandingsTable.StandingsLists",
                10 * 60_000L,
                force,
            )
        val teamRaw =
            gateway.query(
                "$year/constructorstandings",
                "StandingsTable.StandingsLists",
                10 * 60_000L,
                force,
            )
        val drivers = parseStandings(driverRaw, true)
        val teams = parseStandings(teamRaw, false)
        fun basicInfo(raw: CachedJson) =
            ChampionshipStandingInfo(
                round = raw.rows.map { it.text("round") }.distinct().singleOrNull().orEmpty(),
                failed = raw.failed,
                stale = raw.stale,
                updatedAt = raw.updatedAt,
            )
        val driverInfo = basicInfo(driverRaw)
        val teamInfo = basicInfo(teamRaw)
        if (_data.value.year == year)
            _data.update {
                it.copy(
                    championship =
                        DataBlock(
                            ChampionshipState(
                                year.toString(),
                                driverInfo.round,
                                drivers,
                                teams,
                                driversInfo = driverInfo,
                                constructorsInfo = teamInfo,
                            ),
                            loading = includeHistory,
                            stale = driverRaw.stale || teamRaw.stale,
                            failed = driverRaw.failed || teamRaw.failed,
                            updatedAt = minOf(driverRaw.updatedAt, teamRaw.updatedAt),
                        )
                )
            }
        val driverHistory = standingHistory(year, driverRaw, drivers, true, force, includeHistory)
        val teamHistory = standingHistory(year, teamRaw, teams, false, force, includeHistory)
        return DataBlock(
            ChampionshipState(
                year.toString(),
                driverHistory.second.round,
                drivers,
                teams,
                driverHistory.first,
                teamHistory.first,
                driverHistory.second,
                teamHistory.second,
            ),
            stale = driverRaw.stale || teamRaw.stale,
            failed = driverRaw.failed || teamRaw.failed,
            updatedAt = minOf(driverRaw.updatedAt, teamRaw.updatedAt),
        )
    }

    private suspend fun standingHistory(
        year: Int,
        raw: CachedJson,
        current: List<Standing>,
        drivers: Boolean,
        force: Boolean,
        enabled: Boolean,
    ): Pair<List<Standing>, ChampionshipStandingInfo> {
        val round = raw.rows.map { it.text("round") }.distinct().singleOrNull().orEmpty()
        val info =
            ChampionshipStandingInfo(
                round = round,
                failed = raw.failed,
                stale = raw.stale,
                updatedAt = raw.updatedAt,
            )
        val number = round.toIntOrNull() ?: return emptyList<Standing>() to info
        if (
            !enabled ||
                number <= 1 ||
                current.isEmpty() ||
                raw.rows.any { it.text("season") != year.toString() }
        )
            return emptyList<Standing>() to info
        // Standings após Sprint não bastam. Exigimos resultados publicados de Race nas duas
        // rodadas.
        val currentProof = result(year.toString(), round, SessionType.RACE, force)
        if (
            currentProof.data.none {
                it.season == year.toString() && it.round == round && it.entries.isNotEmpty()
            }
        )
            return emptyList<Standing>() to info
        val previousRound = (number - 1).toString()
        val previousProof = result(year.toString(), previousRound, SessionType.RACE, force)
        if (
            previousProof.data.none {
                it.season == year.toString() && it.round == previousRound && it.entries.isNotEmpty()
            }
        )
            return emptyList<Standing>() to info
        val endpoint = if (drivers) "driverstandings" else "constructorstandings"
        val prior =
            gateway.query(
                "$year/$previousRound/$endpoint",
                "StandingsTable.StandingsLists",
                24 * 3600_000L,
                force,
            )
        val previous = parseStandings(prior, drivers)
        val metadataMatches =
            prior.rows.isNotEmpty() &&
                prior.rows.all {
                    it.text("season") == year.toString() && it.text("round") == previousRound
                }
        if (!metadataMatches || previous.isEmpty()) return emptyList<Standing>() to info
        return previous to
            info.copy(
                previousRound = previousRound,
                comparisonReady = true,
                historyStale = raw.stale || prior.stale || currentProof.stale || previousProof.stale,
            )
    }

    private fun parseStandings(raw: CachedJson, drivers: Boolean): List<Standing> =
        orderedStandings(
            raw.rows
                .flatMap { it.items(if (drivers) "DriverStandings" else "ConstructorStandings") }
                .map { row ->
                    val person = row.obj(if (drivers) "Driver" else "Constructor")
                    val constructors = row.items("Constructors")
                    val team =
                        if (drivers) constructors.singleOrNull() ?: buildJsonObject {} else person
                    Standing(
                        person.text(if (drivers) "driverId" else "constructorId"),
                        if (drivers)
                            listOf(person.text("givenName"), person.text("familyName"))
                                .filter { it.isNotBlank() }
                                .joinToString(" ")
                        else person.text("name"),
                        row.text("position").ifBlank { row.text("positionText") },
                        row.text("points"),
                        row.text("wins"),
                        if (drivers)
                            constructors
                                .map { it.text("name") }
                                .filter(String::isNotBlank)
                                .joinToString(" / ")
                        else "",
                        team.text("constructorId"),
                        person.text("permanentNumber"),
                    )
                }
        )

    suspend fun result(
        season: String,
        round: String,
        type: SessionType,
        force: Boolean = false,
    ): DataBlock<List<PublishedResult>> {
        require(season.toIntOrNull() != null && (round == "last" || round.toIntOrNull() != null))
        val endpoint = requireNotNull(type.endpoint)
        val key = "$season/$round/${type.name}"
        val historical = season.toInt() < activeYear
        val prior = _data.value.results[key]
        val published = prior?.data?.isNotEmpty() == true
        val ttl =
            when {
                historical && published -> 7 * 24 * 3600_000L
                historical -> 6 * 3600_000L
                published -> 5 * 60_000L
                else -> 90_000L
            }
        val raw = gateway.query("$season/$round/$endpoint", "RaceTable.Races", ttl, force)
        val block = parseResults(raw, season, round, type)
        _data.update { it.copy(results = it.results + (key to block)) }
        return block
    }

    private fun parseResults(
        raw: CachedJson,
        season: String,
        round: String,
        type: SessionType,
    ): DataBlock<List<PublishedResult>> {
        val field =
            when (type) {
                SessionType.QUALIFYING -> "QualifyingResults"
                SessionType.SPRINT -> "SprintResults"
                else -> "Results"
            }
        val mismatch =
            raw.rows.any {
                it.text("season") != season || (round != "last" && it.text("round") != round)
            }
        val results =
            raw.rows
                .filter {
                    it.text("season") == season && (round == "last" || it.text("round") == round)
                }
                .groupBy { it.text("season") + "/" + it.text("round") }
                .values
                .mapNotNull { pages ->
                    val race = pages.first()
                    val entries =
                        pages
                            .flatMap { it.items(field) }
                            .map(::parseEntry)
                            .distinctBy { it.driverId }
                            .sortedBy { it.position.toIntOrNull() ?: Int.MAX_VALUE }
                    if (entries.isEmpty()) null
                    else
                        PublishedResult(
                            race.text("season"),
                            race.text("round"),
                            race.text("raceName"),
                            race.text("date"),
                            type,
                            entries,
                            runCatching {
                                    json.decodeFromJsonElement(
                                        mayckgomes.com.racetimeapp.domain.models.Circuit
                                            .serializer(),
                                        race.obj("Circuit"),
                                    )
                                }
                                .getOrNull(),
                        )
                }
        return DataBlock(
            results,
            stale = raw.stale,
            failed = raw.failed || mismatch,
            updatedAt = raw.updatedAt,
        )
    }

    private fun parseEntry(row: JsonObject): ResultEntry {
        val driver = row.obj("Driver")
        val team = row.obj("Constructor")
        val fastest = row.obj("FastestLap")
        return ResultEntry(
            row.text("position"),
            row.text("positionText"),
            driver.text("driverId"),
            "${driver.text("givenName")} ${driver.text("familyName")}".trim(),
            driver.text("permanentNumber"),
            team.text("constructorId"),
            team.text("name"),
            row.text("points"),
            row.text("grid"),
            row.text("status"),
            row.obj("Time").text("time"),
            row.text("Q1"),
            row.text("Q2"),
            row.text("Q3"),
            fastest.text("rank"),
            fastest.obj("Time").text("time"),
            fastest.text("lap"),
            row.text("laps"),
        )
    }

    suspend fun pitStops(
        season: Int,
        round: String,
        force: Boolean = false,
    ): DataBlock<List<PitStop>> =
        withContext(Dispatchers.IO) {
            require(season > 0 && round.toIntOrNull()?.let { it > 0 } == true)
            if (!supportsPitStops(season)) return@withContext DataBlock(emptyList())
            val route = "$season/$round/pitstops"
            val populated =
                gateway.cached(route, 0)?.rows?.any { it.items("PitStops").isNotEmpty() } == true
            val ttl = if (season < activeYear && populated) 7 * 24 * 3600_000L else 5 * 60_000L
            val raw = gateway.query(route, "RaceTable.Races", ttl, force)
            val mismatch =
                raw.rows.any { it.text("season") != season.toString() || it.text("round") != round }
            val groups =
                raw.rows
                    .filter { it.text("season") == season.toString() && it.text("round") == round }
                    .flatMap { it.items("PitStops") }
                    .filter { it.text("driverId").isNotBlank() }
                    .map {
                        PitStop(
                            it.text("driverId"),
                            it.text("stop"),
                            it.text("lap"),
                            it.text("duration"),
                            it.text("time"),
                        )
                    }
                    .groupBy { listOf(it.driverId, it.stop, it.lap, it.time) }
                    .values
                    .map { it.distinct() }
            val rows =
                groups
                    .mapNotNull { it.singleOrNull() }
                    .sortedWith(
                        compareBy<PitStop> { it.lap.toIntOrNull() ?: Int.MAX_VALUE }
                            .thenBy { it.driverId }
                            .thenBy { it.stop.toIntOrNull() ?: Int.MAX_VALUE }
                    )
            DataBlock(
                rows,
                failed = raw.failed || mismatch || groups.any { it.size > 1 },
                stale = raw.stale,
                updatedAt = raw.updatedAt,
            )
        }

    suspend fun lapTimes(
        season: Int,
        round: String,
        driverId: String,
        force: Boolean = false,
    ): DataBlock<List<LapTiming>> =
        withContext(Dispatchers.IO) {
            require(
                season > 0 &&
                    round.toIntOrNull()?.let { it > 0 } == true &&
                    driverId.matches(Regex("[a-zA-Z0-9_]+"))
            )
            if (!supportsLapTimes(season)) return@withContext DataBlock(emptyList())
            val route = "$season/$round/drivers/$driverId/laps"
            val populated =
                gateway.cached(route, 0)?.rows?.any {
                    it.items("Laps").any { lap -> lap.items("Timings").isNotEmpty() }
                } == true
            val ttl = if (season < activeYear && populated) 7 * 24 * 3600_000L else 5 * 60_000L
            val raw = gateway.query(route, "RaceTable.Races", ttl, force)
            val mismatch =
                raw.rows.any { race ->
                    race.text("season") != season.toString() ||
                        race.text("round") != round ||
                        race.items("Laps").any { lap ->
                            lap.items("Timings").any { it.text("driverId") != driverId }
                        }
                }
            val groups =
                raw.rows
                    .filter { it.text("season") == season.toString() && it.text("round") == round }
                    .flatMap { it.items("Laps") }
                    .flatMap { lap ->
                        val number = lap.text("number").toIntOrNull()?.takeIf { it > 0 }
                        if (number == null) emptyList()
                        else
                            lap.items("Timings")
                                .filter { it.text("driverId") == driverId }
                                .map {
                                    LapTiming(
                                        driverId,
                                        number,
                                        it.text("position"),
                                        it.text("time"),
                                    )
                                }
                    }
                    .groupBy { it.lap }
                    .values
                    .map { it.distinct() }
            val rows = groups.mapNotNull { it.singleOrNull() }.sortedBy { it.lap }
            DataBlock(
                rows,
                failed = raw.failed || mismatch || groups.any { it.size > 1 },
                stale = raw.stale,
                updatedAt = raw.updatedAt,
            )
        }

    suspend fun favoriteProfile(
        id: String,
        constructor: Boolean,
        force: Boolean = false,
    ): DataBlock<FavoriteProfile?> =
        withContext(Dispatchers.IO) {
            require(id.matches(Regex("[a-zA-Z0-9_]+")))
            val kind = if (constructor) "constructors" else "drivers"
            val path = if (constructor) "ConstructorTable.Constructors" else "DriverTable.Drivers"
            val raw = gateway.query("$kind/$id", path, 7 * 24 * 3600_000L, force)
            val row =
                raw.rows.firstOrNull {
                    it.text(if (constructor) "constructorId" else "driverId") == id
                }
            val profile =
                row?.let {
                    FavoriteProfile(
                        id,
                        if (constructor) it.text("name").trim()
                        else driverDisplayName(it.text("givenName"), it.text("familyName"), ""),
                        it.text("permanentNumber"),
                        it.text("nationality"),
                        it.text("dateOfBirth"),
                        it.text("code"),
                    )
                }
            DataBlock(
                profile,
                stale = raw.stale,
                failed = raw.failed || (raw.rows.isNotEmpty() && row == null),
                updatedAt = raw.updatedAt,
            )
        }

    suspend fun favoriteRace(
        id: String,
        constructor: Boolean,
        force: Boolean = false,
    ): DataBlock<PublishedResult?> =
        withContext(Dispatchers.IO) {
            require(id.matches(Regex("[a-zA-Z0-9_]+")))
            val kind = if (constructor) "constructors" else "drivers"
            val registered =
                if (constructor) _data.value.championship.data.constructors.any { it.id == id }
                else _data.value.championship.data.drivers.any { it.id == id }
            var stale = false
            var failed = false
            var checkedAt = 0L
            var resultCheckedAt = 0L
            suspend fun fromYear(year: Int): PublishedResult? {
                val raw =
                    gateway.query(
                        "$year/$kind/$id/results",
                        "RaceTable.Races",
                        if (year < activeYear) 7 * 24 * 3600_000L else 10 * 60_000L,
                        force,
                    )
                stale = stale || raw.stale
                failed = failed || raw.failed
                checkedAt = maxOf(checkedAt, raw.updatedAt)
                resultCheckedAt = raw.updatedAt
                return raw.rows
                    .filter { it.text("season") == year.toString() }
                    .groupBy { it.text("round") }
                    .values
                    .mapNotNull { pages ->
                        val race = pages.first()
                        val entries =
                            pages
                                .flatMap { it.items("Results") }
                                .map(::parseEntry)
                                .filter {
                                    if (constructor) it.constructorId == id else it.driverId == id
                                }
                                .distinctBy { it.driverId }
                                .sortedBy { it.position.toIntOrNull() ?: Int.MAX_VALUE }
                        if (entries.isEmpty()) null
                        else
                            PublishedResult(
                                year.toString(),
                                race.text("round"),
                                race.text("raceName"),
                                race.text("date"),
                                SessionType.RACE,
                                entries,
                                runCatching {
                                        json.decodeFromJsonElement(
                                            mayckgomes.com.racetimeapp.domain.models.Circuit
                                                .serializer(),
                                            race.obj("Circuit"),
                                        )
                                    }
                                    .getOrNull(),
                            )
                    }
                    .maxByOrNull { it.round.toIntOrNull() ?: 0 }
            }
            if (registered)
                fromYear(activeYear)?.let {
                    return@withContext DataBlock(
                        it,
                        stale = stale,
                        failed = failed,
                        updatedAt = resultCheckedAt,
                    )
                }
            val seasons =
                gateway.query("$kind/$id/seasons", "SeasonTable.Seasons", 24 * 3600_000L, force)
            stale = stale || seasons.stale
            failed = failed || seasons.failed
            checkedAt = maxOf(checkedAt, seasons.updatedAt)
            val years =
                seasons.rows
                    .mapNotNull { it.text("season").toIntOrNull() }
                    .filter { !registered || it != activeYear }
                    .distinct()
                    .sortedDescending()
            // Limita o custo de consulta; não percorre toda a carreira em background.
            for (year in years.take(2)) fromYear(year)?.let {
                return@withContext DataBlock(
                    it,
                    stale = stale,
                    failed = failed,
                    updatedAt = resultCheckedAt,
                )
            }
            val loaded =
                _data.value.results.values
                    .flatMap { it.data }
                    .filter { it.type == SessionType.RACE }
                    .mapNotNull { race ->
                        race.entries
                            .filter {
                                if (constructor) it.constructorId == id else it.driverId == id
                            }
                            .takeIf { it.isNotEmpty() }
                            ?.let { race.copy(entries = it) }
                    }
                    .maxWithOrNull(
                        compareBy<PublishedResult> { it.season.toIntOrNull() ?: 0 }
                            .thenBy { it.round.toIntOrNull() ?: 0 }
                    )
            DataBlock(
                loaded,
                stale = stale || loaded != null,
                failed = failed,
                updatedAt =
                    if (loaded == null) checkedAt
                    else
                        _data.value.results.values
                            .filter { block ->
                                block.data.any {
                                    it.season == loaded.season &&
                                        it.round == loaded.round &&
                                        it.type == SessionType.RACE
                                }
                            }
                            .maxOfOrNull { it.updatedAt } ?: 0L,
            )
        }

    suspend fun driver(id: String): List<JsonObject> {
        require(id.matches(Regex("[a-zA-Z0-9_]+")))
        return gateway.query("drivers/$id", "DriverTable.Drivers", 7 * 24 * 3600_000L).rows
    }
}
