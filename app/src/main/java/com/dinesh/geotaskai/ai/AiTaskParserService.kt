package com.dinesh.geotaskai.ai

import com.dinesh.geotaskai.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class AiTaskParserService(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY,
) {
    fun parseTask(naturalLanguageText: String): Result<AiParsedTask> {
        val input = naturalLanguageText.trim()
        if (input.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter a task description first."))
        }
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("Add GEMINI_API_KEY to local.properties."))
        }

        return runCatching {
            val connection = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = TIMEOUT_MILLIS
                readTimeout = TIMEOUT_MILLIS
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-goog-api-key", apiKey)
            }

            connection.outputStream.use { output ->
                output.write(buildRequestBody(input).toString().toByteArray(Charsets.UTF_8))
            }

            val responseBody = connection.readResponseBody()
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Gemini request failed: ${connection.responseCode}")
            }

            parseResponse(responseBody)
        }
    }

    private fun buildRequestBody(input: String): JSONObject {
        val prompt = """
            Extract task fields from this reminder text.
            Return only JSON with:
            title: short task title
            description: useful task details
            locationName: named place from the text
            priority: one of Low, Medium, High

            Text: "$input"
        """.trimIndent()

        return JSONObject()
            .put("model", MODEL)
            .put(
                "system_instruction",
                "You extract reminder tasks into strict JSON for an Android task app.",
            )
            .put("input", prompt)
            .put(
                "response_format",
                JSONObject()
                    .put("type", "text")
                    .put("mime_type", "application/json")
                    .put("schema", buildResponseSchema()),
            )
    }

    private fun buildResponseSchema(): JSONObject {
        val properties = JSONObject()
            .put("title", JSONObject().put("type", "string"))
            .put("description", JSONObject().put("type", "string"))
            .put("locationName", JSONObject().put("type", "string"))
            .put(
                "priority",
                JSONObject()
                    .put("type", "string")
                    .put("enum", JSONArray(listOf("Low", "Medium", "High"))),
            )

        return JSONObject()
            .put("type", "object")
            .put("properties", properties)
            .put("required", JSONArray(listOf("title", "description", "locationName", "priority")))
    }

    private fun parseResponse(responseBody: String): AiParsedTask {
        val response = JSONObject(responseBody)
        if (response.has("title")) {
            return response.toParsedTask()
        }

        val steps = response.optJSONArray("steps")
        val outputText = response.optString("output_text").ifBlank {
            if (steps != null && steps.length() > 0) {
                steps.optJSONObject(steps.length() - 1)?.optString("output_text").orEmpty()
            } else {
                ""
            }
        }.stripJsonCodeFence()
        if (outputText.isBlank()) {
            throw IllegalStateException("Gemini response did not include parsed text.")
        }

        return JSONObject(outputText).toParsedTask()
    }

    private fun JSONObject.toParsedTask(): AiParsedTask {
        return AiParsedTask(
            title = optString("title").trim(),
            description = optString("description").trim(),
            locationName = optString("locationName").trim(),
            priority = optString("priority").normalizePriority(),
        )
    }

    private fun HttpURLConnection.readResponseBody(): String {
        val stream = if (responseCode in 200..299) inputStream else errorStream
        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
    }

    private fun String.normalizePriority(): String {
        return when (trim().lowercase()) {
            "high" -> "High"
            "low" -> "Low"
            else -> "Medium"
        }
    }

    private fun String.stripJsonCodeFence(): String {
        return trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    private companion object {
        const val API_URL = "https://generativelanguage.googleapis.com/v1beta/interactions"
        const val MODEL = "gemini-3.5-flash"
        const val TIMEOUT_MILLIS = 20_000
    }
}
