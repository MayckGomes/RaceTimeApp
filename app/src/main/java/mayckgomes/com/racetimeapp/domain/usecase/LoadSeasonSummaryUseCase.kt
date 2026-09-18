package mayckgomes.com.racetimeapp.domain.usecase

import mayckgomes.com.racetimeapp.domain.model.SeasonSummary

class LoadSeasonSummaryUseCase {
    fun execute(): SeasonSummary = SeasonSummary(
        season = "current",
        round = 1,
        currentRaceName = "Season overview",
        updatedAt = System.currentTimeMillis()
    )
}
