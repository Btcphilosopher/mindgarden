package com.example.ui.ai

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class Part(
    @Json(name = "text") val text: String? = null
)

data class Content(
    @Json(name = "parts") val parts: List<Part>
)

data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

data class Candidate(
    @Json(name = "content") val content: Content? = null
)

data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: GeminiApiService = retrofit.create(GeminiApiService::class.java)

    /**
     * General content generation interface. Checks API key presence safely.
     */
    suspend fun generateResponse(prompt: String, systemPrompt: String? = null): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "GEMINI_API_KEY") {
            return "API_KEY_MISSING"
        }
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = systemPrompt?.let { Content(parts = listOf(Part(text = it))) }
        )
        return try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Empty response from cognitive engine."
        } catch (e: Exception) {
            "Error: ${e.localizedMessage ?: e.message}"
        }
    }

    /**
     * Task: Auto-suggest 3-5 tags for a note given its title and content.
     */
    suspend fun suggestTags(title: String, content: String): List<String> {
        val prompt = "Based on this note title: \"$title\" and text content: \"$content\", suggest up to 4 relevant single-word tags as a simple comma-separated list without hashtags (e.g., design, computer, research, thought)."
        val res = generateResponse(prompt, "You are a concise, helpful note categorization assistant. Return only a comma-separated list of tags, nothing else.")
        if (res == "API_KEY_MISSING" || res.startsWith("Error:")) return emptyList()
        return res.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() && it.length > 1 }
    }

    /**
     * Task: Structured Weekly Summary of Journal Entries
     */
    suspend fun generateWeeklySummary(journalNotes: List<String>): String {
        if (journalNotes.isEmpty()) {
            return "No journals saved for this period yet. Write down daily thoughts, select mood levels, and they will be analyzed here."
        }
        val combinedText = journalNotes.joinToString("\n---\n")
        val prompt = "Analyze the following personal journals logs written this week and generate a structured cognitive summary with action items, pattern analysis of emotions/moods, and helpful mental alignment insights:\n\n$combinedText"
        val systemPrompt = "You are an empathetic, professional cognitive thinking companion. You specialize in journal reflection, mental clarity, and helping users connect their scattered daily thoughts into positive progress frameworks. Use clear bullet points and markdown titles."
        return generateResponse(prompt, systemPrompt)
    }

    /**
     * Task: Extract actionable tasks or checklists from a note
     */
    suspend fun extractChecklist(title: String, content: String): List<String> {
        val prompt = "Extract all actionable tasks or checklist steps hidden inside this note (Title: \"$title\", Content: \"$content\"). Write them as a simple, newline-separated list where each line represents one specific task (do not use bullet points or numbering, just text)."
        val res = generateResponse(prompt, "You are a precise task extractor. Output only the plaintext tasks, one task per newline. No formatting, no extra dialogue.")
        if (res == "API_KEY_MISSING" || res.startsWith("Error:")) return emptyList()
        return res.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}
