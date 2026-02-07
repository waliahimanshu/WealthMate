package com.waliahimanshu.wealthmate.storage

import com.waliahimanshu.wealthmate.HouseholdFinances
import kotlinx.browser.localStorage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LocalStorageImpl : LocalStorage {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    companion object {
        private const val DATA_KEY = "wealthmate_household_data"
        private const val TOKEN_KEY = "wealthmate_github_token"
        private const val GIST_ID_KEY = "wealthmate_gist_id"
    }

    override suspend fun loadData(): HouseholdFinances? {
        return try {
            val stored = localStorage.getItem(DATA_KEY)
            if (stored != null) {
                json.decodeFromString<HouseholdFinances>(stored)
            } else {
                null
            }
        } catch (e: Exception) {
            console.log("Failed to load data: ${e.message}")
            null
        }
    }

    override suspend fun saveData(data: HouseholdFinances) {
        try {
            val encoded = json.encodeToString(data)
            localStorage.setItem(DATA_KEY, encoded)
        } catch (e: Exception) {
            console.log("Failed to save data: ${e.message}")
        }
    }

    override suspend fun loadToken(): String? {
        return localStorage.getItem(TOKEN_KEY)
    }

    override suspend fun saveToken(token: String) {
        localStorage.setItem(TOKEN_KEY, token)
    }

    override suspend fun clearToken() {
        localStorage.removeItem(TOKEN_KEY)
    }

    override suspend fun loadGistId(): String? {
        return localStorage.getItem(GIST_ID_KEY)
    }

    override suspend fun saveGistId(gistId: String) {
        localStorage.setItem(GIST_ID_KEY, gistId)
    }

    override suspend fun clearGistId() {
        localStorage.removeItem(GIST_ID_KEY)
    }
}

private external object console {
    fun log(message: String)
}
