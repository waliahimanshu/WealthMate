package com.waliahimanshu.wealthmate.storage

import com.waliahimanshu.wealthmate.HouseholdFinances
import com.waliahimanshu.wealthmate.currentTimeMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository that manages finance data with local cache + cloud sync.
 *
 * Flow:
 * 1. Load from local storage first (fast)
 * 2. Then sync with Gist in background
 * 3. Save to both local and Gist on changes
 */
class FinanceRepository(
    private val localStorage: LocalStorage,
    private val gistStorage: GistStorage?
) {
    private val _data = MutableStateFlow<HouseholdFinances?>(null)
    val data: StateFlow<HouseholdFinances?> = _data.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Initialize repository - load local data, then sync with cloud.
     */
    suspend fun initialize() {
        _isLoading.value = true

        // 1. Load from local storage first (fast startup)
        val localData = localStorage.loadData()
        if (localData != null) {
            _data.value = localData
        }

        // 2. If Gist is configured, sync with cloud
        if (gistStorage != null) {
            syncWithCloud()
        }

        _isLoading.value = false
    }

    /**
     * Sync local data with cloud storage.
     */
    suspend fun syncWithCloud() {
        if (gistStorage == null) {
            _syncStatus.value = SyncStatus.NotConfigured
            return
        }

        _syncStatus.value = SyncStatus.Syncing
        println("Repository: Starting cloud sync...")

        try {
            val cloudResult = gistStorage.loadData()

            cloudResult.onSuccess { cloudData ->
                val localData = _data.value
                println("Repository: Cloud data - investments: ${cloudData?.investments?.size ?: 0}")
                println("Repository: Local data - investments: ${localData?.investments?.size ?: 0}")

                when {
                    // No cloud data - upload local
                    cloudData == null && localData != null -> {
                        println("Repository: Uploading local to cloud...")
                        gistStorage.saveData(localData)
                        _syncStatus.value = SyncStatus.Success("Uploaded to cloud ✓")
                    }
                    // No local data - download cloud
                    cloudData != null && localData == null -> {
                        println("Repository: Downloading from cloud...")
                        _data.value = cloudData
                        localStorage.saveData(cloudData)
                        _syncStatus.value = SyncStatus.Success("Downloaded from cloud ✓")
                    }
                    // Both exist - use most recent
                    cloudData != null && localData != null -> {
                        println("Repository: Comparing timestamps - cloud: ${cloudData.updatedAt}, local: ${localData.updatedAt}")
                        if (cloudData.updatedAt > localData.updatedAt) {
                            println("Repository: Cloud is newer, downloading...")
                            _data.value = cloudData
                            localStorage.saveData(cloudData)
                            _syncStatus.value = SyncStatus.Success("Updated from cloud ✓")
                        } else if (localData.updatedAt > cloudData.updatedAt) {
                            println("Repository: Local is newer, uploading...")
                            gistStorage.saveData(localData)
                            _syncStatus.value = SyncStatus.Success("Uploaded to cloud ✓")
                        } else {
                            println("Repository: Already in sync")
                            _syncStatus.value = SyncStatus.Success("Already in sync ✓")
                        }
                    }
                    // Both empty
                    else -> {
                        println("Repository: No data to sync")
                        _syncStatus.value = SyncStatus.Success("No data to sync")
                    }
                }
            }

            cloudResult.onFailure { error ->
                println("Repository: Sync failed - ${error.message}")
                _syncStatus.value = SyncStatus.Error(error.message ?: "Sync failed")
            }
        } catch (e: Exception) {
            println("Repository: Sync exception - ${e.message}")
            _syncStatus.value = SyncStatus.Error(e.message ?: "Sync failed")
        }
    }

    /**
     * Update data - saves to local and syncs to cloud.
     * Now properly awaits cloud sync before showing success.
     */
    suspend fun updateData(transform: (HouseholdFinances) -> HouseholdFinances) {
        val current = _data.value ?: createDefaultHousehold()
        val updated = transform(current).copy(updatedAt = currentTimeMillis())

        _data.value = updated
        localStorage.saveData(updated)
        println("Repository: Saved locally - investments: ${updated.investments.size}, members: ${updated.members.size}")

        // Sync to cloud and wait for result
        if (gistStorage != null) {
            _syncStatus.value = SyncStatus.Syncing
            println("Repository: Starting cloud sync...")
            try {
                val result = gistStorage.saveData(updated)
                result.onSuccess {
                    println("Repository: Cloud sync SUCCESS")
                    _syncStatus.value = SyncStatus.Success("Saved to cloud ✓")
                }
                result.onFailure { e ->
                    println("Repository: Cloud sync FAILED - ${e.message}")
                    _syncStatus.value = SyncStatus.Error("Cloud failed: ${e.message}")
                }
            } catch (e: Exception) {
                println("Repository: Cloud sync EXCEPTION - ${e.message}")
                _syncStatus.value = SyncStatus.Error("Sync error: ${e.message}")
            }
        }
    }

    /**
     * Set data directly (for initial setup).
     */
    suspend fun setData(data: HouseholdFinances) {
        val updated = data.copy(updatedAt = currentTimeMillis())
        _data.value = updated
        localStorage.saveData(updated)

        if (gistStorage != null) {
            _syncStatus.value = SyncStatus.Syncing
            val result = gistStorage.saveData(updated)
            result.onSuccess {
                _syncStatus.value = SyncStatus.Success("Saved to cloud")
            }
            result.onFailure { e ->
                _syncStatus.value = SyncStatus.Error("Cloud sync failed: ${e.message}")
            }
        }
    }

    /**
     * Force refresh from cloud.
     */
    suspend fun forceRefreshFromCloud() {
        if (gistStorage == null) return

        _syncStatus.value = SyncStatus.Syncing

        try {
            val result = gistStorage.loadData()
            result.onSuccess { cloudData ->
                if (cloudData != null) {
                    _data.value = cloudData
                    localStorage.saveData(cloudData)
                    _syncStatus.value = SyncStatus.Success("Refreshed from cloud")
                }
            }
            result.onFailure {
                _syncStatus.value = SyncStatus.Error(it.message ?: "Refresh failed")
            }
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.Error(e.message ?: "Refresh failed")
        }
    }

    private fun createDefaultHousehold(): HouseholdFinances {
        return HouseholdFinances(
            name = "Our Household"
        )
    }
}

sealed class SyncStatus {
    data object Idle : SyncStatus()
    data object Syncing : SyncStatus()
    data object NotConfigured : SyncStatus()
    data class Success(val message: String) : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

/**
 * Platform-specific local storage interface.
 */
interface LocalStorage {
    suspend fun loadData(): HouseholdFinances?
    suspend fun saveData(data: HouseholdFinances)
    suspend fun loadToken(): String?
    suspend fun saveToken(token: String)
    suspend fun clearToken()
    suspend fun loadGistId(): String?
    suspend fun saveGistId(gistId: String)
    suspend fun clearGistId()
}
