package mayckgomes.com.racetimeapp

import mayckgomes.com.racetimeapp.domain.weekend.*
import org.junit.Assert.*
import org.junit.Test

class FavoriteRulesTest {
    @Test
    fun savedIdsRemainVisibleEvenOutsideTheCurrentChampionship() {
        val rows =
            favoriteSummaries(
                mapOf("retired" to "Saved Driver", "active" to "Old Label"),
                listOf(Standing("active", "API Driver", "2", "20")),
                emptyMap(),
                false,
            )
        assertEquals(listOf("active", "retired"), rows.map { it.id })
        assertNull(rows.last().standing)
        assertEquals("Saved Driver", rows.last().name)
        assertEquals("API Driver", rows.first().name)
    }

    @Test
    fun profileThenSavedLabelThenStableIdProvideNamesWithoutChangingIds() {
        val details =
            mapOf(
                "driver:one" to
                    FavoriteDetails(profile = DataBlock(FavoriteProfile("one", "Profile")))
            )
        val rows =
            favoriteSummaries(
                mapOf("one" to "Old", "two" to "", "three" to "Saved"),
                emptyList(),
                details,
                false,
            )
        assertEquals("Profile", rows.single { it.id == "one" }.name)
        assertEquals("two", rows.single { it.id == "two" }.name)
        assertEquals("Saved", rows.single { it.id == "three" }.name)
    }

    @Test
    fun matchingNamesDoNotAssociateDifferentIds() {
        val row =
            favoriteSummaries(
                    mapOf("saved" to "Same Name"),
                    listOf(Standing("other", "Same Name", "1")),
                    emptyMap(),
                    false,
                )
                .single()
        assertNull(row.standing)
    }

    @Test
    fun driversAndConstructorsWithTheSameIdHaveSeparateDetails() {
        val data =
            mapOf(
                favoriteKey("same", false) to
                    FavoriteDetails(profile = DataBlock(FavoriteProfile("same", "Driver"))),
                favoriteKey("same", true) to
                    FavoriteDetails(profile = DataBlock(FavoriteProfile("same", "Team"))),
            )
        assertEquals(
            "Driver",
            favoriteSummaries(mapOf("same" to ""), emptyList(), data, false).single().name,
        )
        assertEquals(
            "Team",
            favoriteSummaries(mapOf("same" to ""), emptyList(), data, true).single().name,
        )
    }

    @Test
    fun removedIdsAreNotReintroducedByCachedProfilesOrStandings() {
        assertTrue(
            favoriteSummaries(
                    emptyMap(),
                    listOf(Standing("old", "Name")),
                    mapOf("driver:old" to FavoriteDetails()),
                    false,
                )
                .isEmpty()
        )
    }

    @Test
    fun duplicatedStandingsDoNotCreateAFabricatedRanking() {
        val row =
            favoriteSummaries(
                    mapOf("id" to "Saved"),
                    listOf(Standing("id", "Name", "1"), Standing("id", "Name", "2")),
                    emptyMap(),
                    false,
                )
                .single()
        assertNull(row.standing)
    }

    @Test
    fun bestTeamEntryUsesOnlyThatConstructorsValidPositions() {
        val result =
            PublishedResult(
                "2026",
                "1",
                "Fixture GP",
                "2026-01-01",
                SessionType.RACE,
                listOf(
                    ResultEntry(position = "1", driverId = "other", constructorId = "other"),
                    ResultEntry(position = "0", driverId = "invalid", constructorId = "team"),
                    ResultEntry(position = "5", driverId = "one", constructorId = "team"),
                    ResultEntry(position = "3", driverId = "two", constructorId = "team"),
                ),
            )
        assertEquals("two", favoriteBestEntry(result, "team", true)?.driverId)
        assertEquals("one", favoriteBestEntry(result, "one", false)?.driverId)
        assertNull(favoriteBestEntry(result, "missing", false))
    }

    @Test
    fun retirementStatusIsRetainedInsteadOfAssumingANormalFinish() {
        val entry =
            ResultEntry(position = "18", driverId = "driver", grid = "20", status = "Engine")
        val result =
            PublishedResult(
                "2026",
                "1",
                "Fixture GP",
                "2026-01-01",
                SessionType.RACE,
                listOf(entry),
            )
        assertEquals("Engine", favoriteBestEntry(result, "driver", false)?.status)
        assertNull(favoriteBestEntry(result, "driver", false)?.positionsGained)
    }
}
