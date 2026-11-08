package mayckgomes.com.racetimeapp

import kotlinx.serialization.json.Json
import mayckgomes.com.racetimeapp.domain.models.*
import org.junit.Assert.*
import org.junit.Test

class OptionalModelsTest {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun optionalResultFieldsMayBeMissingOrNull() {
        val entry =
            json.decodeFromString(
                LastDriverPosition.serializer(),
                """{"position":"1","points":"25","Driver":{"driverId":"fixture","givenName":"Fixture","familyName":"Driver"},"Constructor":null,"status":null}""",
            )
        assertEquals("", entry.Constructor.constructorId)
        assertEquals("", entry.status)
        assertNull(entry.Time)
    }

    @Test
    fun aDriverWithoutNumericStandingDoesNotBreakTheResponse() {
        val entry =
            json.decodeFromString(
                DriverPosition.serializer(),
                """{"positionText":"D","Driver":{"driverId":"fixture","givenName":"Fixture","familyName":"Driver"},"points":"0","wins":"0","Constructors":[]}""",
            )
        assertEquals("", entry.position)
        assertEquals("fixture", entry.Driver.driverId)
    }

    @Test
    fun unknownSessionTimesRemainUnknown() {
        val race =
            json.decodeFromString(
                RacesCalendar.serializer(),
                """{"season":"2026","round":"1","raceName":"Fixture GP","date":"2026-04-09","FirstPractice":{"date":"2026-04-06"}}""",
            )
        assertNull(race.time)
        assertNull(race.FirstPractice?.time)
        assertNull(race.ThirdPractice)
    }
}
