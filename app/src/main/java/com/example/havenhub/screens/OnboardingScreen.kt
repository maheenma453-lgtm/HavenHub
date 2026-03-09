package com.example.havenhub.screens

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.OnboardingViewModel
import kotlinx.coroutines.launch

data class OnboardingPage(
    val emoji: String,
    val title: String,
    val description: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    navController: NavController,
    viewModel: OnboardingViewModel = hiltViewModel() // ✅ ViewModel Inject kiya
) {
    // ✅ ViewModel State observe karna
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    val pages = listOf(
        OnboardingPage(
            emoji = "🏘️",
            title = "Find Your Perfect Property",
            description = "Browse thousands of verified homes, apartments, and rooms across Pakistan. Filter by city, price, and type."
        ),
        OnboardingPage(
            emoji = "📅",
            title = "Book Instantly",
            description = "Book properties for daily, weekly, or monthly stays. Secure your vacation home in northern areas in advance."
        ),
        OnboardingPage(
            emoji = "💳",
            title = "Easy & Secure Payments",
            description = "Pay via JazzCash, EasyPaisa, or Cash. All transactions are tracked and secured within the app."
        )
    )

    // ✅ PagerState ko ViewModel ke currentPage se sync karna
    val pagerState = rememberPagerState(pageCount = { pages.size })

    // UI Reactivity: Jab ViewModel mein page badle, Pager bhi scroll ho
    LaunchedEffect(uiState.currentPage) {
        if (pagerState.currentPage != uiState.currentPage) {
            pagerState.animateScrollToPage(uiState.currentPage)
        }
    }

    // Navigation logic: Jab onboarding complete ho jaye
    LaunchedEffect(uiState.isOnboardingComplete) {
        if (uiState.isOnboardingComplete) {
            navController.navigate(Screen.RoleSelection.route) {
                popUpTo(Screen.Onboarding.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Skip Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { viewModel.skipOnboarding() }) {
                    Text(text = "Skip", color = TextSecondary)
                }
            }

            // Pager content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = true // Manual swipe support
            ) { pageIndex ->
                OnboardingPageContent(page = pages[pageIndex])
            }

            // Custom Page Indicator using uiState
            Row(
                modifier = Modifier.padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(uiState.totalPages) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .width(if (isSelected) 24.dp else 8.dp)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) PrimaryBlue else BorderGray)
                    )
                }
            }

            // Main Action Button
            Button(
                onClick = { viewModel.nextPage() }, // ✅ Logic Moved to ViewModel
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(
                    text = if (uiState.currentPage < uiState.totalPages - 1) "Next" else "Get Started",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BackgroundWhite
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(SurfaceVariantLight),
            contentAlignment = Alignment.Center
        ) {
            Text(text = page.emoji, fontSize = 80.sp)
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = page.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.description,
            fontSize = 15.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}