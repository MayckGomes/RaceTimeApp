"""Verificação estática da Fase 6. Não substitui compilação/testes Android."""
from pathlib import Path
import re,runpy,subprocess
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
runpy.run_path(str(root/'scripts/verify_phase5.py'),run_name='__main__')
b=root/'app/src/main/java/mayckgomes/com/racetimeapp'
base=None
for folder in ['values','values-pt-rBR','values-es','values-fr','values-it']:
    entries={n.attrib['name']:''.join(n.itertext()) for n in ET.parse(root/'app/src/main/res'/folder/'preferences_strings.xml').getroot()}
    if base is None:base=entries
    assert entries.keys()==base.keys(),folder
    for key in base:
        assert re.findall(r'%[0-9]+\$[sd]',entries[key])==re.findall(r'%[0-9]+\$[sd]',base[key]),(folder,key)
settings=(b/'presentation/settings/SettingsScreen.kt').read_text()
assert 'ReminderStore(' not in settings and 'HttpClient' not in settings
for name in ['fp1','fp2','fp3','sprintQualifying','sprint','qualifying','races']:
    assert re.search(rf'(settings|current)\.copy\(\s*{name}\s*=\s*it\s*\)', settings), name
assert 'FlowRow(' in settings and 'NotificationPreferenceItem(' in settings and 'Settings.ACTION_APP_LOCALE_SETTINGS' in settings
rules=(b/'domain/model/SessionReminders.kt').read_text()
assert 'listOf(0, 15, 30, 60)' in rules and 'val fp1: Boolean = practice' in rules and 'val sprintQualifying: Boolean = qualifying' in rules
assert 'existing.any' in rules and '.take(64)' in rules and 'event:${reminder.eventKey}' in rules
scheduler=(b/'notifications/ReminderScheduler.kt').read_text()
assert scheduler.index('store.saveScheduled(plan.active)') < scheduler.index('plan.schedule.forEach')
assert 'Clock' in scheduler and 'canScheduleExactly()' in scheduler and 'setAndAllowWhileIdle' in scheduler
assert '.setOnlyAlertOnce(true)' in scheduler and 'setData(Uri.parse("racetime://gp/$season/$round"))' in scheduler
store=(b/'notifications/ReminderStore.kt').read_text()
assert '"session_reminders"' in store and 'encodeDefaults = true' in store and '.clear(' not in store
worker=(b/'notifications/ReminderSyncWorker.kt').read_text()
assert 'runAttemptCount < 3' in worker and '12, TimeUnit.HOURS' in worker and 'cancelUniqueWork' in worker
assert 'repository.calendar' in worker and 'repository.result(' not in worker
main=(b/'MainActivity.kt').read_text()
assert 'onNewIntent' in main and 'onSaveInstanceState' in main and 'onReminderHandled' in main
nav=(b/'navgation/navgation.kt').read_text()
assert 'grandPrixRoute(target.season, target.round)' in nav and 'launchSingleTop = true' in nav
manifest=ET.parse(root/'app/src/main/AndroidManifest.xml').getroot()
ns='{http://schemas.android.com/apk/res/android}'
permissions={node.attrib[ns+'name'] for node in manifest.findall('uses-permission')}
assert permissions=={'android.permission.POST_NOTIFICATIONS','android.permission.RECEIVE_BOOT_COMPLETED',
'android.permission.SCHEDULE_EXACT_ALARM','android.permission.INTERNET','android.permission.ACCESS_NETWORK_STATE'}
for action in ['BOOT_COMPLETED','MY_PACKAGE_REPLACED','TIME_SET','TIMEZONE_CHANGED','LOCALE_CHANGED']:
    assert any(node.get(ns+'name')=='android.intent.action.'+action for node in manifest.iter('action')),action
assert manifest.find('application').get(ns+'localeConfig')=='@xml/locales_config'
locales={node.get(ns+'name') for node in ET.parse(root/'app/src/main/res/xml/locales_config.xml').getroot()}
assert locales=={'en','pt-BR','es','fr','it'}
# Identificadores de distribuição/assinatura e dependências não foram alterados nesta fase.
gradle=(root/'app/build.gradle.kts').read_text()
try:
    original=subprocess.check_output(['git','show','origin/main:app/build.gradle.kts'],cwd=root).decode()
    for key in ['namespace','applicationId','versionCode','versionName','signingConfig']:
        assert re.search(r'^\s*'+key+r'\s*=.*$',gradle,re.M)[0].strip()==re.search(r'^\s*'+key+r'\s*=.*$',original,re.M)[0].strip(),key
except Exception:
    for key, val in [('namespace','"mayckgomes.com.racetimeapp"'),('applicationId','"mayckgomes.com.racetimeapp"'),('versionCode','4'),('versionName','"1.0.3"')]:
        assert f'{key} = {val}' in gradle, key
print(f'Fase 6: {len(base)} recursos nos cinco idiomas, sete sessões, permissões, migração, entrega e navegação conferidos estaticamente.')
