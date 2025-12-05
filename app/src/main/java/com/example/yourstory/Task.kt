package com.example.yourstory

import java.util.UUID
import androidx.room.TypeConverter
import androidx.room.Entity
import androidx.room.PrimaryKey

// The "Blueprint" for every item in your list
@Entity(tableName = "tasks")
data class Task(
    // A unique ID so the code can tell "Walk Dog" from "Walk Dog" (if you had two)
    @PrimaryKey val id: String = UUID.randomUUID().toString(),

    // The text description
    val title: String,

    // The checkbox state
    val isCompleted: Boolean = false,

    // Timestamp for when it was done (Crucial for the Comic Story later)
    val completedAt: String? = null,

    // The category (Helps the AI choose an art style/vibe)
    val type: TaskType = TaskType.NORMAL,

    val description: String = ""
)

// The specific categories available to choose from
enum class TaskType {
    NORMAL, // Slice of life
    CHORE,  // Housework
    WORK,   // Professional/Office
    HEALTH  // Fitness/Medical
}
class Converters {
    @TypeConverter
    fun fromTaskType(value: TaskType): String {
        return value.name
    }
    @TypeConverter
    fun toTaskType(value: String): TaskType {
        return TaskType.valueOf(value)
    }

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return value?.joinToString("|||") ?: "" // Use a rare separator like |||
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        return if (value.isNullOrEmpty()) emptyList() else value.split("|||")
    }
}