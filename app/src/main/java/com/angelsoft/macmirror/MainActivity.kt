package com.angelsoft.macmirror

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angelsoft.macmirror.data.PreferencesManager
import com.angelsoft.macmirror.ui.screens.AppsScreen
import com.angelsoft.macmirror.ui.screens.MainScreen
import com.angelsoft.macmirror.ui.screens.OnboardingScreen
import com.angelsoft.macmirror.ui.screens.SettingsScreen
import com.angelsoft.macmirror.ui.theme.AppleBlue
import com.angelsoft.macmirror.ui.theme.MacMirrorTheme
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    
    private val preferencesManager: PreferencesManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.angelsoft.macmirror.util.PermissionUtils.rebindNotificationListener(this)
        setContent {
            val themeMode by preferencesManager.themeModeFlow.collectAsState(initial = 0)
            val hasCompletedOnboarding by preferencesManager.hasCompletedOnboardingFlow.collectAsState(initial = true)
            var showOnboardingManual by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            MacMirrorTheme(themeMode = themeMode) {
                if (!hasCompletedOnboarding || showOnboardingManual) {
                    OnboardingScreen(
                        onFinish = {
                            scope.launch {
                                preferencesManager.setHasCompletedOnboarding(true)
                            }
                            showOnboardingManual = false
                        }
                    )
                } else {
                    var selectedTab by remember { mutableIntStateOf(0) }

                    val bottomBarContent: @Composable () -> Unit = {
                        IosTabBar(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )
                    }

                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally(animationSpec = tween(280)) { width -> (width * 0.35).toInt() } + fadeIn(animationSpec = tween(280)))
                                    .togetherWith(slideOutHorizontally(animationSpec = tween(280)) { width -> (-width * 0.35).toInt() } + fadeOut(animationSpec = tween(280)))
                            } else {
                                (slideInHorizontally(animationSpec = tween(280)) { width -> (-width * 0.35).toInt() } + fadeIn(animationSpec = tween(280)))
                                    .togetherWith(slideOutHorizontally(animationSpec = tween(280)) { width -> (width * 0.35).toInt() } + fadeOut(animationSpec = tween(280)))
                            }
                        },
                        label = "ScreenTransition"
                    ) { targetTab ->
                        when (targetTab) {
                            0 -> MainScreen(
                                modifier = Modifier.fillMaxSize(),
                                bottomBar = bottomBarContent
                            )
                            1 -> AppsScreen(
                                modifier = Modifier.fillMaxSize(),
                                bottomBar = bottomBarContent
                            )
                            else -> SettingsScreen(
                                modifier = Modifier.fillMaxSize(),
                                bottomBar = bottomBarContent,
                                onShowOnboarding = { showOnboardingManual = true }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IosTabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
    ) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(54.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IosTabBarItem(
                title = stringResource(R.string.tab_connection),
                icon = Icons.Default.Notifications,
                isSelected = selectedTab == 0,
                onClick = {
                    if (selectedTab != 0) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTabSelected(0)
                    }
                }
            )

            IosTabBarItem(
                title = stringResource(R.string.tab_apps),
                icon = Icons.Default.Menu,
                isSelected = selectedTab == 1,
                onClick = {
                    if (selectedTab != 1) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTabSelected(1)
                    }
                }
            )

            IosTabBarItem(
                title = stringResource(R.string.tab_settings),
                icon = Icons.Default.Settings,
                isSelected = selectedTab == 2,
                onClick = {
                    if (selectedTab != 2) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTabSelected(2)
                    }
                }
            )
        }
    }
}

@Composable
fun RowScope.IosTabBarItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tintColor = if (isSelected) AppleBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = tintColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = tintColor,
            fontSize = 11.sp
        )
    }
}