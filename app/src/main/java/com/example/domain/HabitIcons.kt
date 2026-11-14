package com.example.domain

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.local.CategoryEntity

data class HabitIconOption(
    val key: String,
    val label: String,
    val icon: ImageVector
)

object HabitCatalog {
    val habitIcons: List<HabitIconOption> = listOf(
        HabitIconOption("check_circle", "Check", Icons.Rounded.CheckCircle),
        HabitIconOption("favorite", "Health", Icons.Rounded.Favorite),
        HabitIconOption("fitness_center", "Workout", Icons.Rounded.FitnessCenter),
        HabitIconOption("directions_run", "Run", Icons.Rounded.DirectionsRun),
        HabitIconOption("water_drop", "Hydrate", Icons.Rounded.WaterDrop),
        HabitIconOption("local_drink", "Drink", Icons.Rounded.LocalDrink),
        HabitIconOption("self_improvement", "Meditate", Icons.Rounded.SelfImprovement),
        HabitIconOption("spa", "Wellness", Icons.Rounded.Spa),
        HabitIconOption("bedtime", "Sleep", Icons.Rounded.Bedtime),
        HabitIconOption("menu_book", "Read", Icons.Rounded.MenuBook),
        HabitIconOption("school", "Study", Icons.Rounded.School),
        HabitIconOption("code", "Code", Icons.Rounded.Code),
        HabitIconOption("work", "Focus", Icons.Rounded.Work),
        HabitIconOption("psychology", "Mindset", Icons.Rounded.Psychology),
        HabitIconOption("account_balance_wallet", "Finance", Icons.Rounded.AccountBalanceWallet),
        HabitIconOption("palette", "Create", Icons.Rounded.Palette),
        HabitIconOption("music_note", "Music", Icons.Rounded.MusicNote),
        HabitIconOption("auto_awesome", "Growth", Icons.Rounded.AutoAwesome)
    )

    val colorSwatches: List<String> = listOf(
        "#6C63FF",
        "#A78BFA",
        "#22C55E",
        "#0EA5E9",
        "#F59E0B",
        "#EC4899",
        "#14B8A6",
        "#EF4444"
    )

    fun iconForKey(key: String): ImageVector {
        return when (key) {
            "bolt" -> Icons.Rounded.Bolt
            "local_fire_department" -> Icons.Rounded.LocalFireDepartment
            "workspace_premium" -> Icons.Rounded.WorkspacePremium
            "military_tech" -> Icons.Rounded.MilitaryTech
            "emoji_events" -> Icons.Rounded.EmojiEvents
            "diamond" -> Icons.Rounded.Diamond
            "trending_up" -> Icons.Rounded.TrendingUp
            "stars" -> Icons.Rounded.Stars
            "book" -> Icons.Rounded.Book
            else -> habitIcons.firstOrNull { it.key == key }?.icon ?: Icons.Rounded.CheckCircle
        }
    }

    fun parseColor(hex: String, fallback: Color = Color(0xFF6C63FF)): Color {
        return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)
    }

    fun defaultCategories(userId: String): List<CategoryEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            CategoryEntity("cat_health", userId, "Health", "favorite", "#EF4444", isDefault = true, updatedAtEpochMs = now),
            CategoryEntity("cat_fitness", userId, "Fitness", "fitness_center", "#22C55E", isDefault = true, updatedAtEpochMs = now),
            CategoryEntity("cat_education", userId, "Education", "school", "#0EA5E9", isDefault = true, updatedAtEpochMs = now),
            CategoryEntity("cat_productivity", userId, "Productivity", "work", "#6C63FF", isDefault = true, updatedAtEpochMs = now),
            CategoryEntity("cat_personal_growth", userId, "Personal Growth", "auto_awesome", "#A78BFA", isDefault = true, updatedAtEpochMs = now),
            CategoryEntity("cat_mindfulness", userId, "Mindfulness", "self_improvement", "#14B8A6", isDefault = true, updatedAtEpochMs = now),
            CategoryEntity("cat_finance", userId, "Finance", "account_balance_wallet", "#F59E0B", isDefault = true, updatedAtEpochMs = now),
            CategoryEntity("cat_lifestyle", userId, "Lifestyle", "spa", "#EC4899", isDefault = true, updatedAtEpochMs = now),
            CategoryEntity("cat_other", userId, "Other", "check_circle", "#64748B", isDefault = true, updatedAtEpochMs = now)
        )
    }
}
