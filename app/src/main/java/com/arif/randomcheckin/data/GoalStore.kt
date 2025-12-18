package com.arif.randomcheckin.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.arif.randomcheckin.data.model.Goal
import com.arif.randomcheckin.data.model.ThemeMode
import com.arif.randomcheckin.data.model.goalDateFormatter
import com.arif.randomcheckin.data.model.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

const val MAX_ACTIVE_GOALS = 3

private val Context.dataStore by preferencesDataStore(name = "goal_store")

class GoalStore(private val context: Context) {

    private val GOALS_KEY = stringPreferencesKey("goals_json")
    private val START_MIN_KEY = intPreferencesKey("active_start_min")
    private val END_MIN_KEY = intPreferencesKey("active_end_min")
    private val LAST_NOTE_DAY_KEY = stringPreferencesKey("last_note_day")
    private val LAST_NOTE_KEY = stringPreferencesKey("last_note")
    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

    fun goalListFlow(): Flow<List<Goal>> = context.dataStore.data.map { prefs ->
        prefs[GOALS_KEY]?.toGoalList().orEmpty()
    }

    suspend fun addGoal(title: String, description: String, endDate: String) {
        context.dataStore.edit { prefs ->
            val stored = prefs[GOALS_KEY]?.toGoalList().orEmpty()
            val today = LocalDate.now()
            val newGoal = Goal(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                description = description.trim(),
                startDate = today.format(goalDateFormatter),
                endDate = endDate.trim()
            )
            val activeCount = stored.count { it.isActive(today) }
            if (newGoal.isActive(today) && activeCount >= MAX_ACTIVE_GOALS) {
                throw IllegalStateException("Goal limit reached")
            }
            val updated = stored + newGoal
            prefs[GOALS_KEY] = updated.toJson()
        }
    }

    suspend fun removeGoal(goalId: String) {
        context.dataStore.edit { prefs ->
            val updated = prefs[GOALS_KEY]?.toGoalList().orEmpty().filterNot { it.id == goalId }
            if (updated.isEmpty()) {
                prefs.remove(GOALS_KEY)
            } else {
                prefs[GOALS_KEY] = updated.toJson()
            }
        }
    }

    suspend fun updateGoal(goalId: String, title: String, description: String, endDate: String) {
        context.dataStore.edit { prefs ->
            val stored = prefs[GOALS_KEY]?.toGoalList().orEmpty()
            val index = stored.indexOfFirst { it.id == goalId }
            if (index == -1) return@edit
            val today = LocalDate.now()
            val updatedList = stored.toMutableList().apply {
                this[index] = stored[index].copy(
                    title = title.trim(),
                    description = description.trim(),
                    endDate = endDate.trim()
                )
            }
            val activeCount = updatedList.count { it.isActive(today) }
            if (activeCount > MAX_ACTIVE_GOALS) {
                throw IllegalStateException("Goal limit reached")
            }
            prefs[GOALS_KEY] = updatedList.toJson()
        }
    }

    suspend fun completeGoal(goalId: String) {
        context.dataStore.edit { prefs ->
            val stored = prefs[GOALS_KEY]?.toGoalList().orEmpty()
            val index = stored.indexOfFirst { it.id == goalId }
            if (index == -1) return@edit
            val updated = stored.toMutableList().apply {
                this[index] = stored[index].copy(endDate = LocalDate.now().minusDays(1).format(goalDateFormatter))
            }
            prefs[GOALS_KEY] = updated.toJson()
        }
    }

    fun activeWindowFlow(): Flow<Pair<Int, Int>> = context.dataStore.data.map { prefs ->
        val startMin = prefs[START_MIN_KEY] ?: (9 * 60)
        val endMin = prefs[END_MIN_KEY] ?: (21 * 60)
        Pair(startMin, endMin)
    }

    suspend fun setActiveWindow(startMin: Int, endMin: Int) {
        val safeStart = startMin.coerceIn(0, 1439)
        val safeEnd = endMin.coerceIn(0, 1439)
        context.dataStore.edit { prefs ->
            prefs[START_MIN_KEY] = safeStart
            prefs[END_MIN_KEY] = safeEnd
        }
    }

    suspend fun saveDailyNote(note: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        context.dataStore.edit { prefs ->
            prefs[LAST_NOTE_DAY_KEY] = today
            prefs[LAST_NOTE_KEY] = note
        }
    }

    fun lastDailyNoteFlow(): Flow<Pair<String?, String?>> = context.dataStore.data.map { prefs ->
        Pair(prefs[LAST_NOTE_DAY_KEY], prefs[LAST_NOTE_KEY])
    }

    fun themeModeFlow(): Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        ThemeMode.fromStoredValue(prefs[THEME_MODE_KEY])
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode.name
        }
    }
}

internal fun String.toGoalList(): List<Goal> {
    val jsonArray = JSONArray(this)
    val goals = mutableListOf<Goal>()
    for (i in 0 until jsonArray.length()) {
        val entry = jsonArray.getJSONObject(i)
        goals += Goal(
            id = entry.getString("id"),
            title = entry.getString("title"),
            description = entry.getString("description"),
            startDate = entry.optString("startDate", entry.getString("endDate")),
            endDate = entry.getString("endDate")
        )
    }
    return goals
}

internal fun List<Goal>.toJson(): String {
    val array = JSONArray()
    for (goal in this) {
        val json = JSONObject()
        json.put("id", goal.id)
        json.put("title", goal.title)
        json.put("description", goal.description)
        json.put("startDate", goal.startDate)
        json.put("endDate", goal.endDate)
        array.put(json)
    }
    return array.toString()
}
