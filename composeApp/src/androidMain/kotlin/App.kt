import android.content.Context
import androidx.compose.runtime.*
import com.waliahimanshu.wealthmate.FinanceData
import com.waliahimanshu.wealthmate.WealthMateApp
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val PREFS_NAME = "wealthmate_prefs"
private const val DATA_KEY = "finance_data"

private val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}

@Composable
fun App(context: Context) {
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    val initialData = remember {
        try {
            val stored = prefs.getString(DATA_KEY, null)
            if (stored != null) {
                json.decodeFromString<FinanceData>(stored)
            } else {
                FinanceData()
            }
        } catch (e: Exception) {
            FinanceData()
        }
    }

    WealthMateApp(
        initialData = initialData,
        onDataChanged = { data ->
            try {
                val encoded = json.encodeToString(data)
                prefs.edit().putString(DATA_KEY, encoded).apply()
            } catch (e: Exception) {
                // Log error
            }
        }
    )
}
