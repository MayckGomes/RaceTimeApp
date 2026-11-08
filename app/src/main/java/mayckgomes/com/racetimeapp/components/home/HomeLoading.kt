package mayckgomes.com.racetimeapp.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import mayckgomes.com.racetimeapp.R

enum class HomeSkeletonKind {
    WEEKEND,
    RESULT,
    STANDINGS,
}

@Composable
fun HomeCardSkeleton(kind: HomeSkeletonKind) {
    val loading = stringResource(R.string.home_loading_skeleton)
    RacingCard(accent = false) {
        Column(
            Modifier.fillMaxWidth()
                .testTag("home_skeleton_${kind.name.lowercase()}")
                .clearAndSetSemantics {
                    contentDescription = loading
                    progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                },
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SkeletonBlock(Modifier.fillMaxWidth(.42f).height(12.dp))
            SkeletonBlock(Modifier.fillMaxWidth(.78f).height(28.dp))
            if (kind != HomeSkeletonKind.STANDINGS)
                SkeletonBlock(Modifier.fillMaxWidth(.58f).height(18.dp))
            if (kind == HomeSkeletonKind.STANDINGS)
                repeat(3) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SkeletonBlock(Modifier.size(32.dp))
                        SkeletonBlock(Modifier.weight(1f).height(32.dp))
                        SkeletonBlock(Modifier.width(42.dp).height(32.dp))
                    }
                }
            else
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(if (kind == HomeSkeletonKind.WEEKEND) 4 else 3) {
                        SkeletonBlock(Modifier.weight(1f).height(48.dp))
                    }
                }
            SkeletonBlock(
                Modifier.fillMaxWidth(.38f).height(36.dp).align(androidx.compose.ui.Alignment.End)
            )
        }
    }
}

@Composable
private fun SkeletonBlock(modifier: Modifier) {
    Box(
        modifier.background(
            MaterialTheme.colorScheme.onSurface.copy(alpha = .09f),
            RoundedCornerShape(6.dp),
        )
    )
}
