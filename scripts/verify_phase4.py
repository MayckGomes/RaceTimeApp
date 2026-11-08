"""Verificação estática da Fase 4. Não executa Gradle, JUnit ou testes no aparelho."""
from pathlib import Path
import re,runpy
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
runpy.run_path(str(root/'scripts/verify_phase3.py'),run_name='__main__')
base=None
for folder in ['values','values-pt-rBR','values-es','values-fr','values-it']:
    entries={n.attrib['name']:''.join(n.itertext()) for n in ET.parse(root/'app/src/main/res'/folder/'gp_strings.xml').getroot()}
    if base is None:base=entries
    assert entries.keys()==base.keys(),folder
    for key in base:
        assert re.findall(r'%[0-9]+\$[sd]',entries[key])==re.findall(r'%[0-9]+\$[sd]',base[key]),(folder,key)
b=root/'app/src/main/java/mayckgomes/com/racetimeapp'
calendar=(b/'presentation/circuits/CalendarOverview.kt').read_text()
assert 'rememberSaveableStateHolder' in calendar and 'rememberLazyListState' in calendar and 'selectedYear' in calendar
assert 'HomeViewModel' not in calendar and 'CalendarViewModel' in calendar
vm=(b/'presentation/circuits/CalendarViewModel.kt').read_text()
assert 'calendarJob?.cancel()' in vm and 'ensureActive()' in vm
assert 'repository.refresh(' not in vm
ui=(b/'presentation/gp/GrandPrixScreen.kt').read_text()
assert 'resultAvailability(' in ui and 'SessionScheduleItem(' in ui and 'rememberSaveableStateHolder' in ui
assert 'wikipediaUrl(value)' in ui
repository=(b/'data/repository/RacingRepository.kt').read_text()
assert 'SeasonTable.Seasons' in repository and '$year/$round/races' in repository
assert 'gateway.cached("$year/races"' in repository
print(f'Fase 4: {len(base)} recursos nos cinco idiomas, navegação de temporadas, estados de resultados e cache do GP conferidos.')
