package com.waliahimanshu.wealthmate.storage

import com.waliahimanshu.wealthmate.HouseholdFinances
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
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
    token: String,
    private val localStorage: LocalStorage? = null
) {
    // Trim whitespace from token to avoid "invalid header char" errors
    private val token: String = token.trim()
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30000  // 30 seconds total request timeout
            connectTimeoutMillis = 10000  // 10 seconds to establish connection
        }
    }

    private val baseUrl = "https://api.github.com"
    private val gistFileName = "wealthmate_data.json"
    private val gistDescription = "WealthMate Finance Data (Auto-synced)"

    // In-memory cache of Gist ID
    private var cachedGistId: String? = null

    /**
     * Load finance data from Gist.
     * Returns null if no Gist exists yet or on error.
     */
    suspend fun loadData(): Result<HouseholdFinances?> {
        return try {
            println("GistStorage: Loading data from cloud...")
            val gistId = getOrFindGistId()
            if (gistId == null) {
                println("GistStorage: No gist found, returning null")
                Result.success(null)
            } else {
                println("GistStorage: Fetching gist content...")
                val gist = getGist(gistId)
                val content = gist.files[gistFileName]?.content
                if (content != null) {
                    val data = json.decodeFromString<HouseholdFinances>(content)
                    println("GistStorage: Loaded data - members: ${data.members.size}, investments: ${data.investments.size}")
                    Result.success(data)
                } else {
                    println("GistStorage: Gist exists but file content is null")
                    Result.success(null)
                }
            }
        } catch (e: HttpRequestTimeoutException) {
            println("GistStorage: Load timeout - ${e.message}")
            Result.failure(Exception("Request timed out. Check your internet connection."))
        } catch (e: Exception) {
            println("GistStorage: Load failed - ${e::class.simpleName}: ${e.message}")
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
            println("GistStorage: Saving data - members: ${data.members.size}, investments: ${data.investments.size}, savings: ${data.allSavings.size}")

            val existingGistId = getOrFindGistId()
            println("GistStorage: Using Gist ID: $existingGistId")

            if (existingGistId != null) {
                updateGist(existingGistId, content)
                println("GistStorage: Successfully updated existing gist")
            } else {
                val newGistId = createGist(content)
                cachedGistId = newGistId
                localStorage?.saveGistId(newGistId)
                println("GistStorage: Created new gist with ID: $newGistId")
            }
            Result.success(Unit)
        } catch (e: HttpRequestTimeoutException) {
            println("GistStorage: Request timeout - ${e.message}")
            Result.failure(Exception("Request timed out. Check your internet connection."))
        } catch (e: Exception) {
            println("GistStorage: Save failed - ${e::class.simpleName}: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get cached Gist ID or find it from API.
     * Priority: memory cache -> local storage -> API search
     */
    private suspend fun getOrFindGistId(): String? {
        // 1. Return from memory cache
        cachedGistId?.let { return it }

        // 2. Try to load from local storage
        val storedId = localStorage?.loadGistId()
        if (storedId != null) {
            cachedGistId = storedId
            return storedId
        }

        // 3. Search via API as last resort
        val foundId = searchForGistInApi()
        if (foundId != null) {
            cachedGistId = foundId
            localStorage?.saveGistId(foundId)
        }
        return foundId
    }

    /**
     * Find existing WealthMate Gist by description via API.
     * Fetches up to 100 gists to handle users with many gists.
     */
    private suspend fun searchForGistInApi(): String? {
        println("GistStorage: Searching for existing gist...")
        val response: HttpResponse = client.get("$baseUrl/gists?per_page=100") {
            header("Authorization", "Bearer $token")
            header("Accept", "application/vnd.github+json")
            header("X-GitHub-Api-Version", "2022-11-28")
        }

        if (!response.status.isSuccess()) {
            println("GistStorage: Failed to list gists - ${response.status}")
            throw Exception("Failed to list gists: ${response.status}")
        }

        val gists: List<GistSummary> = response.body()
        println("GistStorage: Found ${gists.size} gists, searching for WealthMate...")
        val found = gists.find { it.description == gistDescription }
        if (found != null) {
            println("GistStorage: Found matching gist: ${found.id}")
        } else {
            println("GistStorage: No matching gist found")
        }
        return found?.id
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
     * Create a new secret Gist. Returns the new Gist ID.
     */
    private suspend fun createGist(content: String): String {
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

        val createdGist: GistResponse = response.body()
        return createdGist.id
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
