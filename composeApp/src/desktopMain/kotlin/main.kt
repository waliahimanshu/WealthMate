import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import com.waliahimanshu.wealthmate.HouseholdFinances
import com.waliahimanshu.wealthmate.WealthMateApp
import com.waliahimanshu.wealthmate.storage.FinanceRepository
import com.waliahimanshu.wealthmate.storage.GistStorage
import com.waliahimanshu.wealthmate.storage.LocalStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.prefs.Preferences

fun main() = application {
    val localStorage = DesktopLocalStorage()
    val scope = CoroutineScope(Dispatchers.Default)

    Window(
        onCloseRequest = ::exitApplication,
        title = "WealthMate",
        state = rememberWindowState(width = 1200.dp, height = 800.dp)
    ) {
        var currentToken by remember { mutableStateOf<String?>(null) }
        var gistStorage by remember { mutableStateOf<GistStorage?>(null) }
        var repository by remember { mutableStateOf<FinanceRepository?>(null) }

        // Load token on startup
        LaunchedEffect(Unit) {
            currentToken = localStorage.loadToken()
            if (currentToken != null) {
                gistStorage = GistStorage(currentToken!!, localStorage)
            }
            repository = FinanceRepository(localStorage, gistStorage)
            repository?.initialize()
        }

        // Handle token changes
        fun saveToken(token: String) {
            scope.launch {
                localStorage.saveToken(token)
                currentToken = token
                gistStorage = GistStorage(token, localStorage)
                // Recreate repository with new Gist storage
                repository = FinanceRepository(localStorage, gistStorage)
                repository?.initialize()
            }
        }

        fun clearToken() {
            scope.launch {
                localStorage.clearToken()
                localStorage.clearGistId()
                currentToken = null
                gistStorage = null
                // Recreate repository without Gist storage
                repository = FinanceRepository(localStorage, null)
                repository?.initialize()
            }
        }

        repository?.let { repo ->
            WealthMateApp(
                repository = repo,
                currentToken = currentToken,
                onSaveToken = ::saveToken,
                onClearToken = ::clearToken
            )
        }
    }
}

/**
 * Desktop implementation of LocalStorage using Java Preferences API and file system
 */
class DesktopLocalStorage : LocalStorage {
    private val prefs = Preferences.userNodeForPackage(DesktopLocalStorage::class.java)
    private val dataDir = File(System.getProperty("user.home"), ".wealthmate").also { it.mkdirs() }
    private val dataFile = File(dataDir, "data.json")

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    companion object {
        private const val TOKEN_KEY = "github_token"
        private const val GIST_ID_KEY = "gist_id"
    }

    override suspend fun saveData(data: HouseholdFinances) {
        try {
            val encoded = json.encodeToString(data)
            dataFile.writeText(encoded)
        } catch (e: Exception) {
            println("Failed to save data: ${e.message}")
        }
    }

    override suspend fun loadData(): HouseholdFinances? {
        return try {
            if (dataFile.exists()) {
                val stored = dataFile.readText()
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
        prefs.put(TOKEN_KEY, token)
        prefs.flush()
    }

    override suspend fun loadToken(): String? {
        return prefs.get(TOKEN_KEY, null)
    }

    override suspend fun clearToken() {
        prefs.remove(TOKEN_KEY)
        prefs.flush()
    }

    override suspend fun saveGistId(gistId: String) {
        prefs.put(GIST_ID_KEY, gistId)
        prefs.flush()
    }

    override suspend fun loadGistId(): String? {
        return prefs.get(GIST_ID_KEY, null)
    }

    override suspend fun clearGistId() {
        prefs.remove(GIST_ID_KEY)
        prefs.flush()
    }
}
