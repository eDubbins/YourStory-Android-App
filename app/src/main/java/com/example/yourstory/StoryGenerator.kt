package com.example.yourstory

import com.google.ai.client.generativeai.GenerativeModel
import org.json.JSONArray
import org.json.JSONObject

class StoryGenerator {

    private val model = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = com.example.yourstory.BuildConfig.GEMINI_API_KEY
    )

    suspend fun generateComicPanels(tasks: List<Task>): List<ComicPanel> {
        // NEW: Filter not just by "Completed", but by "Completed TODAY"
        val today = java.time.LocalDate.now()

        val completed = tasks.filter { task ->
            if (task.isCompleted && task.completedAt != null) {
                try {
                    // Parse the timestamp string back to a Date
                    val completedDate = java.time.LocalDateTime.parse(task.completedAt).toLocalDate()
                    // Keep only if it matches Today
                    completedDate == today
                } catch (e: Exception) {
                    false // If date parsing fails, ignore it
                }
            } else {
                false
            }
        }

        // Fallback for empty days
        if (completed.isEmpty()) {
            return listOf(ComicPanel(1, "A hero sleeping in a bed, peaceful", "Nothing happened today."))
        }

        // The "System Prompt" to force JSON output
        val prompt = buildString {
            append("You are a comic book writer. Generate a 4-panel 'Slice of Life' comic based on these tasks:\n")
            completed.forEach { task -> append("- ${task.title} (${task.type.name})\n") }

            // NEW: Priority Instructions
            append("\nSTORY RULES:")
            append("\n1. Scan the list for tasks that seem unusual, specific, or out of the ordinary (e.g. 'Buy a Kayak' is more important than 'Do Laundry').")
            append("\n2. Prioritize these unique events as the main plot points.")
            append("\n3. If all tasks are mundane, exaggerate them significantly for comedic effect (e.g. 'Battle the dust bunnies' for cleaning).")

            append("\nCRITICAL INSTRUCTION: Output ONLY a valid JSON Array. Do not use Markdown. Do not write 'Here is the JSON'.")
            append("\nFormat: [{\"visual\": \"description of image\", \"caption\": \"text for panel\"}, ...]")
            append("\nMake the visual descriptions detailed (lighting, angle, style).")
        }

        return try {
            val response = model.generateContent(prompt)
            val rawText = response.text ?: ""

            // Clean up Markdown code blocks if Gemini adds them (```json ... ```)
            val cleanJson = rawText.replace("```json", "").replace("```", "").trim()

            parseJsonToPanels(cleanJson)
        } catch (e: Exception) {
            e.printStackTrace()
            // Return a fallback panel if parsing fails
            listOf(ComicPanel(1, "A robot confused, glitching", "Error: ${e.message}"))
        }
    }

    private fun parseJsonToPanels(jsonString: String): List<ComicPanel> {
        val panels = mutableListOf<ComicPanel>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                panels.add(
                    ComicPanel(
                        id = i + 1,
                        visualDescription = obj.getString("visual"),
                        caption = obj.getString("caption")
                    )
                )
            }
        } catch (e: Exception) {
            // If JSON fails, create one panel with the raw text so we can debug
            panels.add(ComicPanel(1, "Matrix code raining down", "Parsing Error: $jsonString"))
        }
        return panels
    }
}