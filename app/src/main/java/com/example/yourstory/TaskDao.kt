package com.example.yourstory // <--- Check your package name!

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    // 1. Get all tasks. 'Flow' means it updates in real-time.
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC")
    fun getAllTasks(): Flow<List<Task>>

    // 2. Add a task.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    // 3. Update a task (check/uncheck).
    @Update
    suspend fun update(task: Task): Int

    // 4. Delete (Optional, but good practice).
    @Delete
    suspend fun delete(task: Task): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE title = :title")
    suspend fun countTasksWithTitle(title: String): Int
}