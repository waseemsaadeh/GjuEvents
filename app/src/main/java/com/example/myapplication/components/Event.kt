package com.example.myapplication.components

import com.google.firebase.firestore.IgnoreExtraProperties


@IgnoreExtraProperties
data class Event(
    val id: String = "",
    val title: String = "",
    val room: String = "",
    val date: String = "",  // Formatted date
    val time: String = "",  // Formatted time
    val description: String = "",
    val imageBase64: String? = null,
    val attendedStudents: List<String> = emptyList(),
    val enrolledStudents: List<String> = emptyList(),
    // Add these new fields
    val organizerId: String = "",
    val categories: List<String> = emptyList(),
    // This will be calculated client-side
    val predictedPopular: Boolean = false
)