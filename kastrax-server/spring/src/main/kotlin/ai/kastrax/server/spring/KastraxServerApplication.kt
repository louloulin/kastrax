package ai.kastrax.server.spring

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KastraxServerApplication

fun main(args: Array<String>) {
    runApplication<KastraxServerApplication>(*args)
}
