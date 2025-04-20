package ai.kastrax.fastembed

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AsyncTextEmbeddingTest {

    @Test
    fun testEmbedSingleText() = runTest {
        AsyncTextEmbedding.create(EmbeddingModel.BGE_SMALL_EN).use { model ->
            val text = "Hello, world!"
            val embedding = model.embed(text)

            // Check that the embedding has the expected dimension
            assertEquals(model.dimension, embedding.dimension)

            // Check that the embedding is not all zeros
            assertTrue(embedding.vector.any { it != 0f })
        }
    }

    @Test
    fun testEmbedMultipleTexts() = runTest {
        AsyncTextEmbedding.create(EmbeddingModel.BGE_SMALL_EN).use { model ->
            val texts = listOf(
                "Hello, world!",
                "This is a test",
                "FastEmbed is awesome"
            )

            val embeddings = model.embed(texts)

            // Check that we got the expected number of embeddings
            assertEquals(texts.size, embeddings.size)

            // Check that each embedding has the expected dimension
            embeddings.forEach { embedding ->
                assertEquals(model.dimension, embedding.dimension)
                assertTrue(embedding.vector.any { it != 0f })
            }
        }
    }

    @Test
    fun testEmbedParallel() = runTest {
        AsyncTextEmbedding.create(EmbeddingModel.BGE_SMALL_EN).use { model ->
            val texts = List(10) { index -> "This is text number $index" }

            val embeddings = model.embedParallel(texts, chunkSize = 3)

            // Check that we got the expected number of embeddings
            assertEquals(texts.size, embeddings.size)

            // Check that each embedding has the expected dimension
            embeddings.forEach { embedding ->
                assertEquals(model.dimension, embedding.dimension)
                assertTrue(embedding.vector.any { it != 0f })
            }
        }
    }

    @Test
    fun testSimilarity() = runTest {
        AsyncTextEmbedding.create(EmbeddingModel.BGE_SMALL_EN).use { model ->
            val text1 = "Hello, world!"
            val text2 = "Hi, world!"
            val text3 = "This is completely different."

            val similarity1 = model.similarity(text1, text2)
            val similarity2 = model.similarity(text1, text3)

            // Similar texts should have higher similarity
            assertTrue(similarity1 > similarity2)

            // Similarity should be between -1 and 1
            assertTrue(similarity1 in -1f..1f)
            assertTrue(similarity2 in -1f..1f)
        }
    }

    @Test
    fun testFindSimilar() = runTest {
        AsyncTextEmbedding.create(EmbeddingModel.BGE_SMALL_EN).use { model ->
            val query = "What is artificial intelligence?"
            val candidates = listOf(
                "AI is the simulation of human intelligence by machines.",
                "Machine learning is a subset of artificial intelligence.",
                "The weather is nice today.",
                "Artificial intelligence involves creating systems that can perform tasks requiring human intelligence."
            )

            // Find the top 2 most similar texts
            val results = model.findSimilar(query, candidates, 2)

            // Check that we got the expected number of results
            assertEquals(2, results.size)

            // The most similar texts should be about AI
            assertTrue(candidates[results[0].first].contains("intelligence", ignoreCase = true) ||
                     candidates[results[0].first].contains("AI", ignoreCase = true))

            // The weather text should not be in the top 2
            assertFalse(candidates[results[0].first].contains("weather", ignoreCase = true))
            assertFalse(candidates[results[1].first].contains("weather", ignoreCase = true))

            // Similarity scores should be between -1 and 1
            assertTrue(results[0].second in -1f..1f)
            assertTrue(results[1].second in -1f..1f)

            // First result should have higher similarity than second
            assertTrue(results[0].second >= results[1].second)
        }
    }
}
