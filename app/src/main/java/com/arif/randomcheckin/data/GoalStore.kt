package com.arif.randomcheckin.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.arif.randomcheckin.data.model.Goal
import com.arif.randomcheckin.data.model.GoalId
import com.arif.randomcheckin.data.model.ThemeMode
import com.arif.randomcheckin.data.model.goalDateFormatter
import com.arif.randomcheckin.data.model.isActive
import com.arif.randomcheckin.notifications.NotificationHelper
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/** Hard cap keeps UI/business rules aligned across layers (max 3 active goals). */
const val MAX_ACTIVE_GOALS = 3

private const val DATA_STORE_NAME = "goal_store"
private const val DEFAULT_ACTIVE_WINDOW_START_MIN = 9 * 60
private const val DEFAULT_ACTIVE_WINDOW_END_MIN = 21 * 60
private const val MINUTES_PER_DAY = (24 * 60) - 1

private val Context.dataStore by preferencesDataStore(name = DATA_STORE_NAME)

class GoalStore(private val context: Context) {

    private val GOALS_KEY = stringPreferencesKey("goals_json")
    private val START_MIN_KEY = intPreferencesKey("active_start_min")
    private val END_MIN_KEY = intPreferencesKey("active_end_min")
    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

    /** Emits every persisted goal so UI progress bars and tabs stay in sync with storage. */
    fun goalListFlow(): Flow<List<Goal>> = context.dataStore.data.map { prefs ->
        prefs.goalList()
    }

    /**
     * Persists a new goal while enforcing business constraints: start date is today, end dates cannot be in the past,
     * and only [MAX_ACTIVE_GOALS] active goals may exist at any time.
     */
    suspend fun addGoal(title: String, endDate: String) {
        val trimmedEndDate = endDate.trim()
        context.dataStore.edit { prefs ->
            val storedGoals = prefs.goalList()
            val today = LocalDate.now()
            val newGoal = Goal(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                startDate = today.format(goalDateFormatter),
                endDate = trimmedEndDate
            )
            if (newGoal.isActive(today) && storedGoals.activeCount(today) >= MAX_ACTIVE_GOALS) {
                throw IllegalStateException("Goal limit reached")
            }
            prefs.writeGoals(storedGoals + newGoal)
        }
    }

    suspend fun removeGoal(goalId: GoalId) {
        context.dataStore.edit { prefs ->
            val updated = prefs.goalList().filterNot { it.id == goalId }
            prefs.writeGoals(updated)
        }
    }

    /**
     * Updates an existing goal and re-validates the active goal cap so edits cannot sneak past the focus rule.
     */
    suspend fun updateGoal(goalId: String, title: String, endDate: String) {
        val trimmedEndDate = endDate.trim()
        context.dataStore.edit { prefs ->
            val stored = prefs.goalList()
            val index = stored.indexOfFirst { it.id == goalId }
            if (index == -1) return@edit
            val today = LocalDate.now()
            val updatedList = stored.toMutableList().apply {
                this[index] = stored[index].copy(
                    title = title.trim(),
                    endDate = trimmedEndDate
                )
            }
            if (updatedList.activeCount(today) > MAX_ACTIVE_GOALS) {
                throw IllegalStateException("Goal limit reached")
            }
            prefs.writeGoals(updatedList)
        }
    }

    /**
     * Marks a goal as completed by shifting its end date to yesterday, which causes it to be filtered into the
     * completed tab without requiring UI-side logic.
     */
    suspend fun completeGoal(goalId: String) {
        context.dataStore.edit { prefs ->
            val stored = prefs.goalList()
            val index = stored.indexOfFirst { it.id == goalId }
            if (index == -1) return@edit
            val completionDate = LocalDate.now().minusDays(1).format(goalDateFormatter)
            val updated = stored.toMutableList().apply {
                this[index] = stored[index].copy(endDate = completionDate)
            }
            prefs.writeGoals(updated)
        }
    }

    fun activeWindowFlow(): Flow<Pair<Int, Int>> = context.dataStore.data.map { prefs ->
        val startMin = prefs[START_MIN_KEY] ?: DEFAULT_ACTIVE_WINDOW_START_MIN
        val endMin = prefs[END_MIN_KEY] ?: DEFAULT_ACTIVE_WINDOW_END_MIN
        Pair(startMin, endMin)
    }

    fun sendTestNotification(goal: Goal) {
        NotificationHelper.showGoalTestNotification(context, goal)
    }

    /** Clamps inputs to valid minutes so notifications never read outside a single 24h window. */
    suspend fun setActiveWindow(startMin: Int, endMin: Int) {
        val safeStart = startMin.coerceIn(0, MINUTES_PER_DAY)
        val safeEnd = endMin.coerceIn(0, MINUTES_PER_DAY)
        context.dataStore.edit { prefs ->
            prefs[START_MIN_KEY] = safeStart
            prefs[END_MIN_KEY] = safeEnd
        }
    }

    fun themeModeFlow(): Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        ThemeMode.fromStoredValue(prefs[THEME_MODE_KEY])
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode.name
        }
    }

    private fun Preferences.goalList(): List<Goal> = this[GOALS_KEY]?.toGoalList().orEmpty()

    private fun MutablePreferences.writeGoals(goals: List<Goal>) {
        if (goals.isEmpty()) {
            remove(GOALS_KEY)
        } else {
            this[GOALS_KEY] = goals.toJson()
        }
    }

    private fun List<Goal>.activeCount(referenceDate: LocalDate): Int = count { it.isActive(referenceDate) }
}

private const val FIELD_ID = "id"
private const val FIELD_TITLE = "title"
private const val FIELD_START_DATE = "startDate"
private const val FIELD_END_DATE = "endDate"

internal fun String.toGoalList(): List<Goal> {
    val jsonArray = JSONArray(this)
    val goals = mutableListOf<Goal>()
    for (i in 0 until jsonArray.length()) {
        val entry = jsonArray.getJSONObject(i)
        goals += Goal(
            id = entry.getString(FIELD_ID),
            title = entry.getString(FIELD_TITLE),
            // Legacy entries may lack a start date; fall back to endDate to preserve ordering.
            startDate = entry.optString(FIELD_START_DATE, entry.getString(FIELD_END_DATE)),
            endDate = entry.getString(FIELD_END_DATE)
        )
    }
    return goals
}

internal fun List<Goal>.toJson(): String {
    val array = JSONArray()
    for (goal in this) {
        val json = JSONObject()
        json.put(FIELD_ID, goal.id)
        json.put(FIELD_TITLE, goal.title)
        json.put(FIELD_START_DATE, goal.startDate)
        json.put(FIELD_END_DATE, goal.endDate)
        array.put(json)
    }
    return array.toString()
}
