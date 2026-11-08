"""Verificação estática da Fase 7. Não executa Gradle/JUnit/testes em aparelho."""
from pathlib import Path
import re,runpy
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
runpy.run_path(str(root/'scripts/verify_phase6.py'),run_name='__main__')
b=root/'app/src/main/java/mayckgomes/com/racetimeapp'
base=None
for folder in ['values','values-pt-rBR','values-es','values-fr','values-it']:
    entries={n.attrib['name']:''.join(n.itertext()) for n in ET.parse(root/'app/src/main/res'/folder/'statistics_strings.xml').getroot()}
    if base is None:base=entries
    assert entries.keys()==base.keys(),folder
    for key in base:
        assert re.findall(r'%[0-9]+\$[sd]',entries[key])==re.findall(r'%[0-9]+\$[sd]',base[key]),(folder,key)
repository=(b/'data/repository/RacingRepository.kt').read_text()
assert '$season/$round/pitstops' in repository and '$season/$round/drivers/$driverId/laps' in repository
assert 'RaceTable.Races' in repository and 'it.items("PitStops")' in repository and 'lap.items("Timings")' in repository
assert 'groups.any { it.size > 1 }' in repository
rules=(b/'domain/weekend/RaceStatistics.kt').read_text()
assert (
    'FinishCategory.OTHER' in rules
    and 'gridPositionCounts[grid] != 1' in rules
    and 'positionCounts[position] != 1' in rules
)
assert 'it.fastestRank == "1"' in rules and '.singleOrNull()' in rules
assert 'season >= 2011' in rules and 'season >= 1996' in rules
vm=(b/'presentation/gp/GrandPrixViewModel.kt').read_text()
assert 'canLoadRaceStatistics(' in vm and 'pitJob?.cancel()' in vm and 'lapJob?.cancel()' in vm and 'ensureActive()' in vm
assert 'selectedLapDriver?.let { loadLapTimes(season, round, it, true)' in vm
ui=(b/'presentation/gp/GrandPrixStatistics.kt').read_text()
assert 'stat_load_pits' in ui and 'stat_load_laps' in ui and 'DropdownMenu(' in ui
assert 'repository.' not in ui and 'HttpClient' not in ui and 'LaunchedEffect' not in ui
screen=(b/'presentation/gp/GrandPrixScreen.kt').read_text()
assert 'ScrollableTabRow(' in screen and 'R.string.stat_title' in screen and 'grandPrixStatistics(' in screen
assert 'rememberSaveable(season, round)' in screen
print(f'Fase 7: {len(base)} recursos nos cinco idiomas, estatísticas reais, paginação, cache, carregamento sob demanda e integração do GP conferidos estaticamente.')
