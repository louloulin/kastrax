package actor.proto.benchmarks

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter

/**
 * Tool to compare benchmark results between ProtoActor-Kotlin and ProtoActor-Go.
 */
object ComparisonTool {
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size < 2) {
            println("Usage: ComparisonTool <kotlin-results.json> <go-results.json> [output.html]")
            return
        }
        
        val kotlinResultsFile = args[0]
        val goResultsFile = args[1]
        val outputFile = if (args.size > 2) args[2] else "comparison-results.html"
        
        val kotlinResults = parseResults(kotlinResultsFile)
        val goResults = parseResults(goResultsFile)
        
        generateComparisonReport(kotlinResults, goResults, outputFile)
        
        println("Comparison report generated at $outputFile")
    }
    
    private fun parseResults(filePath: String): Map<String, Double> {
        val mapper = ObjectMapper()
        val rootNode = mapper.readTree(File(filePath))
        
        val results = mutableMapOf<String, Double>()
        
        if (rootNode.has("benchmarks")) {
            val benchmarks = rootNode["benchmarks"]
            
            for (benchmark in benchmarks) {
                val name = benchmark["benchmark"].asText()
                val score = benchmark["primaryMetric"]["score"].asDouble()
                
                results[name] = score
            }
        }
        
        return results
    }
    
    private fun generateComparisonReport(
        kotlinResults: Map<String, Double>,
        goResults: Map<String, Double>,
        outputFile: String
    ) {
        val writer = PrintWriter(FileWriter(outputFile))
        
        writer.println("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>ProtoActor Performance Comparison</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 20px; }
                    h1 { color: #333; }
                    table { border-collapse: collapse; width: 100%; }
                    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
                    th { background-color: #f2f2f2; }
                    tr:nth-child(even) { background-color: #f9f9f9; }
                    .better { color: green; font-weight: bold; }
                    .worse { color: red; }
                    .chart-container { width: 800px; height: 400px; margin: 20px 0; }
                </style>
                <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
            </head>
            <body>
                <h1>ProtoActor Performance Comparison</h1>
                <p>Comparison between ProtoActor-Kotlin and ProtoActor-Go benchmark results.</p>
                
                <h2>Results Table</h2>
                <table>
                    <tr>
                        <th>Benchmark</th>
                        <th>Kotlin (ops/sec)</th>
                        <th>Go (ops/sec)</th>
                        <th>Difference</th>
                        <th>Ratio (Kotlin/Go)</th>
                    </tr>
        """.trimIndent())
        
        // Combine all benchmark names
        val allBenchmarks = (kotlinResults.keys + goResults.keys).toSet().sorted()
        
        for (benchmark in allBenchmarks) {
            val kotlinScore = kotlinResults[benchmark] ?: 0.0
            val goScore = goResults[benchmark] ?: 0.0
            
            val difference = kotlinScore - goScore
            val ratio = if (goScore > 0) kotlinScore / goScore else 0.0
            
            val differenceClass = when {
                difference > 0 -> "better"
                difference < 0 -> "worse"
                else -> ""
            }
            
            writer.println("""
                <tr>
                    <td>${benchmark}</td>
                    <td>${String.format("%.2f", kotlinScore)}</td>
                    <td>${String.format("%.2f", goScore)}</td>
                    <td class="${differenceClass}">${String.format("%+.2f", difference)}</td>
                    <td>${String.format("%.2f", ratio)}</td>
                </tr>
            """.trimIndent())
        }
        
        writer.println("</table>")
        
        // Add chart
        writer.println("""
            <h2>Comparison Chart</h2>
            <div class="chart-container">
                <canvas id="comparisonChart"></canvas>
            </div>
            
            <script>
                const ctx = document.getElementById('comparisonChart').getContext('2d');
                const chart = new Chart(ctx, {
                    type: 'bar',
                    data: {
                        labels: [${allBenchmarks.joinToString(", ") { "'$it'" }}],
                        datasets: [
                            {
                                label: 'Kotlin',
                                data: [${allBenchmarks.joinToString(", ") { (kotlinResults[it] ?: 0.0).toString() }}],
                                backgroundColor: 'rgba(54, 162, 235, 0.5)',
                                borderColor: 'rgba(54, 162, 235, 1)',
                                borderWidth: 1
                            },
                            {
                                label: 'Go',
                                data: [${allBenchmarks.joinToString(", ") { (goResults[it] ?: 0.0).toString() }}],
                                backgroundColor: 'rgba(255, 99, 132, 0.5)',
                                borderColor: 'rgba(255, 99, 132, 1)',
                                borderWidth: 1
                            }
                        ]
                    },
                    options: {
                        responsive: true,
                        scales: {
                            y: {
                                beginAtZero: true,
                                title: {
                                    display: true,
                                    text: 'Operations per second'
                                }
                            }
                        }
                    }
                });
            </script>
            </body>
            </html>
        """.trimIndent())
        
        writer.close()
    }
}
