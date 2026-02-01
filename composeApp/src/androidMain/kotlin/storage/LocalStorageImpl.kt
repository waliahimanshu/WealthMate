package com.waliahimanshu.wealthmate.storage

import android.content.Context
import com.waliahimanshu.wealthmate.HouseholdFinances
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LocalStorageImpl(private val context: Context) : LocalStorage {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        private const val PREFS_NAME = "wealthmate_prefs"
        private const val DATA_KEY = "wealthmate_household_data"
        private const val TOKEN_KEY = "wealthmate_github_token"
    }

    override suspend fun loadData(): HouseholdFinances? {
        return try {
            val stored = prefs.getString(DATA_KEY, null)
            if (stored != null) {
                json.decodeFromString<HouseholdFinances>(stored)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun saveData(data: HouseholdFinances) {
        try {
            val encoded = json.encodeToString(data)
            prefs.edit().putString(DATA_KEY, encoded).apply()
        } catch (e: Exception) {
            // Log error
        }
    }

    override suspend fun loadToken(): String? {
        return prefs.getString(TOKEN_KEY, null)
    }

    override suspend fun saveToken(token: String) {
        prefs.edit().putString(TOKEN_KEY, token).apply()
    }

    override suspend fun clearToken() {
        prefs.edit().remove(TOKEN_KEY).apply()
    }
}
