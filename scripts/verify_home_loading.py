"""Verificação estática de skeleton, ações e restauração local do dashboard."""
from pathlib import Path
import runpy
import re
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
runpy.run_path(str(root/'scripts/verify_driver_profile.py'),run_name='__main__')
b=root/'app/src/main/java/mayckgomes/com/racetimeapp'
for folder in ['values','values-pt-rBR','values-es','values-fr','values-it']:
    assert ET.parse(root/'app/src/main/res'/folder/'loading_strings.xml').find("string[@name='home_loading_skeleton']") is not None
home=(b/'presentation/home/HomeOverview.kt').read_text()
for kind in ['WEEKEND','RESULT','STANDINGS']: assert 'HomeSkeletonKind.'+kind in home
assert 'HomeFavoriteRow(' in home and 'TextButton(' not in home
vm=(b/'presentation/home/HomeViewModel.kt').read_text()
assert re.search(r'init\s*\{\s*refreshAll\(force\s*=\s*false\)', vm)
assert vm.index('repository.restoreHomeCache()') < vm.index('repository.refresh(force = force)')
repo=(b/'data/repository/RacingRepository.kt').read_text()
restore=repo.split('suspend fun restoreHomeCache')[1].split('suspend fun refresh(year:')[0]
assert 'gateway.query(' not in restore and 'gateway.cached(' in restore
assert re.search(r'current\.results\s*\+\s*savedResults\.filter', restore)
assert 'priorCalendar' in restore
assert 'parseResults(raw, season, round, type)' in repo
skeleton=(b/'components/home/HomeLoading.kt').read_text()
assert 'ProgressBarRangeInfo.Indeterminate' in skeleton and 'clearAndSetSemantics' in skeleton
buttons=(b/'components/home/RaceActionButton.kt').read_text()
assert '48.dp' in buttons and 'onPrimaryContainer' in buttons and 'onSurfaceVariant' in buttons
print('Home: skeleton acessível, botões padronizados, restauração sem HTTP e atualização manual/automática conferidos estaticamente.')
