package mayckgomes.com.racetimeapp

import mayckgomes.com.racetimeapp.domain.weekend.*
import org.junit.Assert.*
import org.junit.Test

class ChampionshipRulesTest {
    private val current = Standing("stable", "Fixture", "2", "100", "1")
    private val info = ChampionshipStandingInfo("3", "2", comparisonReady = true)

    @Test
    fun movementUsesStableIdRatherThanDisplayName() {
        val previous = listOf(current.copy(name = "Old display name", position = "4"))
        assertEquals(2, confirmedMovement(current, previous, info))
    }

    @Test
    fun movementCanBeUpDownOrUnchanged() {
        assertEquals(2, confirmedMovement(current, listOf(current.copy(position = "4")), info))
        assertEquals(-1, confirmedMovement(current, listOf(current.copy(position = "1")), info))
        assertEquals(0, confirmedMovement(current, listOf(current), info))
    }

    @Test
    fun firstRoundAndUnconfirmedHistoryHaveNoMovement() {
        assertNull(
            confirmedMovement(current, listOf(current), info.copy(round = "1", previousRound = "0"))
        )
        assertNull(confirmedMovement(current, listOf(current), info.copy(comparisonReady = false)))
    }

    @Test
    fun nonAdjacentOrMissingRoundsHaveNoMovement() {
        assertNull(confirmedMovement(current, listOf(current), info.copy(previousRound = "1")))
        assertNull(confirmedMovement(current, listOf(current), info.copy(previousRound = null)))
    }

    @Test
    fun missingExcludedAndAmbiguousPositionsHaveNoMovement() {
        assertNull(confirmedMovement(current.copy(position = "D"), listOf(current), info))
        assertNull(confirmedMovement(current, listOf(current.copy(position = "E")), info))
        assertNull(confirmedMovement(current, emptyList(), info))
        assertNull(confirmedMovement(current, listOf(current, current.copy(position = "4")), info))
    }

    @Test
    fun tiedPointsDoNotOverrideOfficialPositions() {
        val a = current.copy(id = "a", position = "2", points = "100", wins = "9")
        val b = current.copy(id = "b", position = "1", points = "100", wins = "0")
        assertEquals(listOf("b", "a"), orderedStandings(listOf(a, b)).map { it.id })
    }

    @Test
    fun nonNumericPositionsAreKeptAfterRankedParticipants() {
        val excluded = current.copy(id = "excluded", position = "E")
        assertEquals(listOf(current, excluded), orderedStandings(listOf(excluded, current)))
    }

    @Test
    fun identicalDuplicatesAreCollapsedButConflictingIdsAreOmitted() {
        assertEquals(listOf(current), orderedStandings(listOf(current, current)))
        assertTrue(orderedStandings(listOf(current, current.copy(position = "3"))).isEmpty())
        assertTrue(orderedStandings(listOf(current.copy(id = ""))).isEmpty())
    }
}
