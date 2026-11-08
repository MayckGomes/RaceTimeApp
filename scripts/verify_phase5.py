"""Verificação estática da Fase 5. Não executa Gradle, JUnit ou testes no aparelho."""
from pathlib import Path
import re,runpy
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
runpy.run_path(str(root/'scripts/verify_phase4.py'),run_name='__main__')
base=None
for folder in ['values','values-pt-rBR','values-es','values-fr','values-it']:
    entries={n.attrib['name']:''.join(n.itertext()) for n in ET.parse(root/'app/src/main/res'/folder/'favorites_strings.xml').getroot()}
    if base is None:base=entries
    assert entries.keys()==base.keys(),folder
    for key in base:
        assert re.findall(r'%[0-9]+\$[sd]',entries[key])==re.findall(r'%[0-9]+\$[sd]',base[key]),(folder,key)
b=root/'app/src/main/java/mayckgomes/com/racetimeapp'
for name in ['driversRepository.kt','teamsRepository.kt']:
    path=b/'data/repository'/name
    assert path.exists(),name
store=(b/'data/local/FavoritesStore.kt').read_text()
assert 'driversFavorites' in store and 'teamsFavorites' in store and '.clear(' not in store
assert '.remove(id).commit()' in store and '.putString(id, label).commit()' in store
vm=(b/'presentation/favorites/FavoritesViewModel.kt').read_text()
assert 'repository.refreshChampionship' in vm and 'generations' in vm and 'jobs.remove(it)?.cancel()' in vm
assert 'driversViewModel(' not in vm and 'teamsViewModel(' not in vm
ui=(b/'presentation/favorites/favoritsScreen.kt').read_text()
assert 'favoriteSummaries(' in ui and 'SnackbarResult.ActionPerformed' in ui and 'vm.restore(' in ui
assert re.search(r'grandPrixRoute\(\s*result\.season\.toInt\(\),\s*result\.round', ui)
assert 'rememberSaveableStateHolder' in ui and 'rememberLazyListState' in ui
assert 'FavoritesViewModel(get(), get(), get())' in (b/'di/AppModule.kt').read_text()
repository=(b/'data/repository/RacingRepository.kt').read_text()
assert '$year/$kind/$id/results' in repository and '$kind/$id/seasons' in repository
assert 'years.take(2)' in repository and 'resultCheckedAt' in repository
print(f'Fase 5: {len(base)} recursos nos cinco idiomas, favoritos legados intactos, remoção/desfazer, rolagem e integração conferidos.')
