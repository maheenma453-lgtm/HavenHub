package com.example.havenhub.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.navigation.Screen
import com.example.havenhub.viewmodel.OnboardingViewModel

// ── Colors ────────────────────────────────────────────────────────────────────
private val OB_NavyDark    = Color(0xFF1A2B5E)
private val OB_NavyMid     = Color(0xFF2E4A8A)
private val OB_GoldPrimary = Color(0xFFC9A84C)
private val OB_GoldLight   = Color(0xFFE8C96A)
private val OB_White       = Color.White
private val OB_BgTop       = Color(0xFF3D5A99)
private val OB_BgBottom    = Color(0xFF1E3570)

private data class OBPage(
    val emoji    : String,
    val title    : String,
    val subtitle : String,
    val features : List<Pair<String, String>>,
    val color    : Color
)

@Composable
fun OnboardingScreen(
    navController : NavController,
    viewModel     : OnboardingViewModel = hiltViewModel()
) {
    OnboardingContent(navController = navController, viewModel = viewModel)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OnboardingContent(
    navController : NavController,
    viewModel     : OnboardingViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    val pages = listOf(
        OBPage(
            emoji    = "\uD83D\uDD0D",
            title    = "Find Your Perfect Stay",
            subtitle = "Search verified properties by city, type, price & duration across Pakistan",
            features = listOf(
                "\u2705" to "Verified listings with PT-1 documents",
                "\uD83D\uDD12" to "Secure payments — JazzCash & EasyPaisa",
                "\u26FA" to "Pre-book northern area vacation stays",
                "\u2B50" to "Reviews & ratings for trust & transparency"
            ),
            color = Color(0xFF2E7BC4)
        ),
        OBPage(
            emoji    = "\uD83D\uDCC5",
            title    = "Book Instantly",
            subtitle = "Choose daily, weekly or monthly packages and confirm in seconds",
            features = listOf(
                "\uD83C\uDFE0" to "Daily, weekly & monthly rental packages",
                "\uD83D\uDCCD" to "Properties in Lahore, Karachi, Islamabad & more",
                "\uD83D\uDCF2" to "Instant booking confirmation on your phone",
                "\uD83D\uDD04" to "Easy cancellation & rescheduling"
            ),
            color = Color(0xFF1A7A6E)
        ),
        OBPage(
            emoji    = "\uD83D\uDCB3",
            title    = "Easy & Secure Payments",
            subtitle = "Pay via JazzCash, EasyPaisa or Cash — every transaction tracked",
            features = listOf(
                "\uD83D\uDFE2" to "JazzCash mobile wallet — instant & secure",
                "\uD83D\uDFE0" to "EasyPaisa transfers in seconds",
                "\uD83D\uDCB5" to "Cash on arrival option available",
                "\uD83E\uDDFE" to "Full payment history & digital receipts"
            ),
            color = Color(0xFF9A6F28)
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
        if (uiState.isOnboardingComplete)
            navController.navigate(Screen.SignIn.route) {
                popUpTo(Screen.Onboarding.route) { inclusive = true }
            }
    }

    val floatAnim by rememberInfiniteTransition(label = "fl").animateFloat(
        initialValue  = -10f, targetValue = 10f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "float"
    )
    val pulseAnim by rememberInfiniteTransition(label = "pu").animateFloat(
        initialValue  = 0.88f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "pulse"
    )
    val shimmer by rememberInfiniteTransition(label = "sh").animateFloat(
        initialValue  = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label         = "shimmer"
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
        targetValue   = if (pageKey) 0f else 24f,
        animationSpec = tween(420, easing = EaseOutCubic), label = "psl"
    )

    val currentColor = pages[currentPage].color

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Full Navy Blue background ─────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(OB_BgTop, OB_BgBottom),
                        startY = 0f, endY = 900f
                    )
                )
        )

        // ── White card bottom 58% ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.60f)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp))
                .background(OB_White)
        )

        Column(modifier = Modifier.fillMaxSize()) {

            // ── TOP SECTION: Navy with emoji ──────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.40f)
            ) {
                // HavenHub label top-left
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 52.dp, start = 24.dp)
                ) {
                    Row {
                        Text(
                            text       = "HAVEN",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Black,
                            color      = OB_White
                        )
                        Text(
                            text       = "HUB",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Black,
                            color      = OB_GoldPrimary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(2.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(OB_GoldPrimary, OB_GoldLight)
                                )
                            )
                    )
                }

                // Skip button top-right
                TextButton(
                    onClick  = { viewModel.skipOnboarding() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 48.dp, end = 20.dp)
                ) {
                    Text(
                        text       = "Skip",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = OB_White.copy(alpha = 0.75f)
                    )
                }

                // Center: floating emoji in glass card
                Box(
                    modifier         = Modifier
                        .align(Alignment.Center)
                        .padding(top = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Pulse ring
                    Box(
                        modifier = Modifier
                            .size((150 * pulseAnim).dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        OB_GoldPrimary.copy(alpha = 0.12f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    // Glass card
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .offset(y = floatAnim.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(OB_White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Gold top stripe on card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .align(Alignment.TopCenter)
                                .clip(
                                    RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                                )
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(OB_GoldPrimary, OB_GoldLight, OB_GoldPrimary)
                                    )
                                )
                        )
                        Text(
                            text     = pages[currentPage].emoji,
                            fontSize = 58.sp
                        )
                    }
                }
            }

            // ── BOTTOM WHITE SECTION ──────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp))
                    .background(OB_White)
            ) {
                // Gold shimmer top stripe
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    OB_GoldPrimary.copy(alpha = 0.4f + 0.6f * shimmer),
                                    OB_GoldLight,
                                    OB_GoldPrimary.copy(alpha = 0.4f + 0.6f * (1f - shimmer)),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Pager content
                HorizontalPager(
                    state             = pagerState,
                    modifier          = Modifier.weight(1f),
                    userScrollEnabled = true
                ) { pageIndex ->
                    OBPageBody(
                        page        = pages[pageIndex],
                        alpha       = if (pageIndex == currentPage) pageAlpha else 1f,
                        slideOffset = if (pageIndex == currentPage) pageSlide else 0f,
                        accentColor = pages[pageIndex].color
                    )
                }

                // ── Dots + Buttons ────────────────────────────────────────────
                Column(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Page dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        modifier              = Modifier.padding(bottom = 20.dp)
                    ) {
                        repeat(uiState.totalPages) { index ->
                            val isSelected = currentPage == index
                            val dotWidth by animateDpAsState(
                                targetValue   = if (isSelected) 28.dp else 8.dp,
                                animationSpec = tween(300, easing = EaseOutCubic),
                                label         = "dw$index"
                            )
                            Box(
                                modifier = Modifier
                                    .width(dotWidth)
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected)
                                            Brush.horizontalGradient(
                                                listOf(OB_GoldPrimary, OB_GoldLight)
                                            )
                                        else
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color(0xFFD0D5E8),
                                                    Color(0xFFD0D5E8)
                                                )
                                            )
                                    )
                            )
                        }
                    }

                    // Next / Get Started button
                    Button(
                        onClick   = { viewModel.nextPage() },
                        modifier  = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = ButtonDefaults.buttonColors(
                            containerColor = if (currentPage == uiState.totalPages - 1)
                                OB_GoldPrimary else OB_NavyMid
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text          = if (currentPage < uiState.totalPages - 1)
                                "Next" else "Get Started",
                            fontSize      = 15.sp,
                            fontWeight    = FontWeight.ExtraBold,
                            color         = if (currentPage == uiState.totalPages - 1)
                                OB_NavyDark else OB_White,
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
                                text       = "Back",
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = OB_NavyDark.copy(alpha = 0.45f)
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
    slideOffset : Float,
    accentColor : Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .offset(y = slideOffset.dp)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Text(
            text       = page.title,
            fontSize   = 23.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = OB_NavyDark
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Gold underline accent
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(3.dp)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(OB_GoldPrimary, OB_GoldLight)
                    )
                )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text       = page.subtitle,
            fontSize   = 13.5.sp,
            color      = OB_NavyDark.copy(alpha = 0.55f),
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Feature rows
        page.features.forEach { (icon, text) ->
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                // Icon bubble
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = icon, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text       = text,
                    fontSize   = 13.sp,
                    color      = OB_NavyDark.copy(alpha = 0.80f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}