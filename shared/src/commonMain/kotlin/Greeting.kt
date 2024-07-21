import kotlin.random.Random

class Greeting {
    private val platform = getPlatform()

    fun greet() = buildList {
        add(if (Random.nextBoolean()) "Hi!" else "Hello!")
        add("${num} guess what this is ! > ${platform.name.reversed()}")
    }
}