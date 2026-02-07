import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.waliahimanshu.wealthmate.WealthMateApp
import com.waliahimanshu.wealthmate.storage.FinanceRepository
import com.waliahimanshu.wealthmate.storage.GistStorage
import com.waliahimanshu.wealthmate.storage.LocalStorageImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val localStorage = LocalStorageImpl()
    val scope = CoroutineScope(Dispatchers.Default)

    CanvasBasedWindow(canvasElementId = "ComposeTarget", title = "WealthMate") {
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
                localStorage.clearGistId() // Clear cached Gist ID when token is removed
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
