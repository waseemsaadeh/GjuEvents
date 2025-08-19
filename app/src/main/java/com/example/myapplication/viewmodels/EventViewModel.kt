package com.example.myapplication.viewmodels

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.components.Enrollment
import com.example.myapplication.components.Event
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import android.provider.Settings
import com.example.myapplication.components.EventReminderReceiver
import java.util.Calendar

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val isAdmin: Boolean = false
)

class EventViewModel() : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()
    private val _enrollments = MutableStateFlow<List<Enrollment>>(emptyList())
    val enrollments: StateFlow<List<Enrollment>> = _enrollments.asStateFlow()

    fun getEventById(eventId: String): Event? {
        return _events.value.find { it.id == eventId }
    }

    fun getEventByTitle(title: String): Event? {
        return _events.value.find { it.title == title }
    }

    fun updateEvent(
        event: Event,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            try {
                db.collection("events")
                    .document(event.id)
                    .set(event)
                    .await()
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    private val _enrolledEvents = MutableStateFlow<List<Event>>(emptyList())
    val enrolledEvents: StateFlow<List<Event>> = _enrolledEvents.asStateFlow()

    private val _currentUser = MutableStateFlow(
        User(
            email = FirebaseAuth.getInstance().currentUser?.email?:"" ,
            isAdmin = false
        )
    )

    val currentUser: StateFlow<User> = _currentUser.asStateFlow()

    private val auth = FirebaseAuth.getInstance()

    init {
        fetchEventsRealTime() // Start listening for data changes
    }

    private fun fetchEventsRealTime() {
        db.collection("events")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val eventsList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Event::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                // Add popularity predictions
                val eventsWithPredictions = eventsList.map { event ->
                    event.copy(predictedPopular = predictPopularity(event, eventsList))
                }

                _events.value = eventsWithPredictions
                loadEnrolledEventsFromLocalEvents()
            }
    }

    private fun predictPopularity(event: Event, allEvents: List<Event>): Boolean {
        // 1. Category popularity (with default for new categories)
        val categoryScore = if (event.categories.isNotEmpty()) {
            event.categories.map { category ->
                val categoryEvents = allEvents.filter {
                    it.categories.contains(category) &&
                            it.id != event.id
                }

                if (categoryEvents.isEmpty()) {
                    // Default score for new categories
                    5.0
                } else {
                    categoryEvents.map { it.enrolledStudents.size }
                        .average()
                        .takeIf { !it.isNaN() } ?: 0.0
                }
            }.average()
        } else 3.0 // Default for uncategorized events

        // 2. Organizer popularity (with default for new organizers)
        val organizerScore = if (event.organizerId.isNotEmpty()) {
            val organizerEvents = allEvents.filter {
                it.organizerId == event.organizerId &&
                        it.id != event.id
            }

            if (organizerEvents.isEmpty()) {
                // Default score for new organizers
                5.0
            } else {
                organizerEvents.map { it.enrolledStudents.size }
                    .average()
                    .takeIf { !it.isNaN() } ?: 0.0
            }
        } else 3.0 // Default for unknown organizers

        // 3. Time popularity (prime time bonus)
        val timeScore = try {
            when (event.time.take(2).toInt()) {
                in 17..20 -> 1.5  // Evening events get boost
                in 12..14 -> 1.2  // Lunchtime boost
                else -> 1.0
            }
        } catch (e: Exception) { 1.0 }

        // 4. Day of week popularity
        val dayScore = try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = dateFormat.parse(event.date)
            val calendar = Calendar.getInstance().apply { time = date }
            when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.FRIDAY, Calendar.SATURDAY -> 1.3
                Calendar.SUNDAY -> 1.2
                else -> 1.0
            }
        } catch (e: Exception) { 1.0 }

        // Add debug logging
        Log.d("Popularity", "Event: ${event.title}")
        Log.d("Popularity", "CategoryScore: $categoryScore")
        Log.d("Popularity", "OrganizerScore: $organizerScore")
        Log.d("Popularity", "TimeScore: $timeScore")
        Log.d("Popularity", "DayScore: $dayScore")

        // Weighted scoring
        val totalScore = (categoryScore * 0.4) + (organizerScore * 0.3) +
                (timeScore * 0.2) + (dayScore * 0.1)

        Log.d("Popularity", "TotalScore: $totalScore")

        // Lower threshold for better detection
        val isPopular = totalScore > 1.5
        Log.d("Popularity", "Popular: $isPopular")

        return isPopular
    }

    fun addEvent(
        event: Event,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Use the event's ID as the Firestore document ID
                db.collection("events")
                    .document(event.id)
                    .set(event)
                    .await()
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun scheduleEventReminderWithCheck(context: Context, event: Event) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                context.startActivity(intent)
                return
            }
        }

        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val eventTimeMillis = formatter.parse("${event.date} ${event.time}")?.time ?: return
        val reminderTime = eventTimeMillis - 60 * 60 * 1000

        if (reminderTime <= System.currentTimeMillis()) return

        val intent = Intent(context, EventReminderReceiver::class.java).apply {
            putExtra("title", "Upcoming Event")
            putExtra("message", "Your event \"${event.title}\" starts in 1 hour!")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.title.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun cancelEventReminder(context: Context, event: Event) {
        val intent = Intent(context, EventReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.title.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    fun enrollToEvent(eventId: String,context: Context) {
        viewModelScope.launch {
            val userEmail = currentUser.value.email
            val eventRef = db.collection("events").document(eventId)

            eventRef.update("enrolledStudents", FieldValue.arrayUnion(userEmail))
                .addOnSuccessListener {
                    val event = getEventById(eventId)
                    if (event != null) {
                        scheduleEventReminderWithCheck(context, event)
                    }
                    Log.d("Enroll", "Successfully enrolled $userEmail in event $eventId")
                }
                .addOnFailureListener { e ->
                    Log.e("Enroll", "Failed to enroll $userEmail in event $eventId", e)
                }
        }
    }

    fun isUserEnrolled(event: Event): Boolean {
        return event.enrolledStudents.contains(currentUser.value.email)
    }

    private fun loadEnrolledEventsFromLocalEvents() {
        val userEmail = currentUser.value.email
        _enrolledEvents.value = _events.value.filter { event ->
            event.enrolledStudents.contains(userEmail)
        }
    }

    fun deleteEvent(
        eventId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        // Get reference to Firestore collection
        val db = FirebaseFirestore.getInstance()
        db.collection("events").document(eventId)
            .delete()
            .addOnSuccessListener {
                // Remove from local list if needed
                _events.value = _events.value.filter { it.id != eventId }
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    fun logout() {
        viewModelScope.launch {
            // Add actual logout logic here
            _currentUser.value = User() // Reset user
        }
    }

    fun unenrollFromEvent(eventId: String,context: Context,onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val userEmail = currentUser.value.email
            val eventRef = db.collection("events").document(eventId)

            eventRef.update("enrolledStudents", FieldValue.arrayRemove(userEmail))
                .addOnSuccessListener {
                    val event = getEventById(eventId)
                    if (event != null) {
                        scheduleEventReminderWithCheck(context, event)
                    }
                    Log.d("Unenroll", "Successfully unenrolled $userEmail from event $eventId")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.e("Unenroll", "Failed to unenroll $userEmail from event $eventId", e)
                }
        }
    }
}

fun isEventInPast(dateString: String, timeString: String): Boolean {
    return try {
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val fullDateTime = formatter.parse("$dateString $timeString")
        fullDateTime?.before(Date()) ?: false
    } catch (e: Exception) {
        false
    }
}