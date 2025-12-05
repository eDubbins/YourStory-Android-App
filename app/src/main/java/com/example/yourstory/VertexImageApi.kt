package com.example.yourstory

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query


interface VertexImageApi {
    @Headers("Content-Type: application/json")
    @POST("v1/projects/{projectId}/locations/us-central1/publishers/google/models/imagen-3.0-fast-generate-001:predict")
    suspend fun generateImage(
        @Path("projectId") projectId: String,
        @Query("key") apiKey: String,
        @Body request: ImagenRequest
    ): ImagenResponse
}