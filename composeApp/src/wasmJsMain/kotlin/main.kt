import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.waliahimanshu.wealthmate.FinanceData
import com.waliahimanshu.wealthmate.WealthMateApp
import kotlinx.browser.localStorage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val STORAGE_KEY = "wealthmate_data"

private val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}

fun loadData(): FinanceData {
    return try {
        val stored = localStorage.getItem(STORAGE_KEY)
        if (stored != null) {
            json.decodeFromString<FinanceData>(stored)
        } else {
            FinanceData()
        }
    } catch (e: Exception) {
        console.log("Failed to load data: ${e.message}")
        FinanceData()
    }
}

fun saveData(data: FinanceData) {
    try {
        val encoded = json.encodeToString(data)
        localStorage.setItem(STORAGE_KEY, encoded)
    } catch (e: Exception) {
        console.log("Failed to save data: ${e.message}")
    }
}

external object console {
    fun log(message: String)
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val initialData = loadData()

    CanvasBasedWindow(canvasElementId = "ComposeTarget", title = "WealthMate") {
        WealthMateApp(
            initialData = initialData,
            onDataChanged = { data ->
                saveData(data)
            }
        )
    }
}
