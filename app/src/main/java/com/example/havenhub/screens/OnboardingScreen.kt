package com.example.havenhub.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.OnboardingViewModel

private data class OBFeatureItem(val icon: String, val text: String)

private data class OBPage(
    val illustrationEmoji : String,
    val title             : String,
    val subtitle          : String,
    val features          : List<OBFeatureItem>,
    val gradientColors    : List<Color>,
    val accentColor       : Color
)

// ✅ FIXED: Hilt ViewModel alag entry point se inject hoga
@Composable
fun OnboardingScreen(
    navController : NavController,
    viewModel     : OnboardingViewModel = hiltViewModel()
) {
    OnboardingContent(
        navController = navController,
        viewModel     = viewModel
    )
}

// ✅ Asli UI yahan hai — Preview crash nahi karega
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OnboardingContent(
    navController : NavController,
    viewModel     : OnboardingViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    val pages = listOf(
        OBPage(
            illustrationEmoji = "🔍",
            title    = "Find Your Perfect Stay",
            subtitle = "Search verified properties by city, type, price and duration across Pakistan",
            features = listOf(
                OBFeatureItem("✅", "Verified property listings with PT-1 documents"),
                OBFeatureItem("🔒", "Secure online payments — JazzCash & EasyPaisa"),
                OBFeatureItem("⛺", "Pre-booking for northern area vacation stays"),
                OBFeatureItem("⭐", "Reviews & ratings for trust & transparency")
            ),
            gradientColors = listOf(Color(0xFF0D7377), Color(0xFF14A085)),
            accentColor    = Color(0xFF0D7377)
        ),
        OBPage(
            illustrationEmoji = "📅",
            title    = "Book Instantly",
            subtitle = "Choose daily, weekly or monthly packages and confirm in seconds",
            features = listOf(
                OBFeatureItem("🏠", "Daily, weekly & monthly rental packages"),
                OBFeatureItem("📍", "Properties in Lahore, Karachi, Islamabad & more"),
                OBFeatureItem("📲", "Instant booking confirmation on your phone"),
                OBFeatureItem("🔄", "Easy cancellation & rescheduling")
            ),
            gradientColors = listOf(Color(0xFF1A3A6B), Color(0xFF1F4E8C)),
            accentColor    = Color(0xFF1A3A6B)
        ),
        OBPage(
            illustrationEmoji = "💳",
            title    = "Easy & Secure Payments",
            subtitle = "Pay via JazzCash, EasyPaisa or Cash — every transaction tracked",
            features = listOf(
                OBFeatureItem("💚", "JazzCash mobile wallet — instant & secure"),
                OBFeatureItem("🟠", "EasyPaisa transfers in seconds"),
                OBFeatureItem("💵", "Cash on arrival option available"),
                OBFeatureItem("🧾", "Full payment history & digital receipts")
            ),
            gradientColors = listOf(Color(0xFF9A7A30), Color(0xFFC9A84C)),
            accentColor    = Color(0xFF9A7A30)
        )
    )

    val pagerState  = rememberPagerState(pageCount = { pages.size })
    val currentPage = pagerState.currentPage

    LaunchedEffect(uiState.currentPage) {
        if (pagerState.currentPage != uiState.currentPage)
            pagerState.animateScrollToPage(uiState.currentPage)
    }
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != uiState.currentPage) {
            if (pagerState.currentPage > uiState.currentPage) viewModel.nextPage()
            else viewModel.previousPage()
        }
    }
    LaunchedEffect(uiState.isOnboardingComplete) {
        if (uiState.isOnboardingComplete) {
            navController.navigate(Screen.SignIn.route) {
                popUpTo(Screen.Onboarding.route) { inclusive = true }
            }
        }
    }

    val floatAnim by rememberInfiniteTransition(label = "fl").animateFloat(
        initialValue  = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "float"
    )
    val pulseAnim by rememberInfiniteTransition(label = "pu").animateFloat(
        initialValue  = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "pulse"
    )

    var pageKey by remember(currentPage) { mutableStateOf(false) }
    LaunchedEffect(currentPage) {
        pageKey = false
        kotlinx.coroutines.delay(60)
        pageKey = true
    }
    val pageAlpha by animateFloatAsState(
        targetValue   = if (pageKey) 1f else 0f,
        animationSpec = tween(400, easing = EaseOut), label = "pa"
    )
    val pageSlide by animateFloatAsState(
        targetValue   = if (pageKey) 0f else 22f,
        animationSpec = tween(420, easing = EaseOutCubic), label = "psl"
    )

    Box(modifier = Modifier.fillMaxSize()) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            pages[currentPage].gradientColors[0],
                            pages[currentPage].gradientColors[1]
                        ),
                        startY = 0f, endY = 900f
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.62f)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(BackgroundWhite)
        )

        Column(modifier = Modifier.fillMaxSize()) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.38f)
            ) {
                Text(
                    "HavenHub",
                    modifier   = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 54.dp, start = 20.dp),
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = BackgroundWhite.copy(alpha = 0.95f)
                )

                TextButton(
                    onClick  = { viewModel.skipOnboarding() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 50.dp, end = 18.dp)
                ) {
                    Text(
                        "Skip",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = BackgroundWhite.copy(alpha = 0.8f)
                    )
                }

                Box(
                    modifier         = Modifier.align(Alignment.Center).padding(top = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size((160 * pulseAnim).dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(BackgroundWhite.copy(alpha = 0.12f), Color.Transparent)
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .offset(y = floatAnim.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(BackgroundWhite.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text     = pages[currentPage].illustrationEmoji,
                            fontSize = 64.sp
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(BackgroundWhite)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth().height(3.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(AccentGoldDark, AccentGold, AccentGoldLight, AccentGold, AccentGoldDark)
                            )
                        )
                )

                HorizontalPager(
                    state             = pagerState,
                    modifier          = Modifier.weight(1f),
                    userScrollEnabled = true
                ) { pageIndex ->
                    OBPageBody(
                        page        = pages[pageIndex],
                        alpha       = if (pageIndex == currentPage) pageAlpha else 1f,
                        slideOffset = if (pageIndex == currentPage) pageSlide else 0f
                    )
                }

                Column(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp)
                        .padding(bottom = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        modifier              = Modifier.padding(bottom = 16.dp)
                    ) {
                        repeat(uiState.totalPages) { index ->
                            val isSelected = currentPage == index
                            val dotWidth by animateDpAsState(
                                targetValue   = if (isSelected) 24.dp else 7.dp,
                                animationSpec = tween(300, easing = EaseOutCubic),
                                label         = "dw$index"
                            )
                            Box(
                                modifier = Modifier
                                    .width(dotWidth).height(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) pages[currentPage].accentColor
                                        else BorderGray
                                    )
                            )
                        }
                    }

                    Button(
                        onClick   = { viewModel.nextPage() },
                        modifier  = Modifier.fillMaxWidth().height(54.dp),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = ButtonDefaults.buttonColors(
                            containerColor = pages[currentPage].accentColor
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Text(
                            text          = if (currentPage < uiState.totalPages - 1) "Next" else "Get Started",
                            fontSize      = 15.sp,
                            fontWeight    = FontWeight.ExtraBold,
                            color         = BackgroundWhite,
                            letterSpacing = 0.3.sp
                        )
                    }

                    if (currentPage > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick  = { viewModel.previousPage() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Back",
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OBPageBody(
    page        : OBPage,
    alpha       : Float,
    slideOffset : Float
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .offset(y = slideOffset.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text       = page.title,
            fontSize   = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = TextPrimary
        )
        Text(
            text       = page.subtitle,
            fontSize   = 14.sp,
            color      = TextSecondary,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        page.features.forEach { feature ->
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier              = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(page.accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = feature.icon, fontSize = 16.sp)
                }
                Text(
                    text     = feature.text,
                    fontSize = 13.sp,
                    color    = TextPrimary
                )
            }
        }
    }
}