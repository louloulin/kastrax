package actor.proto.benchmarks

import org.openjdk.jmh.results.format.ResultFormatType
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.OptionsBuilder
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Runner for all benchmarks.
 */
object BenchmarkRunner {
    @JvmStatic
    fun main(args: Array<String>) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        val resultsFile = File("benchmark-results-$timestamp.json")
        
        val options = OptionsBuilder()
            .include(".*Benchmark.*")
            .resultFormat(ResultFormatType.JSON)
            .result(resultsFile.absolutePath)
            .build()
        
        Runner(options).run()
        
        println("Benchmark results saved to ${resultsFile.absolutePath}")
    }
}
