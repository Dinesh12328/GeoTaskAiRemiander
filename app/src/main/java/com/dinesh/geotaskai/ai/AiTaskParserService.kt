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

            try {
                connection.outputStream.use { output ->
                    output.write(buildRequestBody(input).toString().toByteArray(Charsets.UTF_8))
                }

                val responseBody = connection.readResponseBody()
                if (connection.responseCode !in 200..299) {
                    throw IllegalStateException(buildHttpErrorMessage(connection.responseCode, responseBody))
                }

                parseResponse(responseBody).validated()
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun buildRequestBody(input: String): JSONObject {
        val prompt = """
            Extract reminder task fields from this user text.

            Return only JSON with these fields:
            - title: short action phrase without reminder wording or location timing words
            - description: one useful sentence describing the reminder
            - locationName: named place from the text
            - priority: one of Low, Medium, High

            Priority rules:
            - High for urgent, important, emergency, exam, deadline, or today
            - Low for casual or optional tasks
            - Medium for normal reminders

            Text: "$input"
        """.trimIndent()

        return JSONObject()
            .put("model", MODEL)
            .put("store", false)
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
            .put(
                "title",
                JSONObject()
                    .put("type", "string")
                    .put("description", "Short action title for the task."),
            )
            .put(
                "description",
                JSONObject()
                    .put("type", "string")
                    .put("description", "One sentence with useful reminder details."),
            )
            .put(
                "locationName",
                JSONObject()
                    .put("type", "string")
                    .put("description", "Named place that should trigger the reminder."),
            )
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

        val outputText = response.extractOutputText().stripJsonCodeFence().extractJsonObject()
        if (outputText.isBlank()) {
            throw IllegalStateException("Gemini response did not include parsed text.")
        }

        return JSONObject(outputText).toParsedTask()
    }

    private fun JSONObject.extractOutputText(): String {
        optString("output_text").takeIf { it.isNotBlank() }?.let { return it }

        val steps = optJSONArray("steps") ?: return ""
        for (index in steps.length() - 1 downTo 0) {
            val step = steps.optJSONObject(index) ?: continue
            step.optString("output_text").takeIf { it.isNotBlank() }?.let { return it }
            step.optString("text").takeIf { it.isNotBlank() }?.let { return it }
        }
        return ""
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
        if (stream == null) return ""
        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
    }

    private fun buildHttpErrorMessage(responseCode: Int, responseBody: String): String {
        val apiMessage = runCatching {
            JSONObject(responseBody)
                .optJSONObject("error")
                ?.optString("message")
                .orEmpty()
        }.getOrDefault("")

        return if (apiMessage.isNotBlank()) {
            "Gemini request failed: $apiMessage"
        } else {
            "Gemini request failed: HTTP $responseCode"
        }
    }

    private fun AiParsedTask.validated(): AiParsedTask {
        if (title.isBlank()) {
            throw IllegalStateException("Gemini did not return a task title.")
        }
        if (locationName.isBlank()) {
            throw IllegalStateException("Gemini did not return a location name.")
        }

        return copy(
            description = description.ifBlank { title },
            priority = priority.normalizePriority(),
        )
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

    private fun String.extractJsonObject(): String {
        val trimmed = trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed
        }

        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        return if (start >= 0 && end > start) {
            trimmed.substring(start, end + 1)
        } else {
            trimmed
        }
    }

    private companion object {
        const val API_URL = "https://generativelanguage.googleapis.com/v1beta/interactions"
        const val MODEL = "gemini-3.5-flash"
        const val TIMEOUT_MILLIS = 20_000
    }
}
