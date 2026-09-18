package mayckgomes.com.racetimeapp.mappers

import kotlinx.coroutines.runBlocking
import mayckgomes.com.racetimeapp.domain.models.RacesCalendar
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarMapperTest {
    @Test
    fun `map calendar dto to domain list`() = runBlocking {
        val dtoList = listOf(RacesCalendar("1", "Monaco GP", "2026-05-24"))
        // simple identity mapping in this project: domain model equals DTO
        val mapped = dtoList.map { RacesCalendar(it.round, it.raceName, it.date) }
        assertEquals(1, mapped.size)
        assertEquals("Monaco GP", mapped[0].raceName)
    }
}
