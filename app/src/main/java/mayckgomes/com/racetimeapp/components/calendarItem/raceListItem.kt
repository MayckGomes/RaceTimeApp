package mayckgomes.com.racetimeapp.components.calendarItem

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mayckgomes.com.racetimeapp.domain.models.RacesCalendar
import mayckgomes.com.racetimeapp.ui.theme.RaceTimeAppTheme
import java.text.SimpleDateFormat
import java.time.LocalDate

@SuppressLint("SimpleDateFormat")
@Composable
fun RaceItem(race: RacesCalendar, actualWeekend: String) {


    RaceTimeAppTheme {

        val borderColor = if (actualWeekend == race.date) MaterialTheme.colorScheme.primary else Color.Transparent

        Surface(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(2.dp, color = borderColor)
        ) {

            Column(
                modifier = Modifier
                    .padding(10.dp)
            ) {

                Text(
                    text = "Round ${race.round}",
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.size(10.dp))

                Text(
                    text = race.raceName,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.size(10.dp))

                val format = race.date.split("-")

                Text(
                    text = "${format[2]}/${format[1]}/${format[0]}",
                    color = MaterialTheme.colorScheme.onBackground
                )

            }

        }

    }

}