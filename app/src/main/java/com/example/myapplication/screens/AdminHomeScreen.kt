package com.example.myapplication.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapplication.components.EventItem
import com.example.myapplication.components.Screen
import com.example.myapplication.viewmodels.EventViewModel
import com.example.myapplication.viewmodels.isEventInPast

@Composable
fun HomeScreen(
    viewModel: EventViewModel,
    navController: NavController,
    onEventClick: (String) -> Unit
) {
    val events by viewModel.events.collectAsState()
    val activeEvents = events.filter { event ->
        event.date != null && event.time != null &&
                !isEventInPast(event.date!!, event.time!!)
    }

    val context = LocalContext.current

    // Make Scaffold background transparent to show the gradient
    Scaffold(
        containerColor = Color.Transparent
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // All Events Section
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "📋 All Events",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        "Manage all",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.clickable {
                            // Handle manage all events
                        }
                    )
                }
            }

            // Check if we have events to display
            if (activeEvents.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "📅",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Spacer(modifier = Modifier.padding(8.dp))
                        Text(
                            text = "No Events Found",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Create your first event to get started",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(activeEvents.reversed()) { event ->
                    EventItem(
                        event = event,
                        navController = navController,
                        onCardClick = {
                            navController.navigate(Screen.EventDetails.createRoute(event.id))
                        },
                        onAttendanceClick = {
                            navController.navigate(Screen.Attendance.createRoute(event.id))
                        }
                    )
                }
            }
        }
    }
}