package com.example.myapplication.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.components.Event
import com.example.myapplication.components.EventItemUser
import com.example.myapplication.components.PopularEventCard
import com.example.myapplication.components.PremiumPopularBadge
import com.example.myapplication.viewmodels.EventViewModel
import com.example.myapplication.viewmodels.isEventInPast
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun UserHomePage(navController: NavHostController, viewModel: EventViewModel) {
    val auth = Firebase.auth
    val events by viewModel.events.collectAsState()
    val activeEvents = events.filter { event ->
        event.date != null && event.time != null &&
                !isEventInPast(event.date, event.time)
    }

    // Get popular events
    val popularEvents = remember(activeEvents) {
        activeEvents.filter { it.predictedPopular }
    }

    val context = LocalContext.current

    // Make Scaffold background transparent to show the gradient
    Scaffold(
        containerColor = Color.Transparent  // This is the key fix!
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp) // Reduced from 24.dp to 12.dp
        ) {
            // Popular Events Section
            if (popularEvents.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "🔥 Popular Events",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            "See all",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.clickable {
                                // Handle see all popular events
                            }
                        )
                    }
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(popularEvents) { event ->
                            val isEnrolled = event.let { viewModel.isUserEnrolled(it) }
                            PopularEventCard(
                                event = event,
                                isEnrolled = isEnrolled,
                                onCardClick = {
                                    navController.navigate("user_event_details/${event.title}")
                                },
                                onEnrollClick = {
                                    if (!isEnrolled) {
                                        viewModel.enrollToEvent(event.id, context)
                                    } else {
                                        viewModel.unenrollFromEvent(event.id, context)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Latest Events Section
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "📅 Latest Events",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        "See all",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.clickable {
                            // Handle see all latest events
                        }
                    )
                }
            }

            items(activeEvents.reversed()) { event ->
                val isEnrolled = event.let { viewModel.isUserEnrolled(it) }
                EventItemUser(
                    event = event,
                    onEnrollClick = {
                        if (!isEnrolled) {
                            viewModel.enrollToEvent(event.id, context)
                        } else {
                            viewModel.unenrollFromEvent(event.id, context)
                        }
                    },
                    onCardClick = {
                        navController.navigate("user_event_details/${event.title}")
                    },
                    isEnrolled = isEnrolled
                )
                // Removed the extra Spacer since we're using verticalArrangement.spacedBy
            }
        }
    }
}