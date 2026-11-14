package com.example.domain

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.local.CategoryEntity
import com.example.data.local.DailyJournalEntity
import com.example.data.local.HabitCompletionEntity
import com.example.data.local.HabitEntity
import com.example.data.local.UserAchievementEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object DataExportHelper {

    fun buildExportJson(
        userId: String,
        displayName: String,
        email: String,
        categories: List<CategoryEntity>,
        habits: List<HabitEntity>,
        completions: List<HabitCompletionEntity>,
        journals: List<DailyJournalEntity>,
        achievements: List<UserAchievementEntity>
    ): String {
        val root = JSONObject()
        root.put("app", "Habitly")
        root.put("exportedAtEpochMs", System.currentTimeMillis())
        root.put("user", JSONObject().apply {
            put("id", userId)
            put("displayName", displayName)
            put("email", email)
        })

        root.put("categories", JSONArray().apply {
            categories.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("colorHex", c.colorHex)
                })
            }
        })

        root.put("habits", JSONArray().apply {
            habits.forEach { h ->
                put(JSONObject().apply {
                    put("id", h.id)
                    put("name", h.name)
                    put("description", h.description)
                    put("categoryId", h.categoryId)
                    put("frequencyType", h.frequencyType)
                    put("startDate", h.startDate)
                    put("isPaused", h.isPaused)
                    put("isArchived", h.isArchived)
                })
            }
        })

        root.put("completions", JSONArray().apply {
            completions.forEach { cp ->
                put(JSONObject().apply {
                    put("id", cp.id)
                    put("habitId", cp.habitId)
                    put("dateIso", cp.dateIso)
                    put("isCompleted", cp.isCompleted)
                    put("note", cp.note)
                })
            }
        })

        root.put("journals", JSONArray().apply {
            journals.forEach { j ->
                put(JSONObject().apply {
                    put("id", j.id)
                    put("dateIso", j.dateIso)
                    put("mood", j.mood)
                    put("content", j.content)
                    put("habitId", j.habitId)
                })
            }
        })

        root.put("achievements", JSONArray().apply {
            achievements.forEach { a ->
                put(JSONObject().apply {
                    put("key", a.achievementKey)
                    put("unlockedAtEpochMs", a.unlockedAtEpochMs)
                    put("pointsAwarded", a.pointsAwarded)
                })
            }
        })

        return root.toString(2)
    }

    fun shareJsonExport(context: Context, jsonContent: String) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "habitly_backup_${System.currentTimeMillis()}.json")
        file.writeText(jsonContent)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Habitly Data Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export Habitly Data"))
    }
}
