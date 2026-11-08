"""Verificação estática da revisão do perfil; não substitui Gradle/testes Android."""
from pathlib import Path
import re,runpy
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
runpy.run_path(str(root/'scripts/verify_phase7.py'),run_name='__main__')
b=root/'app/src/main/java/mayckgomes/com/racetimeapp'
base=None
for folder in ['values','values-pt-rBR','values-es','values-fr','values-it']:
    entries={n.attrib['name']:''.join(n.itertext()) for n in ET.parse(root/'app/src/main/res'/folder/'driver_strings.xml').getroot()}
    if base is None:base=entries
    assert entries.keys()==base.keys(),folder
    for key in base:
        assert re.findall(r'%[0-9]+\$[sd]',entries[key])==re.findall(r'%[0-9]+\$[sd]',base[key]),(folder,key)
header=(b/'components/home/DashboardCards.kt').read_text().split('fun sessionLabel')[0]
assert 'stringResource(R.string.app_name)' in header and 'Text("RACE TIME"' not in header
screen=(b/'presentation/driver/driverScreen.kt').read_text()
assert 'DriverDetailsViewModel' in screen and 'PullToRefreshBox' in screen and 'rememberSaveable(state.id)' in screen
assert 'teamRoute(' in screen and 'grandPrixRoute(' in screen and 'constructorColor(' in screen
assert 'HttpClient' not in screen and 'CoroutineScope(' not in screen
vm=(b/'presentation/driver/DriverDetailsViewModel.kt').read_text()
assert 'FavoritesStore' in vm and 'favorites.add(id, name, false)' in vm and 'ensureActive()' in vm
assert 'DriverDetailsViewModel(get(), get(), get())' in (b/'di/AppModule.kt').read_text()
assert not (b/'presentation/driver/driverViewmodel.kt').exists() or 'CancellationException' in (b/'presentation/driver/driverViewmodel.kt').read_text()
print(f'Perfil: nome do app centralizado, {len(base)} textos nos cinco idiomas, navegação, favoritos por ID e estado compartilhado conferidos estaticamente.')
