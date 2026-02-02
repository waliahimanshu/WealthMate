import android.content.Context
import androidx.compose.runtime.*
import com.waliahimanshu.wealthmate.WealthMateApp
import com.waliahimanshu.wealthmate.storage.FinanceRepository
import com.waliahimanshu.wealthmate.storage.GistStorage
import com.waliahimanshu.wealthmate.storage.LocalStorageImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun App(context: Context) {
    val localStorage = remember { LocalStorageImpl(context) }
    val scope = remember { CoroutineScope(Dispatchers.IO) }

    var currentToken by remember { mutableStateOf<String?>(null) }
    var gistStorage by remember { mutableStateOf<GistStorage?>(null) }
    var repository by remember { mutableStateOf<FinanceRepository?>(null) }

    // Load token on startup
    LaunchedEffect(Unit) {
        currentToken = localStorage.loadToken()
        if (currentToken != null) {
            gistStorage = GistStorage(currentToken!!)
        }
        repository = FinanceRepository(localStorage, gistStorage)
        repository?.initialize()
    }

    // Handle token changes
    fun saveToken(token: String) {
        scope.launch {
            localStorage.saveToken(token)
            currentToken = token
            gistStorage = GistStorage(token)
            repository = FinanceRepository(localStorage, gistStorage)
            repository?.initialize()
        }
    }

    fun clearToken() {
        scope.launch {
            localStorage.clearToken()
            currentToken = null
            gistStorage = null
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
