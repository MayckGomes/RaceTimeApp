"""Validação estática da Fase 3; não compila nem executa testes Android/JVM."""
from pathlib import Path
import re
import runpy
import xml.etree.ElementTree as ET
root = Path(__file__).resolve().parents[1]
runpy.run_path(str(root / 'scripts/verify_phase2.py'), run_name='__main__')
res = root / 'app/src/main/res'
base = None
for folder in ['values', 'values-pt-rBR', 'values-es', 'values-fr', 'values-it']:
    values = {n.attrib['name']: ''.join(n.itertext()) for n in ET.parse(res / folder / 'championship_strings.xml').getroot()}
    if base is None: base = values
    assert values.keys() == base.keys(), folder
    for key in base:
        assert re.findall(r'%[0-9]+\$[sd]', values[key]) == re.findall(r'%[0-9]+\$[sd]', base[key]), (folder, key)
source = root / 'app/src/main/java/mayckgomes/com/racetimeapp'
main = (source / 'presentation/main/mainScreen.kt').read_text()
assert 'ChampionshipScreen(' in main and 'DriversScreen(' not in main and 'TeamsScreen(' not in main
screen = (source / 'presentation/championship/ChampionshipScreen.kt').read_text()
assert 'rememberLazyListState' in screen and 'rememberSaveableStateHolder' in screen
assert 'confirmedMovement(row, previous, info)' in screen
assert 'constructorstandings' not in screen and 'client.get' not in screen
repo = (source / 'data/repository/RacingRepository.kt').read_text()
assert 'currentProof' in repo and 'previousProof' in repo and 'SessionType.RACE' in repo
assert 'driversInfo' in repo and 'constructorsInfo' in repo
ads = (source / 'components/ads/bannerAds.kt').read_text()
assert 'onRelease = { ad -> ad.destroy() }' in ads
print(f'Fase 3: {len(base)} recursos traduzidos, integração, rolagem, regras de comparação e descarte do banner conferidos.')
