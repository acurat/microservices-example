package bombardier

import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.web.reactive.function.client.WebClient
import kotlin.random.Random

const val url = "http://localhost:8090"
val client: WebClient = WebClient.create(url)

fun main() = runBlocking {
    val channel = Channel<String>()
    launch { makeCall(channel, "customer", 1000L) }
    launch { makeCall(channel, "order", 500L) }
    repeat(100_000) {
        println(channel.receive())
    }
    coroutineContext.cancelChildren()
}

suspend fun makeCall(channel: SendChannel<String>, s: String, time: Long) {
    while (true) {
        delay(time)
        val resp = client.get().uri("/$s?delay=${Random.nextFloat()}")
                .retrieve()
                .bodyToMono(String::class.java)
                .doOnError{}
                .block()

        channel.send("$s created...")
    }
}