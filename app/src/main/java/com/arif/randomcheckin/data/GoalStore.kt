package com.arif.randomcheckin.data.model

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


private val Context.dataStore by preferencesDataStore(name = "goal_store")

class GoalStore(private val context: Context) {

    // Goal fields
    private val TITLE_KEY = stringPreferencesKey("goal_title")
    private val DESC_KEY = stringPreferencesKey("goal_description")
    private val END_DATE_KEY = stringPreferencesKey("goal_end_date")

    // Active window (minutes in day: 0..1439)
    private val START_MIN_KEY = intPreferencesKey("active_start_min")
    private val END_MIN_KEY = intPreferencesKey("active_end_min")

    private val LAST_NOTE_DAY_KEY = stringPreferencesKey("last_note_day") // yyyy-MM-dd
    private val LAST_NOTE_KEY = stringPreferencesKey("last_note")


    fun goalFlow(): Flow<Triple<String, String, String>?> {
        return context.dataStore.data.map { prefs ->
            val title = prefs[TITLE_KEY]
            val desc = prefs[DESC_KEY]
            val endDate = prefs[END_DATE_KEY]

            if (title != null && desc != null && endDate != null) {
                Triple(title, desc, endDate)
            } else {
                null
            }
        }
    }

    fun activeWindowFlow(): Flow<Pair<Int, Int>> {
        return context.dataStore.data.map { prefs ->
            val startMin = prefs[START_MIN_KEY] ?: (9 * 60)   // default 09:00
            val endMin = prefs[END_MIN_KEY] ?: (21 * 60)      // default 21:00
            Pair(startMin, endMin)
        }
    }

    suspend fun saveGoal(
        title: String,
        description: String,
        endDate: String,
        startMin: Int = 9 * 60,
        endMin: Int = 21 * 60
    ) {
        val safeStart = startMin.coerceIn(0, 1439)
        val safeEnd = endMin.coerceIn(0, 1439)

        context.dataStore.edit { prefs ->
            prefs[TITLE_KEY] = title
            prefs[DESC_KEY] = description
            prefs[END_DATE_KEY] = endDate

            prefs[START_MIN_KEY] = safeStart
            prefs[END_MIN_KEY] = safeEnd
        }
    }

    suspend fun clearGoal() {
        context.dataStore.edit { prefs ->
            prefs.remove(TITLE_KEY)
            prefs.remove(DESC_KEY)
            prefs.remove(END_DATE_KEY)
        }
    }
    suspend fun saveDailyNote(note: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        context.dataStore.edit { prefs ->
            prefs[LAST_NOTE_DAY_KEY] = today
            prefs[LAST_NOTE_KEY] = note
        }
    }

    fun lastDailyNoteFlow(): Flow<Pair<String?, String?>> {
        return context.dataStore.data.map { prefs ->
            Pair(
                prefs[LAST_NOTE_DAY_KEY],
                prefs[LAST_NOTE_KEY]
            )
        }
    }

    suspend fun setActiveWindow(startMin: Int, endMin: Int) {
        val safeStart = startMin.coerceIn(0, 1439)
        val safeEnd = endMin.coerceIn(0, 1439)

        context.dataStore.edit { prefs ->
            prefs[START_MIN_KEY] = safeStart
            prefs[END_MIN_KEY] = safeEnd
        }
    }
}
