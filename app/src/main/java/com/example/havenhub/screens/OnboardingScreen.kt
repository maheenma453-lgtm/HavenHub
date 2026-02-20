package com.example.havenhub.screens
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.havenhub.ui.navigation.Screen
import com.havenhub.ui.theme.*
import com.havenhub.ui.viewmodel.OnboardingViewModel
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────
// OnboardingScreen.kt
// PURPOSE : 3-page swipeable intro shown only on first app launch.
//           Each page highlights a key feature of HavenHub.
// NAVIGATION: OnboardingScreen → SignInScreen
// ─────────────────────────────────────────────────────────────────

// Data class representing each onboarding page content
data class OnboardingPage(
    val emoji       : String,
    val title       : String,
    val description : String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    navController : NavController,
    viewModel     : OnboardingViewModel = hiltViewModel()
) {

    // ── Onboarding Pages Content ──────────────────────────────────
    val pages = listOf(
        OnboardingPage(
            emoji       = "🏘️",
            title       = "Find Your Perfect Property",
            description = "Browse thousands of verified homes, apartments, and rooms across Pakistan. Filter by city, price, and type."
        ),
        OnboardingPage(
            emoji       = "📅",
            title       = "Book Instantly",
            description = "Book properties for daily, weekly, or monthly stays. Secure your vacation home in northern areas in advance."
        ),
        OnboardingPage(
            emoji       = "💳",
            title       = "Easy & Secure Payments",
            description = "Pay via JazzCash, EasyPaisa, or Cash. All transactions are tracked and secured within the app."
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope      = rememberCoroutineScope()

    // ── UI ────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {

        Column(
            modifier            = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Skip Button (top right) ───────────────────────────
            Row(
                modifier       = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        viewModel.completeOnboarding()  // Mark onboarding done
                        navController.navigate(Screen.SignIn.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                ) {
                    Text(
                        text  = "Skip",
                        color = TextSecondary
                    )
                }
            }

            // ── Horizontal Pager (swipeable pages) ───────────────
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.weight(1f)
            ) { pageIndex ->
                OnboardingPageContent(page = pages[pageIndex])
            }

            // ── Page Indicator Dots ───────────────────────────────
            Row(
                modifier              = Modifier.padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                pages.indices.forEach { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .width(if (isSelected) 24.dp else 8.dp)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) PrimaryBlue else BorderGray
                            )
                    )
                }
            }

            // ── Next / Get Started Button ─────────────────────────
            Button(
                onClick = {
                    if (pagerState.currentPage < pages.size - 1) {
                        // Go to next page
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        // Last page → navigate to Sign In
                        viewModel.completeOnboarding()
                        navController.navigate(Screen.SignIn.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(52.dp),
                shape  = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(
                    text       = if (pagerState.currentPage < pages.size - 1) "Next" else "Get Started",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = BackgroundWhite
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// OnboardingPageContent
// Reusable composable for individual onboarding page
// ─────────────────────────────────────────────────────────────────
@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // ── Illustration Area ─────────────────────────────────────
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(SurfaceGray),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text     = page.emoji,
                fontSize = 80.sp
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // ── Title ─────────────────────────────────────────────────
        Text(
            text       = page.title,
            fontSize   = 24.sp,
            fontWeight = FontWeight.Bold,
            color      = TextPrimary,
            textAlign  = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Description ───────────────────────────────────────────
        Text(
            text      = page.description,
            fontSize  = 15.sp,
            color     = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

