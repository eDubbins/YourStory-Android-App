package com.example.yourstory

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

data class ImageRequest(
    val input: InputParams
)

data class InputParams(
    val prompt: String,
    val aspect_ratio: String = "3:2",
    val output_format: String = "jpg"
)

data class ImageResponse(
    val output: Any? = null,
    val error: String? = null
)

interface ReplicateApi {
    @Headers(
        "Content-Type: application/json",
        "Prefer: wait"
    )
    @POST("v1/models/black-forest-labs/flux-1.1-pro/predictions")
    suspend fun generateImage(@Body request: ImageRequest): ImageResponse
}