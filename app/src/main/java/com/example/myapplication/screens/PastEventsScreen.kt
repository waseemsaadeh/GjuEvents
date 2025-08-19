package com.example.myapplication.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.viewmodels.EventViewModel
import com.example.myapplication.components.PastEventItemUser
import com.example.myapplication.viewmodels.isEventInPast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastEventsScreen(navController: NavHostController, viewModel: EventViewModel) {
    val allEvents by viewModel.events.collectAsState()
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()

    val finishedAttendedEvents = allEvents.filter { event ->
        event.date.isNotBlank() && event.time.isNotBlank() &&
                isEventInPast(event.date, event.time) &&
                event.attendedStudents.contains(currentUser?.email ?: "")
    }

    // Make Scaffold background transparent to show the gradient
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finished Events") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate("user_settings")
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back to Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            )
        },
        containerColor = Color.Transparent  // Match UserHomePage styling
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp) // Match UserHomePage spacing
        ) {
            // Header Section - similar to UserHomePage sections
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "📚 Finished Events",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        "${finishedAttendedEvents.size} events",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            if (finishedAttendedEvents.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No finished events found.",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            } else {
                items(finishedAttendedEvents) { event ->
                    val isEnrolled = viewModel.isUserEnrolled(event)
                    PastEventItemUser(
                        event = event,
                        onEnrollClick = {
                            val newEnrollmentState = !isEnrolled
                            if (newEnrollmentState) {
                                viewModel.enrollToEvent(event.title, context)
                            } else {
                                viewModel.unenrollFromEvent(event.title, context)
                            }
                        },
                        onCardClick = {
                            navController.navigate("user_event_details/${event.title}")
                        },
                        isEnrolled = isEnrolled
                    )
                    // No extra Spacer needed since we're using verticalArrangement.spacedBy
                }
            }
        }
    }
}