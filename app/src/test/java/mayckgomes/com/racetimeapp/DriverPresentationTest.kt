package mayckgomes.com.racetimeapp

import java.time.*
import mayckgomes.com.racetimeapp.domain.weekend.*
import org.junit.Assert.*
import org.junit.Test

class DriverPresentationTest {
    private fun clock(date: String) =
        Clock.fixed(Instant.parse("${date}T12:00:00Z"), ZoneOffset.UTC)

    @Test
    fun fullNameTrimsFieldsAndDoesNotAddAnExtraSpaceForMissingParts() {
        assertEquals("Lewis Hamilton", driverDisplayName(" Lewis ", " Hamilton ", "id"))
        assertEquals("Hamilton", driverDisplayName("", "Hamilton", "id"))
        assertEquals("Original Label", driverDisplayName(" ", "", "Original Label"))
    }

    @Test
    fun namesAndInitialsPreserveAccentedCharacters() {
        assertEquals("José Hülkenberg", driverDisplayName("José", "Hülkenberg", "id"))
        assertEquals("JH", driverInitials(" José Hülkenberg "))
        assertEquals("S", driverInitials("Senna"))
        assertEquals("", driverInitials(" "))
    }

    @Test
    fun ageChangesOnTheBirthdayUsingTheInjectedClock() {
        assertEquals(24, driverAge("2000-11-02", clock("2025-11-01")))
        assertEquals(25, driverAge("2000-11-02", clock("2025-11-02")))
    }

    @Test
    fun invalidMissingAndFutureBirthDatesDoNotProduceAnAge() {
        for (date in listOf("", "?", "2000-02-30", "2030-01-01")) {
            assertNull(driverBirthDate(date, clock("2026-01-01")))
            assertNull(driverAge(date, clock("2026-01-01")))
        }
    }
}
