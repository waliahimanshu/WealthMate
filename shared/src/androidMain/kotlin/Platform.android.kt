import android.os.Build
import java.util.Random
import kotlin.random.asKotlinRandom

class AndroidPlatform : Platform {
    val random = Random().nextInt()
    override val name: String = "Android ${Build.VERSION.SDK_INT}"

}

actual fun getPlatform(): Platform = AndroidPlatform()
actual val num: Int = 1