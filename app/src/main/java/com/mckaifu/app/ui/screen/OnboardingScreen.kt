@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.mckaifu.app.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mckaifu.app.ui.theme.*
import com.mckaifu.app.util.AppPrefs
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val gradient: Brush
)

val onboardingPages = listOf(
    OnboardingPage(
        Icons.Filled.Dns,
        "欢迎使用 McKaiFu",
        "一款强大的 Minecraft 服务器管理工具\n在手机上轻松开服、管理、监控",
        Brush.linearGradient(listOf(ZalithPrimary, Color(0xFF7B4FFF)))
    ),
    OnboardingPage(
        Icons.Filled.Extension,
        "多核心支持",
        "支持 PaperMC、Purpur、Pufferfish、Nukkit\n等主流 Java 版和基岩版服务端核心",
        Brush.linearGradient(listOf(ZalithSecondary, Color(0xFF0099CC)))
    ),
    OnboardingPage(
        Icons.Filled.Insights,
        "全方位监控",
        "实时查看 TPS、内存、CPU 使用情况\n管理玩家、世界、插件、配置文件",
        Brush.linearGradient(listOf(ZalithTertiary, Color(0xFFCC3366)))
    ),
    OnboardingPage(
        Icons.Filled.Wifi,
        "内网穿透",
        "集成 Playit.gg / Ngrok / 樱花frp 隧道\n提供国内节点，无需公网IP即可让好友加入",
        Brush.linearGradient(listOf(Color(0xFFFFA502), Color(0xFFCC7700)))
    ),
)

@Composable
fun OnboardingScreen(navController: NavController) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showSkip by remember { mutableStateOf(true) }

    val finish = {
        AppPrefs.setOnboardingDone(context)
        navController.navigate("servers") {
            popUpTo("onboarding") { inclusive = true }
            launchSingleTop = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ZalithBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(onboardingPages[page])
            }

            // Indicators
            Row(
                modifier = Modifier.padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(onboardingPages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 24.dp else 8.dp, 8.dp)
                            .background(
                                if (isSelected) ZalithPrimary else ZalithCardBorder,
                                MaterialTheme.shapes.extraSmall
                            )
                    )
                }
            }

            // Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { finish() }
                ) {
                    Text("跳过", color = TextSecondary)
                }

                if (pagerState.currentPage < onboardingPages.size - 1) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZalithPrimary
                        ),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("下一步")
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                    }
                } else {
                    Button(
                        onClick = { finish() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZalithPrimary
                        ),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("开始使用")
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        page.gradient,
                        MaterialTheme.shapes.extraLarge
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    page.icon,
                    null,
                    modifier = Modifier.size(56.dp),
                    tint = Color.White
                )
            }
            Spacer(Modifier.height(32.dp))
            Text(
                page.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = TextPrimary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                page.subtitle,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = TextSecondary,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
            )
        }
    }
}
