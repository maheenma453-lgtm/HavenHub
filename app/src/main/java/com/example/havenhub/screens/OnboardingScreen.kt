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
import androidx.navigation.NavController
import com.example.havenhub.navigation.Screen
import com.example.havenhub.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(
    val emoji       : String,
    val title       : String,
    val description : String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    navController : NavController
) {

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {

        Column(
            modifier            = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Row(
                modifier       = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
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

            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.weight(1f)
            ) { pageIndex ->
                OnboardingPageContent(page = pages[pageIndex])
            }

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

            Button(
                onClick = {
                    if (pagerState.currentPage < pages.size - 1) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
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

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier            = Modifier
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
            Text(
                text     = page.emoji,
                fontSize = 80.sp
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text       = page.title,
            fontSize   = 24.sp,
            fontWeight = FontWeight.Bold,
            color      = TextPrimary,
            textAlign  = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text       = page.description,
            fontSize   = 15.sp,
            color      = TextSecondary,
            textAlign  = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}