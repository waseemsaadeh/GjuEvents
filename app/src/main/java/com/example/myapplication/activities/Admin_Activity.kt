package com.example.myapplication.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.components.BottomNavigationBar
import com.example.myapplication.components.NavigationHost
import com.example.myapplication.viewmodels.EventViewModel
import com.example.myapplication.viewmodels.UserViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

class Admin_Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // Apply gradient background to entire app
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFF8FAFC), // Light gray-blue
                                    Color(0xFFE0E7FF), // Light indigo
                                    Color(0xFFDDD6FE), // Light purple
                                    Color(0xFFF3E8FF)  // Very light purple
                                ),
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY
                            )
                        )
                ) {
                    AdminMainScreen()
                }
            }
        }
    }
}

@Composable
fun AdminMainScreen() {
    val navController = rememberNavController()
    val eventViewModel: EventViewModel = viewModel()
    val userViewModel: UserViewModel = viewModel()

    Scaffold(
        bottomBar = { BottomNavigationBar(navController = navController) },
        containerColor = Color.Transparent // Make scaffold background transparent
    ) { innerPadding ->
        NavigationHost(
            navController = navController,
            eventViewModel = eventViewModel,
            userViewModel = userViewModel
        )
    }
}