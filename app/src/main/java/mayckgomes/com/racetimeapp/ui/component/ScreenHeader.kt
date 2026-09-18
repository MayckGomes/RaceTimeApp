package mayckgomes.com.racetimeapp.ui.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default
) {
    Text(text = title, modifier = modifier, style = style)
}
