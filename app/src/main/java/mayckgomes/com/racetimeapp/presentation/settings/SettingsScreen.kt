@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package mayckgomes.com.racetimeapp.presentation.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import mayckgomes.com.racetimeapp.R
import mayckgomes.com.racetimeapp.components.home.RacingCard
import mayckgomes.com.racetimeapp.components.home.SectionLabel
import mayckgomes.com.racetimeapp.data.local.Appearance
import mayckgomes.com.racetimeapp.domain.model.reminderLeadTimes
import mayckgomes.com.racetimeapp.notifications.sessionTitle
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(showHeading: Boolean = true) {
    val vm: SettingsViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val settings = state.settings
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var actionFailed by remember { mutableStateOf(false) }
    var syncRequested by remember { mutableStateOf(false) }
    fun openScreen(intent: Intent) {
        actionFailed = runCatching { context.startActivity(intent) }.isFailure
    }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) vm.save { it.copy(enabled = true) }
        }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { vm.observeWhileResumed() }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (showHeading)
            Text(
                stringResource(R.string.settings_tab),
                style = MaterialTheme.typography.displayLarge,
            )
        if (state.saveFailed)
            Text(
                stringResource(R.string.notify_save_failed),
                color = MaterialTheme.colorScheme.error,
            )
        if (actionFailed)
            Text(
                stringResource(R.string.notify_open_failed),
                color = MaterialTheme.colorScheme.error,
            )
        Text(
            stringResource(R.string.notifications_heading),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            stringResource(R.string.notifications_intro),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RacingCard {
            NotificationPreferenceItem(
                stringResource(R.string.notifications_enable),
                settings.enabled,
                true,
            ) { enabled ->
                if (
                    enabled &&
                        Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED
                )
                    permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else vm.save { it.copy(enabled = enabled) }
            }
            if (!state.allowed) {
                Text(
                    stringResource(R.string.notifications_permission_off),
                    style = MaterialTheme.typography.bodySmall,
                )
                SettingsActionButton(
                    label = stringResource(R.string.notifications_system_settings),
                    onClick = {
                        openScreen(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        )
                    },
                )
            }
        }
        RacingCard(accent = false) {
            SectionLabel(stringResource(R.string.notifications_sessions))
            val editable = settings.enabled
            NotificationPreferenceItem(stringResource(R.string.home_fp1), settings.fp1, editable) {
                vm.save { current -> current.copy(fp1 = it) }
            }
            NotificationPreferenceItem(stringResource(R.string.home_fp2), settings.fp2, editable) {
                vm.save { current -> current.copy(fp2 = it) }
            }
            NotificationPreferenceItem(stringResource(R.string.home_fp3), settings.fp3, editable) {
                vm.save { current -> current.copy(fp3 = it) }
            }
            NotificationPreferenceItem(
                stringResource(R.string.home_sprint_qualifying),
                settings.sprintQualifying,
                editable,
            ) {
                vm.save { current -> current.copy(sprintQualifying = it) }
            }
            NotificationPreferenceItem(
                stringResource(R.string.home_sprint),
                settings.sprint,
                editable,
            ) {
                vm.save { current -> current.copy(sprint = it) }
            }
            NotificationPreferenceItem(
                stringResource(R.string.home_qualifying),
                settings.qualifying,
                editable,
            ) {
                vm.save { current -> current.copy(qualifying = it) }
            }
            NotificationPreferenceItem(
                stringResource(R.string.home_race),
                settings.races,
                editable,
            ) {
                vm.save { current -> current.copy(races = it) }
            }
        }
        RacingCard(accent = false) {
            SectionLabel(stringResource(R.string.notifications_before))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                reminderLeadTimes.forEach { minutes ->
                    SettingsChoiceChip(
                        selected = settings.minutesBefore == minutes,
                        onClick = { vm.save { it.copy(minutesBefore = minutes) } },
                        enabled = settings.enabled,
                        label =
                            stringResource(
                                if (minutes == 0) R.string.notify_at_start
                                else R.string.notifications_minutes,
                                minutes,
                            ),
                    )
                }
            }
            Text(
                stringResource(
                    if (state.exact) R.string.notifications_exact
                    else R.string.notifications_approximate
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (Build.VERSION.SDK_INT >= 31 && !state.exact)
                SettingsActionButton(
                    label = stringResource(R.string.notifications_allow_exact),
                    primary = true,
                    enabled = settings.enabled && state.allowed,
                    onClick = {
                        openScreen(
                            Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:${context.packageName}"),
                            )
                        )
                    },
                )
        }
        RacingCard(accent = false) {
            SectionLabel(stringResource(R.string.notifications_status))
            when {
                !settings.enabled -> Text(stringResource(R.string.notifications_disabled))
                !state.allowed -> Text(stringResource(R.string.notifications_permission_off))
                state.upcoming.isEmpty() ->
                    Text(stringResource(R.string.notifications_no_reminders))
                else -> {
                    val next = state.upcoming.first()
                    Text(stringResource(R.string.notifications_scheduled, state.upcoming.size))
                    Text(next.raceName, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        stringResource(sessionTitle(next.label)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        localReminderTime(next.triggerMillis),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        stringResource(R.string.home_local_time),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (state.lastSync > 0)
                Text(
                    stringResource(
                        R.string.notifications_last_sync,
                        localReminderTime(state.lastSync),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            if (state.syncFailed)
                Text(
                    stringResource(R.string.notify_sync_failed),
                    color = MaterialTheme.colorScheme.error,
                )
            if (state.schedulingFailed)
                Text(
                    stringResource(R.string.notify_schedule_failed),
                    color = MaterialTheme.colorScheme.error,
                )
            SettingsActionButton(
                label = stringResource(R.string.notifications_sync),
                primary = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    vm.refreshCalendar()
                    syncRequested = true
                },
                enabled = settings.enabled,
            )
            if (syncRequested)
                Text(
                    stringResource(R.string.notify_sync_requested),
                    style = MaterialTheme.typography.bodySmall,
                )
            Text(
                stringResource(R.string.notifications_sync_info),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.notify_favorite_results),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        RacingCard(accent = false) {
            SectionLabel(stringResource(R.string.pref_appearance))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                        Appearance.SYSTEM to R.string.pref_system,
                        Appearance.DARK to R.string.pref_dark,
                        Appearance.LIGHT to R.string.pref_light,
                    )
                    .forEach { (value, title) ->
                        SettingsChoiceChip(
                            selected = state.appearance == value,
                            onClick = { vm.setAppearance(value) },
                            label = stringResource(title),
                        )
                    }
            }
        }
        RacingCard(accent = false) {
            SectionLabel(stringResource(R.string.pref_language))
            Text(
                context.resources.configuration.locales[0].getDisplayName(
                    context.resources.configuration.locales[0]
                )
            )
            Text(
                stringResource(
                    if (Build.VERSION.SDK_INT >= 33) R.string.pref_language_info
                    else R.string.pref_language_legacy
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            SettingsActionButton(
                onClick = {
                    openScreen(
                        if (Build.VERSION.SDK_INT >= 33)
                            Intent(
                                Settings.ACTION_APP_LOCALE_SETTINGS,
                                Uri.parse("package:${context.packageName}"),
                            )
                        else Intent(Settings.ACTION_LOCALE_SETTINGS)
                    )
                }
            ) {
                Text(
                    stringResource(
                        if (Build.VERSION.SDK_INT >= 33) R.string.pref_choose_language
                        else R.string.pref_device_language
                    )
                )
            }
        }
        RacingCard(accent = false) {
            SectionLabel(stringResource(R.string.v2_about))
            Text("RaceTime App", style = MaterialTheme.typography.headlineMedium)
            val version = remember {
                runCatching {
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName
                    }
                    .getOrNull()
                    .orEmpty()
            }
            if (version.isNotBlank())
                Text(
                    stringResource(R.string.pref_version, version),
                    style = MaterialTheme.typography.bodySmall,
                )
            Text(stringResource(R.string.v2_fan_app), style = MaterialTheme.typography.bodySmall)
            SettingsActionButton(
                onClick = {
                    openScreen(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/MayckGomes/RaceTimeApp"),
                        )
                    )
                }
            ) {
                Text(stringResource(R.string.pref_project))
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            SectionLabel(stringResource(R.string.pref_legal))
            Text(stringResource(R.string.v2_source), style = MaterialTheme.typography.bodySmall)
            Text(
                stringResource(R.string.pref_legal_info),
                style = MaterialTheme.typography.bodySmall,
            )
            SettingsActionButton(
                onClick = {
                    openScreen(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/jolpica/jolpica-f1/blob/main/TERMS.md"),
                        )
                    )
                }
            ) {
                Text(stringResource(R.string.v2_terms))
            }
            SettingsActionButton(
                onClick = {
                    openScreen(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/jolpica/jolpica-f1/blob/main/LICENSE"),
                        )
                    )
                }
            ) {
                Text(stringResource(R.string.pref_license))
            }
        }
    }
}

private fun localReminderTime(millis: Long): String =
    Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))

@Composable
fun NotificationPreferenceItem(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onChange,
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
