package com.example.yourstory

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ImageGenerator {

    private val client = OkHttpClient.Builder()
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val api = Retrofit.Builder()
        // FIXED: Vertex AI Base URL
        .baseUrl("https://us-central1-aiplatform.googleapis.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(VertexImageApi::class.java)

    suspend fun generateComicPanel(prompt: String): String? {
        try {
            println("APP_DEBUG: Requesting Imagen 3.0 Fast (Vertex)...")

            // 1. Build Request
            val request = ImagenRequest(
                instances = listOf(
                    // Note: 'prompt' parameter inside List_ImagenPrompt class
                    List_ImagenPrompt(prompt = "comic book style, manga, black and white, line art: $prompt")
                ),
                parameters = ImagenParameters(sampleCount = 1)
            )

            // 2. Call API (Passing Project ID and Key)
            val response = api.generateImage(
                projectId = com.example.yourstory.BuildConfig.VERTEX_PROJECT_ID,
                apiKey = com.example.yourstory.BuildConfig.VERTEX_API_KEY,
                request = request
            )

            // 3. Extract Image
            val prediction = response.predictions?.firstOrNull()

            return if (prediction != null) {
                val mimeType = prediction.mimeType
                val base64Data = prediction.bytesBase64Encoded.trim()
                println("APP_DEBUG: Vertex delivered! Format: $mimeType")
                "data:$mimeType;base64,$base64Data"
            } else {
                println("APP_DEBUG: No prediction in response.")
                null
            }

        } catch (e: Exception) {
            println("APP_DEBUG: CRASH! ${e.message}")
            e.printStackTrace()
            return null
        }
    }
}