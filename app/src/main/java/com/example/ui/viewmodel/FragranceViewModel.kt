package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.FragranceInfo
import com.example.data.api.GeminiClient
import com.example.data.api.GeminiRequest
import com.example.data.api.Content
import com.example.data.api.Part
import com.example.data.api.GenerationConfig
import com.example.data.database.AppDatabase
import com.example.data.database.Bottle
import com.example.data.database.FragranceRepository
import com.example.data.database.LogEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Combined representation of a Bottle with its log history
data class BottleWithLogs(
    val bottle: Bottle,
    val logs: List<LogEntity>
) {
    val totalSprays: Int = bottle.totalSprays()
    val spraysUsed: Int = logs.sumOf { it.sprays }
    val spraysRemaining: Int = (totalSprays - spraysUsed).coerceAtLeast(0)
    val mlRemaining: Double = spraysRemaining * bottle.mlPerSpray
    val percentRemaining: Double = if (totalSprays > 0) {
        (spraysRemaining.toDouble() / totalSprays.toDouble() * 100.0).coerceIn(0.0, 100.0)
    } else 0.0
    val costPerSpray: Double = if (totalSprays > 0) bottle.price / totalSprays else 0.0
    val lastUsedDate: String? = logs.maxByOrNull { it.date }?.date
    val sessionsCount: Int = logs.size
}

sealed interface AutoFillState {
    object Idle : AutoFillState
    object Loading : AutoFillState
    data class Success(val info: FragranceInfo) : AutoFillState
    data class Error(val message: String) : AutoFillState
}

class FragranceViewModel(application: Application) : AndroidViewModel(application) {
    private val database = RoomDbHelper.getDatabase(application)
    private val repository = FragranceRepository(database.fragranceDao())

    // Auto-fill state
    val autoFillState = MutableStateFlow<AutoFillState>(AutoFillState.Idle)

    // Live state flows of data
    val bottlesList: StateFlow<List<Bottle>> = repository.allBottles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val logsList: StateFlow<List<LogEntity>> = repository.allLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Toast flow
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    // Combined items details
    val collectionDetails: StateFlow<List<BottleWithLogs>> = combine(bottlesList, logsList) { bottles, logs ->
        bottles.map { bottle ->
            BottleWithLogs(
                bottle = bottle,
                logs = logs.filter { it.bottleId == bottle.id }
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Current screen navigation
    val currentTab = MutableStateFlow("collection") // "collection" or "analytics"

    // Action modals toggles
    val showAddBottleModal = MutableStateFlow(false)
    val showLogSprayModal = MutableStateFlow<Bottle?>(null)

    // Toast show helper
    fun showToast(message: String) {
        viewModelScope.launch {
            _toastMessage.emit(message)
        }
    }

    // Insert Bottle
    fun saveBottle(
        house: String,
        name: String,
        concentration: String,
        sizeMl: Int,
        price: Double,
        currency: String,
        purchaseDate: String,
        mlPerSpray: Double,
        imageUrl: String?,
        topNotes: String,
        middleNotes: String,
        baseNotes: String,
        family: String,
        year: Int?,
        description: String,
        perfumer: String?,
        personalNotes: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val bottle = Bottle(
                house = house.trim(),
                name = name.trim(),
                concentration = concentration.trim(),
                sizeMl = sizeMl,
                price = price,
                currency = currency,
                purchaseDate = purchaseDate,
                mlPerSpray = mlPerSpray,
                imageUrl = if (imageUrl.isNullOrBlank()) null else imageUrl.trim(),
                topNotes = topNotes.trim(),
                middleNotes = middleNotes.trim(),
                baseNotes = baseNotes.trim(),
                family = family.trim(),
                year = year,
                description = description.trim(),
                perfumer = if (perfumer.isNullOrBlank()) null else perfumer.trim(),
                personalNotes = personalNotes.trim()
            )
            repository.insertBottle(bottle)
            showToast("Added fragrance: $name")
            showAddBottleModal.value = false
        }
    }

    // Delete Bottle
    fun deleteBottle(bottleId: Int, bottleName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBottle(bottleId)
            showToast("Removed fragrance: $bottleName")
        }
    }

    // Update Bottle
    fun updateBottle(bottle: Bottle) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertBottle(bottle)
            showToast("Saved changes: ${bottle.name}")
        }
    }

    // Add usage log
    fun logSprays(bottleId: Int, sprays: Int, date: String, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val logEntity = LogEntity(
                bottleId = bottleId,
                date = date,
                sprays = sprays,
                notes = notes.trim()
            )
            repository.insertLog(logEntity)
            showToast("Logged $sprays sprays used")
            showLogSprayModal.value = null
        }
    }

    // Perform Auto-fill via Gemini
    fun fetchFragranceInfo(house: String, name: String) {
        if (house.isBlank() || name.isBlank()) {
            autoFillState.value = AutoFillState.Error("Please enter both house and fragrance name first.")
            return
        }

        autoFillState.value = AutoFillState.Loading
        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "MY_NEW_API_KEY_DEFAULT_VALUE") {
                    throw IllegalStateException("API Key is not configured. Please add your GEMINI_API_KEY in the Secrets panel in Google AI Studio to enable Auto-fill.")
                }

                val prompt = """
                    Search the web or retrieve information for the fragrance "${name}" by "${house}". 
                    Return ONLY a valid JSON object matching this exact schema:
                    {
                      "topNotes": "Note 1, Note 2, Note 3",
                      "middleNotes": "Note 1, Note 2",
                      "baseNotes": "Note 1, Note 2",
                      "family": "Aromatic Woody",
                      "year": 2015,
                      "concentration": "EDP",
                      "description": "A refined woody fragrance with prominent initial spice notes settling into a rich cedar and amber base.",
                      "perfumer": "Jean-Claude Ellena"
                    }
                    Fields explanation:
                    - concentration must be one of: "EDT", "EDP", "Parfum", "EDC", "Cologne", "Other"
                    - year should be an integer, e.g. 2015, or null if unknown
                    - description must be 2-3 elegant sentences.
                    Do not wrap inside markdown blocks (such as ```json) or backticks. Return raw JSON string only.
                """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    generationConfig = GenerationConfig(
                        responseMimeType = "application/json",
                        temperature = 0.2f
                    )
                )

                val response = withContext(Dispatchers.IO) {
                    GeminiClient.service.generateContent(apiKey, request)
                }

                val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (jsonText.isNullOrBlank()) {
                    throw Exception("No content returned from Gemini.")
                }

                // Extract or clean JSON block
                val cleanedJson = cleanJsonString(jsonText)
                
                val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(FragranceInfo::class.java)
                val info = adapter.fromJson(cleanedJson)

                if (info != null) {
                    autoFillState.value = AutoFillState.Success(info)
                    showToast("Found details for $name!")
                } else {
                    throw Exception("Failed to parse JSON content.")
                }
            } catch (e: Exception) {
                Log.e("FragranceVM", "Gemini error", e)
                autoFillState.value = AutoFillState.Error(e.message ?: "Unknown error occurred.")
            }
        }
    }

    private fun cleanJsonString(input: String): String {
        var str = input.trim()
        if (str.startsWith("```json")) {
            str = str.substringAfter("```json")
        } else if (str.startsWith("```")) {
            str = str.substringAfter("```")
        }
        if (str.endsWith("```")) {
            str = str.substringBeforeLast("```")
        }
        return str.trim()
    }

    fun resetAutoFillState() {
        autoFillState.value = AutoFillState.Idle
    }
}

// Room DB helper
object RoomDbHelper {
    private var instance: AppDatabase? = null

    fun getDatabase(context: android.content.Context): AppDatabase {
        return instance ?: synchronized(this) {
            val db = androidx.room.Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "atelier_fragrance_db"
            ).fallbackToDestructiveMigration().build()
            instance = db
            db
        }
    }
}
