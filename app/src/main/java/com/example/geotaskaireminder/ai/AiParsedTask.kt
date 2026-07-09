package com.example.geotaskaireminder.ai

data class AiParsedTask(
    val title: String,
    val description: String,
    val locationName: String,
    val priority: String,
)
