package com.example.yourstory

import android.app.Application
import androidx.room.Room

class StoryApplication : Application() {
    // We create the database exactly ONCE and keep it open.
    // "lazy" means: Don't build it until the first time we ask for it.
    val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "story_database" // The name of the file on the phone
        ).build()
    }
}