package com.angelsoft.macmirror.ui.whatsnew

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.angelsoft.macmirror.R
import com.angelsoft.macmirror.ui.theme.AppleAmber
import com.angelsoft.macmirror.ui.theme.AppleBlue
import com.angelsoft.macmirror.ui.theme.AppleGreen

data class WhatsNewFeatureItem(
    val id: String,
    val icon: ImageVector,
    val iconColor: Color,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descRes: Int
)

data class WhatsNewRelease(
    val version: String,
    val items: List<WhatsNewFeatureItem>
)

object WhatsNewCatalog {

    val releases = listOf(
        WhatsNewRelease(
            version = "1.2",
            items = listOf(
                WhatsNewFeatureItem(
                    id = "autostart",
                    icon = Icons.Default.PlayArrow,
                    iconColor = AppleAmber,
                    titleRes = R.string.whats_new_autostart_title,
                    descRes = R.string.whats_new_autostart_desc
                ),
                WhatsNewFeatureItem(
                    id = "low_latency",
                    icon = Icons.Default.Refresh,
                    iconColor = AppleBlue,
                    titleRes = R.string.whats_new_low_latency_title,
                    descRes = R.string.whats_new_low_latency_desc
                ),
                WhatsNewFeatureItem(
                    id = "e2ee",
                    icon = Icons.Default.Lock,
                    iconColor = AppleGreen,
                    titleRes = R.string.whats_new_e2ee_title,
                    descRes = R.string.whats_new_e2ee_desc
                )
            )
        )
    )

    fun hasHighlights(version: String): Boolean {
        return highlights(version) != null
    }

    fun highlights(version: String): WhatsNewRelease? {
        val clean = version.trim()
        return releases.firstOrNull { release ->
            clean == release.version ||
            clean.startsWith("${release.version}.") ||
            clean.startsWith("${release.version}-") ||
            release.version.startsWith("${clean}.")
        }
    }

    fun latestRelease(): WhatsNewRelease = releases.first()
}
