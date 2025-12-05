package com.example.yourstory

// --- REQUEST DATA CLASSES ---

data class ImagenRequest(
    // Vertex expects a list of prompt objects
    val instances: List<List_ImagenPrompt>,
    val parameters: ImagenParameters
)

data class List_ImagenPrompt(
    val prompt: String
)

data class ImagenParameters(
    val sampleCount: Int = 1,
    // Vertex supports "1:1", "16:9", "9:16", "3:2", "4:3"
    val aspectRatio: String = "1:1"
)

// --- RESPONSE DATA CLASSES ---

data class ImagenResponse(
    val predictions: List<ImagenPrediction>?
)

data class ImagenPrediction(
    val bytesBase64Encoded: String,
    val mimeType: String
)