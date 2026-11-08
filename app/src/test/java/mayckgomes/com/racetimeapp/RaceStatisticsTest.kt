package mayckgomes.com.racetimeapp

import kotlinx.serialization.json.Json
import mayckgomes.com.racetimeapp.domain.weekend.*
import org.junit.Assert.*
import org.junit.Test

class RaceStatisticsTest {
    private fun driver(id: String, position: String, grid: String, status: String = "Finished") =
        ResultEntry(
            driverId = id,
            name = "Fixture $id",
            position = position,
            grid = grid,
            status = status,
        )

    private fun race(vararg entries: ResultEntry, type: SessionType = SessionType.RACE) =
        PublishedResult("2026", "1", "Fixture GP", "2026-04-01", type, entries.toList())

    @Test
    fun emptyAndQualifyingResultsDoNotCreateRaceStatistics() {
        assertNull(raceStatistics(race()))
        assertNull(raceStatistics(race(driver("one", "1", "3"), type = SessionType.QUALIFYING)))
    }

    @Test
    fun podiumUsesOfficialPositionsInsteadOfResponseOrder() {
        val stats =
            raceStatistics(
                race(
                    driver("third", "3", "3"),
                    driver("winner", "1", "1"),
                    driver("second", "2", "2"),
                )
            )!!
        assertEquals(listOf("winner", "second", "third"), stats.podium.map { it.driverId })
    }

    @Test
    fun duplicateFinalPositionsDoNotCreateAWinnerOrPositionChange() {
        val stats = raceStatistics(race(driver("one", "1", "2"), driver("two", "1", "3")))!!
        assertTrue(stats.podium.isEmpty())
        assertTrue(stats.changes.isEmpty())
    }

    @Test
    fun onlyConfirmedFinishesHavePositionChanges() {
        val stats =
            raceStatistics(
                race(driver("dnf", "10", "20", "Engine"), driver("finished", "2", "4", "+1 Lap"))
            )!!
        assertNull(stats.changes["dnf"])
        assertEquals(2, stats.changes["finished"])
        assertEquals(listOf("dnf"), stats.groups[FinishCategory.RETIRED]?.map { it.driverId })
    }

    @Test
    fun gridZeroDoesNotBecomeAPitLanePositionGain() {
        assertTrue(raceStatistics(race(driver("one", "4", "0")))!!.changes.isEmpty())
    }

    @Test
    fun missingOrInvalidGridIsOmitted() {
        for (grid in listOf("", "-1", "?")) assertTrue(
            raceStatistics(race(driver("one", "4", grid)))!!.changes.isEmpty()
        )
    }

    @Test
    fun nonNumericOrContradictoryPositionTextPreventsPositionChange() {
        for (text in listOf("R", "D", "3")) {
            val entry = driver("one", "2", "4").copy(positionText = text)
            assertNull(entry.positionsGained)
            assertTrue(raceStatistics(race(entry))!!.changes.isEmpty())
        }
    }

    @Test
    fun duplicateGridPositionsAreNotUsedToCalculateMovement() {
        assertTrue(
            raceStatistics(race(driver("one", "1", "3"), driver("two", "2", "3")))!!
                .changes
                .isEmpty()
        )
    }

    @Test
    fun tiesInLargestGainKeepBothDrivers() {
        val stats = raceStatistics(race(driver("one", "1", "4"), driver("two", "2", "5")))!!
        assertEquals(setOf("one", "two"), stats.mostGained.map { it.entry.driverId }.toSet())
        assertEquals(listOf(3, 3), stats.mostGained.map { it.delta })
    }

    @Test
    fun lostAndUnchangedPositionsAreShownButDoNotCreateAPositiveHighlight() {
        val stats = raceStatistics(race(driver("one", "3", "1"), driver("two", "2", "2")))!!
        assertEquals(-2, stats.changes["one"])
        assertEquals(0, stats.changes["two"])
        assertTrue(stats.mostGained.isEmpty())
    }

    @Test
    fun unknownStatusDoesNotAutomaticallyBecomeARetirement() {
        assertEquals(
            FinishCategory.OTHER,
            finishCategory(driver("one", "18", "20", "Unknown source status")),
        )
        assertEquals(
            FinishCategory.OTHER,
            finishCategory(driver("one", "18", "20", "Not classified")),
        )
    }

    @Test
    fun disqualificationAndNonStartsAreSeparateFromRetirements() {
        assertEquals(
            FinishCategory.DISQUALIFIED,
            finishCategory(driver("one", "18", "20", "Disqualified")),
        )
        assertEquals(
            FinishCategory.NOT_STARTED,
            finishCategory(driver("one", "18", "20", "Did not start")),
        )
        assertEquals(
            FinishCategory.NOT_STARTED,
            finishCategory(driver("one", "18", "20", "Withdrawn")),
        )
    }

    @Test
    fun disqualifiedAndNonStartingEntriesDoNotBecomePodiumHighlights() {
        val stats =
            raceStatistics(
                race(
                    driver("one", "1", "1", "Disqualified"),
                    driver("two", "2", "2", "Did not start"),
                )
            )!!
        assertTrue(stats.podium.isEmpty())
    }

    @Test
    fun fastestLapRequiresExplicitRankOneAndAValidTime() {
        val entry =
            driver("one", "4", "4")
                .copy(fastestRank = "1", fastestTime = "1:23.456", fastestLap = "32")
        assertEquals("one", raceStatistics(race(entry))?.fastest?.driverId)
        assertNull(raceStatistics(race(entry.copy(fastestRank = "")))?.fastest)
        assertNull(raceStatistics(race(entry.copy(fastestTime = "?")))?.fastest)
    }

    @Test
    fun ambiguousOrDisqualifiedFastestLapIsNotInvented() {
        val entry = driver("one", "4", "4").copy(fastestRank = "1", fastestTime = "1:23.456")
        assertNull(
            raceStatistics(race(entry, entry.copy(driverId = "two", position = "5", grid = "5")))
                ?.fastest
        )
        assertNull(raceStatistics(race(entry.copy(status = "Disqualified")))?.fastest)
    }

    @Test
    fun timingParserHandlesSecondsMinutesHoursAndRejectsInvalidOrOverflowingValues() {
        assertEquals(23_456L, timingMillis("23.456"))
        assertEquals(83_456L, timingMillis("1:23.456"))
        assertEquals(3_683_456L, timingMillis("1:01:23.456"))
        for (value in
            listOf(
                "0.000",
                "-1:23.456",
                "1:60.000",
                "1:23",
                "+1.234",
                "999999999999999999999.000",
            )) assertNull(timingMillis(value))
    }

    @Test
    fun historicalCoverageAndGpIdentityGuardExtraRequests() {
        assertFalse(supportsPitStops(2010))
        assertTrue(supportsPitStops(2011))
        assertFalse(supportsLapTimes(1995))
        assertTrue(supportsLapTimes(1996))
        val result = race(driver("one", "1", "1"))
        assertTrue(canLoadRaceStatistics(result, 2026, "1"))
        assertFalse(canLoadRaceStatistics(result, 2025, "1"))
        assertFalse(canLoadRaceStatistics(result, 2026, "2"))
        assertFalse(canLoadRaceStatistics(result.copy(type = SessionType.SPRINT), 2026, "1"))
    }

    @Test
    fun duplicateOrMissingDriverIdsAreNotUsedForHighlights() {
        val entry = driver("one", "1", "2")
        val stats =
            raceStatistics(race(entry, entry.copy(position = "2"), entry.copy(driverId = "")))!!
        assertTrue(stats.podium.isEmpty())
        assertTrue(stats.changes.isEmpty())
    }

    @Test
    fun optionalNewResultFieldsKeepOldSerializedResultsReadable() {
        val entry =
            Json.decodeFromString(ResultEntry.serializer(), """{"driverId":"one","position":"1"}""")
        assertEquals("", entry.fastestLap)
        assertEquals("", entry.completedLaps)
    }

    @Test
    fun sprintUsesItsOwnPublishedResultAndNeverQualifyingGrid() {
        val stats = raceStatistics(race(driver("one", "1", "3"), type = SessionType.SPRINT))!!
        assertEquals(2, stats.changes["one"])
    }

    @Test
    fun completedStatusAcceptsWhitespaceAndLappedFinishesButNotZeroLaps() {
        assertTrue(driver("one", "2", "3", " Finished ").classifiedFinish)
        assertTrue(driver("one", "2", "3", "+2 Laps").classifiedFinish)
        assertFalse(driver("one", "2", "3", "+0 Laps").classifiedFinish)
    }
}
