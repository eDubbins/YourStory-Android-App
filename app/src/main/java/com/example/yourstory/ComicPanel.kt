
package com.example.yourstory
data class ComicPanel(
    val id: Int,
    val visualDescription: String, // What we send to Replicate
    val caption: String,           // What we show to the User
    var imageUrl: String? = null   // The result (starts null)
)