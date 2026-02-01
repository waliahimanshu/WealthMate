package com.waliahimanshu.wealthmate.storage

import com.waliahimanshu.wealthmate.HouseholdFinances
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * GitHub Gist-based storage for syncing finance data across devices.
 * Data is stored in a secret Gist (only visible to the token owner).
 */
class GistStorage(
    private val token: String
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private val baseUrl = "https://api.github.com"
    private val gistFileName = "wealthmate_data.json"
    private val gistDescription = "WealthMate Finance Data (Auto-synced)"

    /**
     * Load finance data from Gist.
     * Returns null if no Gist exists yet or on error.
     */
    suspend fun loadData(): Result<HouseholdFinances?> {
        return try {
            val gistId = findExistingGist()
            if (gistId == null) {
                Result.success(null)
            } else {
                val gist = getGist(gistId)
                val content = gist.files[gistFileName]?.content
                if (content != null) {
                    Result.success(json.decodeFromString<HouseholdFinances>(content))
                } else {
                    Result.success(null)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Save finance data to Gist.
     * Creates a new Gist if one doesn't exist, otherwise updates existing.
     */
    suspend fun saveData(data: HouseholdFinances): Result<Unit> {
        return try {
            val content = json.encodeToString(data)
            val existingGistId = findExistingGist()

            if (existingGistId != null) {
                updateGist(existingGistId, content)
            } else {
                createGist(content)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Find existing WealthMate Gist by description.
     */
    private suspend fun findExistingGist(): String? {
        val response: HttpResponse = client.get("$baseUrl/gists") {
            header("Authorization", "Bearer $token")
            header("Accept", "application/vnd.github+json")
            header("X-GitHub-Api-Version", "2022-11-28")
        }

        if (!response.status.isSuccess()) {
            throw Exception("Failed to list gists: ${response.status}")
        }

        val gists: List<GistSummary> = response.body()
        return gists.find { it.description == gistDescription }?.id
    }

    /**
     * Get full Gist content by ID.
     */
    private suspend fun getGist(gistId: String): GistResponse {
        val response: HttpResponse = client.get("$baseUrl/gists/$gistId") {
            header("Authorization", "Bearer $token")
            header("Accept", "application/vnd.github+json")
            header("X-GitHub-Api-Version", "2022-11-28")
        }

        if (!response.status.isSuccess()) {
            throw Exception("Failed to get gist: ${response.status}")
        }

        return response.body()
    }

    /**
     * Create a new secret Gist.
     */
    private suspend fun createGist(content: String) {
        val request = CreateGistRequest(
            description = gistDescription,
            public = false,
            files = mapOf(gistFileName to GistFileContent(content))
        )

        val response: HttpResponse = client.post("$baseUrl/gists") {
            header("Authorization", "Bearer $token")
            header("Accept", "application/vnd.github+json")
            header("X-GitHub-Api-Version", "2022-11-28")
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (!response.status.isSuccess()) {
            throw Exception("Failed to create gist: ${response.status}")
        }
    }

    /**
     * Update existing Gist.
     */
    private suspend fun updateGist(gistId: String, content: String) {
        val request = UpdateGistRequest(
            description = gistDescription,
            files = mapOf(gistFileName to GistFileContent(content))
        )

        val response: HttpResponse = client.patch("$baseUrl/gists/$gistId") {
            header("Authorization", "Bearer $token")
            header("Accept", "application/vnd.github+json")
            header("X-GitHub-Api-Version", "2022-11-28")
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (!response.status.isSuccess()) {
            throw Exception("Failed to update gist: ${response.status}")
        }
    }

    /**
     * Validate the token by checking if we can access gists.
     */
    suspend fun validateToken(): Boolean {
        return try {
            val response: HttpResponse = client.get("$baseUrl/gists") {
                header("Authorization", "Bearer $token")
                header("Accept", "application/vnd.github+json")
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    fun close() {
        client.close()
    }
}

// API Response/Request models

@Serializable
private data class GistSummary(
    val id: String,
    val description: String? = null
)

@Serializable
private data class GistResponse(
    val id: String,
    val description: String? = null,
    val files: Map<String, GistFile>
)

@Serializable
private data class GistFile(
    val filename: String,
    val content: String? = null
)

@Serializable
private data class CreateGistRequest(
    val description: String,
    val public: Boolean,
    val files: Map<String, GistFileContent>
)

@Serializable
private data class UpdateGistRequest(
    val description: String,
    val files: Map<String, GistFileContent>
)

@Serializable
private data class GistFileContent(
    val content: String
)
