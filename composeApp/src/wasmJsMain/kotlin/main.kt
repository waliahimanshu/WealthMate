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

    // For now, start without Gist storage (user can configure later)
    // In future: load token and create GistStorage if available
    val repository = FinanceRepository(localStorage, null)

    // Initialize repository
    scope.launch {
        repository.initialize()
    }

    CanvasBasedWindow(canvasElementId = "ComposeTarget", title = "WealthMate") {
        WealthMateApp(repository = repository)
    }
}
