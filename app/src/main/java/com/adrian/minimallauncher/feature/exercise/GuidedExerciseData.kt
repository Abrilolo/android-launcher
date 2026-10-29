package com.adrian.minimallauncher.feature.exercise

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.withTransaction
import com.adrian.minimallauncher.data.DayCheck
import com.adrian.minimallauncher.data.LauncherDatabase
import com.adrian.minimallauncher.data.ModuleSettings
import com.adrian.minimallauncher.data.PersonalRepository
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Versioned session drafts and the custom catalog; included in the launcher's normal backup. */
@Entity(tableName = "guided_exercise_records")
data class GuidedExerciseRecord(@PrimaryKey val id: String, val payload: String, val updatedAt: Long)

@Dao
interface GuidedExerciseDao {
    @Query("SELECT * FROM guided_exercise_records WHERE id = :id")
    suspend fun get(id: String): GuidedExerciseRecord?
    @Query("SELECT * FROM guided_exercise_records")
    suspend fun all(): List<GuidedExerciseRecord>
    @Upsert suspend fun put(record: GuidedExerciseRecord)
}

internal class GuidedExerciseRepository(private val db: LauncherDatabase) {
    private val records = db.guidedExercise()

    suspend fun load(date: LocalDate, settings: ModuleSettings): String {
        val session = TokyoGuide.sessionFor(date)
        val logs = db.exercise().allSets()
        val today = logs.filter { it.date == date.toString() }
        fun planItem(ex: PlanExercise): JSONObject {
            val last = logs.filter { it.exerciseId == ex.id && it.date < date.toString() }.maxByOrNull { it.createdAt }
            return JSONObject().put("id", ex.id).put("name", ex.name).put("zone", ex.group())
                .put("sets", JSONArray(List(ex.sets) { JSONArray(listOf(ex.minTarget, last?.weightKg ?: 0f)) }))
                .put("timed", ex.timed).put("posture", ex.how).put("tips", ex.care)
                .put("hist", JSONArray()).put("kcal", JSONObject.NULL)
        }
        val exercises = session.exercises.map(::planItem).ifEmpty {
            val minutes = session.minutes() ?: 15
            listOf(JSONObject().put("id", "session_${session.kind.name.lowercase()}")
                .put("name", if (session.kind == SessionKind.REST) "Paseo suave" else session.title)
                .put("zone", "cardio").put("timed", true)
                .put("sets", JSONArray().put(JSONArray().put(minutes * 60).put(0)))
                .put("posture", session.note).put("tips", session.note).put("hist", JSONArray()).put("kcal", JSONObject.NULL))
        }
        val catalog = TokyoGuide.week.flatMap { it.exercises }.distinctBy { it.id }.map(::planItem) + db.stack().all().map {
            JSONObject().put("id", it.id).put("name", it.name).put("zone", it.muscleGroup).put("timed", true)
                .put("sets", JSONArray().put(JSONArray().put(it.minutes * 60).put(0)))
                .put("posture", "").put("tips", "").put("hist", JSONArray()).put("kcal", JSONObject.NULL)
        }
        return JSONObject().put("version", 1).put("date", date.toString())
            .put("title", session.title).put("restSeconds", settings.exerciseRestSeconds)
            .put("groups", JSONArray(exercises.map { it.getString("zone") }.distinct().map { JSONArray(listOf(it, MuscleGroups[it] ?: it)) }))
            .put("exercises", JSONArray(exercises)).put("catalog", JSONArray(catalog))
            .put("logs", JSONArray(today.map { JSONObject().put("exerciseId", it.exerciseId).put("index", it.setIndex).put("r", it.reps).put("kg", it.weightKg.toDouble()) }))
            .put("history", JSONArray(logs.filter { it.date < date.toString() }.takeLast(2000).map {
                JSONObject().put("exerciseId", it.exerciseId).put("date", it.date).put("r", it.reps).put("kg", it.weightKg.toDouble())
            }))
            .put("saved", records.get("session:$date")?.let { JSONObject(it.payload) } ?: JSONObject.NULL)
            .put("library", records.get("library")?.let { JSONObject(it.payload) } ?: JSONObject.NULL).toString()
    }

    /** Acknowledged, transactional writes: repeated checkpoints never duplicate finished sets. */
    suspend fun save(date: LocalDate, text: String): Boolean = runCatching {
        require(text.length <= 2_000_000)
        val root = JSONObject(text)
        require(root.getInt("version") == 1 && root.getString("date") == date.toString())
        val state = root.getJSONObject("state")
        val logs = root.getJSONArray("logs")
        val managed = root.getJSONArray("managedIds")
        require(logs.length() <= 2000 && managed.length() <= 300)
        val ids = (0 until managed.length()).map { managed.getString(it).also { id -> require(id.length in 1..160) } }.toSet()
        val parsed = (0 until logs.length()).map { i ->
            val row = logs.getJSONObject(i)
            val id = row.getString("exerciseId"); val index = row.getInt("index")
            val kg = row.getDouble("kg"); val reps = row.getInt("r")
            require(id in ids && index in 0..999 && kg.isFinite() && kg in 0.0..500.0 && reps in 0..86400)
            SetLog(date = date.toString(), exerciseId = id, setIndex = index, weightKg = kg.toFloat(), reps = reps)
        }
        require(parsed.map { it.exerciseId to it.setIndex }.distinct().size == parsed.size)
        db.withTransaction {
            val previous = records.get("session:$date")?.let { JSONObject(it.payload) }
            if (previous?.optJSONArray("logs")?.toString() != logs.toString()) {
                val existing = db.exercise().allSets().filter { it.date == date.toString() }.associateBy { it.exerciseId to it.setIndex }
                // Only exercises owned by this draft are synchronized. Unrelated records are untouched.
                ids.forEach { db.exercise().deleteFrom(date.toString(), it, 0) }
                parsed.forEach { row -> db.exercise().insert(row.copy(createdAt = existing[row.exerciseId to row.setIndex]?.createdAt ?: row.createdAt)) }
            }
            records.put(GuidedExerciseRecord("session:$date", text, System.currentTimeMillis()))
            root.optJSONObject("library")?.let { records.put(GuidedExerciseRecord("library", it.toString(), System.currentTimeMillis())) }
            if (state.optString("stage") == "end") PersonalRepository(db).setCheck(date.toString(), DayCheck.WORKOUT, true)
        }
        true
    }.getOrDefault(false)
}
