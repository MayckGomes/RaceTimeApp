package mayckgomes.com.racetimeapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import mayckgomes.com.racetimeapp.navgation.Navgation
import mayckgomes.com.racetimeapp.notifications.ReminderTarget
import mayckgomes.com.racetimeapp.notifications.reminderTarget
import mayckgomes.com.racetimeapp.ui.theme.RaceTimeAppTheme

class MainActivity : ComponentActivity() {
    private var pendingReminder by mutableStateOf<ReminderTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingReminder =
            if (savedInstanceState == null) reminderTarget(intent)
            else {
                val season = savedInstanceState.getInt("reminder_pending_year")
                val round = savedInstanceState.getString("reminder_pending_round")
                if (season > 0 && round != null) ReminderTarget(season, round) else null
            }
        enableEdgeToEdge()
        setContent {
            RaceTimeAppTheme {
                Navgation(pendingReminder, onReminderHandled = { pendingReminder = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingReminder = reminderTarget(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        pendingReminder?.let {
            outState.putInt("reminder_pending_year", it.season)
            outState.putString("reminder_pending_round", it.round)
        }
        super.onSaveInstanceState(outState)
    }
}
