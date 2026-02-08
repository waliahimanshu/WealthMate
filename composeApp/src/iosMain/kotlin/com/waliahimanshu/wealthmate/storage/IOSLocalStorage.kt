package com.waliahimanshu.wealthmate.storage

import com.waliahimanshu.wealthmate.HouseholdFinances
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

/**
 * iOS implementation of LocalStorage using NSUserDefaults
 */
class IOSLocalStorage : LocalStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    companion object {
        private const val DATA_KEY = "wealthmate_data"
        private const val TOKEN_KEY = "github_token"
        private const val GIST_ID_KEY = "gist_id"
    }

    override suspend fun saveData(data: HouseholdFinances) {
        try {
            val encoded = json.encodeToString(data)
            defaults.setObject(encoded, DATA_KEY)
            defaults.synchronize()
        } catch (e: Exception) {
            println("Failed to save data: ${e.message}")
        }
    }

    override suspend fun loadData(): HouseholdFinances? {
        return try {
            val stored = defaults.stringForKey(DATA_KEY)
            if (stored != null) {
                json.decodeFromString<HouseholdFinances>(stored)
            } else {
                null
            }
        } catch (e: Exception) {
            println("Failed to load data: ${e.message}")
            null
        }
    }

    override suspend fun saveToken(token: String) {
        defaults.setObject(token, TOKEN_KEY)
        defaults.synchronize()
    }

    override suspend fun loadToken(): String? {
        return defaults.stringForKey(TOKEN_KEY)
    }

    override suspend fun clearToken() {
        defaults.removeObjectForKey(TOKEN_KEY)
        defaults.synchronize()
    }

    override suspend fun saveGistId(gistId: String) {
        defaults.setObject(gistId, GIST_ID_KEY)
        defaults.synchronize()
    }

    override suspend fun loadGistId(): String? {
        return defaults.stringForKey(GIST_ID_KEY)
    }

    override suspend fun clearGistId() {
        defaults.removeObjectForKey(GIST_ID_KEY)
        defaults.synchronize()
    }
}
