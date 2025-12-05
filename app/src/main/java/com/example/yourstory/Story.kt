package com.example.yourstory

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "stories")
data class Story(
    @PrimaryKey val date: String = LocalDate.now().toString(),

    val completedTasks: List<String>,

    val panel1Path: String?,
    val panel1Caption: String,

    val panel2Path: String?,
    val panel2Caption: String,

    val panel3Path: String?,
    val panel3Caption: String,

    val panel4Path: String?,
    val panel4Caption: String
)