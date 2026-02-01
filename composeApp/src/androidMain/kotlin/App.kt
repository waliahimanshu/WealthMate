import android.content.Context
import androidx.compose.runtime.*
import com.waliahimanshu.wealthmate.WealthMateApp
import com.waliahimanshu.wealthmate.storage.FinanceRepository
import com.waliahimanshu.wealthmate.storage.LocalStorageImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun App(context: Context) {
    val localStorage = remember { LocalStorageImpl(context) }
    val repository = remember { FinanceRepository(localStorage, null) }

    LaunchedEffect(Unit) {
        repository.initialize()
    }

    WealthMateApp(repository = repository)
}
