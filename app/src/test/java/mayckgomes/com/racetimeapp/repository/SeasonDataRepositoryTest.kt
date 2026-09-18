package mayckgomes.com.racetimeapp.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import mayckgomes.com.racetimeapp.data.local.db.AppDatabase
import mayckgomes.com.racetimeapp.data.local.db.CalendarDao
import mayckgomes.com.racetimeapp.data.local.db.StandingsDao
import mayckgomes.com.racetimeapp.data.remote.JolpicaApiService
import mayckgomes.com.racetimeapp.data.repository.SeasonDataRepository
import mayckgomes.com.racetimeapp.data.network.ApiResult
import mayckgomes.com.racetimeapp.domain.models.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SeasonDataRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var calendarDao: CalendarDao
    private lateinit var standingsDao: StandingsDao
    private lateinit var repo: SeasonDataRepository

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        calendarDao = db.calendarDao()
        standingsDao = db.standingsDao()

        val stubApi = object : JolpicaApiService(io.ktor.client.HttpClient()) {
            override suspend fun getSeasonCalendar(): ApiResult<SeasonCalendarResponse> = ApiResult.NetworkError
            override suspend fun getDriverStandings(): ApiResult<driversStadingsResponse> = ApiResult.NetworkError
            override suspend fun getConstructorStandings(): ApiResult<TeamsStandingsResponse> = ApiResult.NetworkError
        }

        repo = SeasonDataRepository(stubApi, calendarDao, standingsDao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `no cache and offline returns network error`() = runBlocking {
        val res = repo.loadCalendar(isOnline = false)
        assertNull(res.data)
        assertTrue(res.error is ApiResult.NetworkError)
    }
}
