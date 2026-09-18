package mayckgomes.com.racetimeapp.data.repository

import mayckgomes.com.racetimeapp.data.network.ApiResult
import mayckgomes.com.racetimeapp.data.network.safeRequest
import mayckgomes.com.racetimeapp.data.remote.JolpicaApiService
import mayckgomes.com.racetimeapp.data.local.db.CalendarDao
import mayckgomes.com.racetimeapp.data.local.db.StandingsDao
import mayckgomes.com.racetimeapp.data.local.db.entities.CalendarEntity
import mayckgomes.com.racetimeapp.data.local.db.entities.DriverStandingEntity
import mayckgomes.com.racetimeapp.data.local.db.entities.ConstructorStandingEntity
import mayckgomes.com.racetimeapp.domain.models.RacesCalendar
import mayckgomes.com.racetimeapp.domain.models.DriverPosition
import mayckgomes.com.racetimeapp.domain.models.ConstructorStandings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val CACHE_VALIDITY_MS = 7L * 24L * 60L * 60L * 1000L // 7 days

data class CacheResult<T>(val data: T?, val isStale: Boolean, val error: ApiResult<*>? = null)

class SeasonDataRepository(
    private val api: JolpicaApiService,
    private val calendarDao: CalendarDao,
    private val standingsDao: StandingsDao
) {

    suspend fun loadCalendar(isOnline: Boolean): CacheResult<List<RacesCalendar>> = withContext(Dispatchers.IO) {
        val cached = calendarDao.all()
        if (cached.isNotEmpty()) {
            val fresh = (System.currentTimeMillis() - (cached.first().fetchedAt)) <= CACHE_VALIDITY_MS
            if (fresh) return@withContext CacheResult(cached.map { RacesCalendar(it.round, it.raceName, it.date) }, false)
            else {
                if (isOnline) {
                    when (val remote = api.getSeasonCalendar()) {
                        is ApiResult.Success -> {
                            val races = remote.data.MRData.RaceTable.Races
                            calendarDao.clear()
                            calendarDao.insertAll(races.map { CalendarEntity(it.round, remote.data.MRData.RaceTable.season, it.raceName, it.date, System.currentTimeMillis()) })
                            return@withContext CacheResult(races, false)
                        }
                        else -> return@withContext CacheResult(cached.map { RacesCalendar(it.round, it.raceName, it.date) }, true, remote)
                    }
                } else {
                    return@withContext CacheResult(cached.map { RacesCalendar(it.round, it.raceName, it.date) }, true)
                }
            }
        } else {
            if (!isOnline) return@withContext CacheResult(null, false, ApiResult.NetworkError)
            when (val remote = api.getSeasonCalendar()) {
                is ApiResult.Success -> {
                    val races = remote.data.MRData.RaceTable.Races
                    calendarDao.insertAll(races.map { CalendarEntity(it.round, remote.data.MRData.RaceTable.season, it.raceName, it.date, System.currentTimeMillis()) })
                    return@withContext CacheResult(races, false)
                }
                else -> return@withContext CacheResult(null, false, remote)
            }
        }
    }

    suspend fun loadDriverStandings(isOnline: Boolean): CacheResult<List<DriverPosition>> = withContext(Dispatchers.IO) {
        val cached = standingsDao.drivers()
        if (cached.isNotEmpty()) {
            val fresh = (System.currentTimeMillis() - cached.first().fetchedAt) <= CACHE_VALIDITY_MS
            if (fresh) return@withContext CacheResult(cached.map { DriverPosition(it.position, it.points, it.wins, mayckgomes.com.racetimeapp.domain.models.Driver(it.driverId, it.givenName, it.familyName), emptyList()) }, false)
            else {
                if (isOnline) {
                    when (val remote = api.getDriverStandings()) {
                        is ApiResult.Success -> {
                            val list = remote.data.MRData.StandingsTable.StandingsLists.first().DriverStandings
                            standingsDao.clearDrivers()
                            standingsDao.insertDrivers(list.map { DriverStandingEntity(it.Driver.driverId, it.Driver.givenName, it.Driver.familyName, it.position, it.points, it.wins, System.currentTimeMillis()) })
                            return@withContext CacheResult(list, false)
                        }
                        else -> return@withContext CacheResult(cached.map { DriverPosition(it.position, it.points, it.wins, mayckgomes.com.racetimeapp.domain.models.Driver(it.driverId, it.givenName, it.familyName), emptyList()) }, true, remote)
                    }
                } else {
                    return@withContext CacheResult(cached.map { DriverPosition(it.position, it.points, it.wins, mayckgomes.com.racetimeapp.domain.models.Driver(it.driverId, it.givenName, it.familyName), emptyList()) }, true)
                }
            }
        } else {
            if (!isOnline) return@withContext CacheResult(null, false, ApiResult.NetworkError)
            when (val remote = api.getDriverStandings()) {
                is ApiResult.Success -> {
                    val list = remote.data.MRData.StandingsTable.StandingsLists.first().DriverStandings
                    standingsDao.insertDrivers(list.map { DriverStandingEntity(it.Driver.driverId, it.Driver.givenName, it.Driver.familyName, it.position, it.points, it.wins, System.currentTimeMillis()) })
                    return@withContext CacheResult(list, false)
                }
                else -> return@withContext CacheResult(null, false, remote)
            }
        }
    }

    suspend fun loadConstructorStandings(isOnline: Boolean): CacheResult<List<ConstructorStandings>> = withContext(Dispatchers.IO) {
        val cached = standingsDao.constructors()
        if (cached.isNotEmpty()) {
            val fresh = (System.currentTimeMillis() - cached.first().fetchedAt) <= CACHE_VALIDITY_MS
            if (fresh) return@withContext CacheResult(cached.map { ConstructorStandings(it.position, it.points, mayckgomes.com.racetimeapp.domain.models.Constructors(it.constructorId, it.name, "")) }, false)
            else {
                if (isOnline) {
                    when (val remote = api.getConstructorStandings()) {
                        is ApiResult.Success -> {
                            val list = remote.data.MRData.StandingsTable.StandingsLists.first().ConstructorStandings
                            standingsDao.clearConstructors()
                            standingsDao.insertConstructors(list.map { ConstructorStandingEntity(it.Constructor.constructorId, it.Constructor.name, it.position, it.points, System.currentTimeMillis()) })
                            return@withContext CacheResult(list, false)
                        }
                        else -> return@withContext CacheResult(cached.map { ConstructorStandings(it.position, it.points, mayckgomes.com.racetimeapp.domain.models.Constructors(it.constructorId, it.name, "")) }, true, remote)
                    }
                } else {
                    return@withContext CacheResult(cached.map { ConstructorStandings(it.position, it.points, mayckgomes.com.racetimeapp.domain.models.Constructors(it.constructorId, it.name, "")) }, true)
                }
            }
        } else {
            if (!isOnline) return@withContext CacheResult(null, false, ApiResult.NetworkError)
            when (val remote = api.getConstructorStandings()) {
                is ApiResult.Success -> {
                    val list = remote.data.MRData.StandingsTable.StandingsLists.first().ConstructorStandings
                    standingsDao.insertConstructors(list.map { ConstructorStandingEntity(it.Constructor.constructorId, it.Constructor.name, it.position, it.points, System.currentTimeMillis()) })
                    return@withContext CacheResult(list, false)
                }
                else -> return@withContext CacheResult(null, false, remote)
            }
        }
    }
}
