"""Verificações estáticas da Fase 2; não substituem build, JUnit ou testes no aparelho."""
from pathlib import Path
import re
import runpy
import xml.etree.ElementTree as ET
root = Path(__file__).resolve().parents[1]
runpy.run_path(str(root / 'scripts/verify_phase1.py'), run_name='__main__')
res = root / 'app/src/main/res'
folders = ['values', 'values-pt-rBR', 'values-es', 'values-fr', 'values-it']
strings = {}
for folder in folders:
    values = {}
    for file in (res / folder).glob('*.xml'):
        for node in ET.parse(file).getroot():
            if node.tag != 'string': continue
            key = node.attrib['name']
            assert key not in values, (folder, key, 'duplicate resource')
            values[key] = ''.join(node.itertext())
    strings[folder] = values
required = {k for k in strings['values'] if k.startswith(('v2_', 'home_', 'notifications_'))} | {'settings_tab'}
for folder in folders:
    assert required <= strings[folder].keys(), (folder, sorted(required - strings[folder].keys()))
    for key in required:
        args = lambda text: sorted(re.findall(r'%[0-9]+\$[sd]', text))
        assert args(strings[folder][key]) == args(strings['values'][key]), (folder, key, 'format arguments')
main = (root / 'app/src/main/java/mayckgomes/com/racetimeapp/presentation/main/mainScreen.kt').read_text()
titles = re.search(r'val titles\s*=\s*listOf\((.*?)\)', main, re.S).group(1)
assert re.findall(r'R.string.(\w+)', titles) == ['home', 'home_calendar', 'v2_championship', 'myfavorites']
assert 'navigate(settingsRoute)' in main
assert 'rememberSaveableStateHolder' in main
assert 'grandPrixRoute' in main
for file in ['driver/driverScreen.kt', 'team/teamScreen.kt']:
    text = (root / 'app/src/main/java/mayckgomes/com/racetimeapp/presentation' / file).read_text()
    assert 'popBackStack(main' not in text
print(f'Fase 2: {len(required)} recursos com traduções e argumentos verificados nos cinco idiomas.')
print('Quatro destinos, acesso a configurações, navegação ao GP e retorno de detalhes conferidos estaticamente.')
