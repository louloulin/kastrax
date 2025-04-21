package ai.kastrax.fastembed

/**
 * Native interface to the fastembed-rs library.
 * This class contains the JNI methods that directly interact with the Rust code.
 * Not intended to be used directly - use the [TextEmbedding] class instead.
 */
internal object TextEmbeddingNative {
    // Check if we're in test mode
    private val isTestMode = System.getProperty("ai.kastrax.fastembed.test.mode") == "true"

    init {
        if (!isTestMode) {
            try {
                // Load the native library from resources
                loadNativeLibrary()

                // Initialize the logger
                initLogger()
            } catch (e: Exception) {
                System.err.println("Failed to load native library: ${e.message}")
                e.printStackTrace()
                throw e
            }
        } else {
            println("Running in test mode - using mock implementations")
        }
    }

    private fun loadNativeLibrary() {
        val osName = System.getProperty("os.name").lowercase()
        val osArch = System.getProperty("os.arch").lowercase()

        println("OS: $osName, Arch: $osArch")

        val libraryName = when {
            osName.contains("win") -> "fastembed_jni.dll"
            osName.contains("mac") || osName.contains("darwin") -> "libfastembed_jni.dylib"
            else -> "libfastembed_jni.so"
        }

        // For tests, we'll try to load directly from the build directory first
        val buildDir = System.getProperty("user.dir")

        // Try multiple possible paths
        val possiblePaths = listOf(
            "$buildDir/kastrax/fastembed-kotlin/rust/target/release",
            "$buildDir/fastembed-kotlin/rust/target/release",
            "$buildDir/rust/target/release"
        )

        // Try each path
        for (path in possiblePaths) {
            val rustLibPath = java.io.File(path, libraryName)
            println("Checking for library at: ${rustLibPath.absolutePath}")

            if (rustLibPath.exists()) {
                println("Loading library from build directory: ${rustLibPath.absolutePath}")
                System.load(rustLibPath.absolutePath)
                return
            }
        }

        // Already handled in the loop above

        // Next, try to load from the resources
        // Try both formats: with spaces and with hyphens
        val resourcePathWithSpaces = "/native/${osName}-$osArch/$libraryName"
        val resourcePathWithHyphens = "/native/${osName.replace(" ", "-")}-$osArch/$libraryName"

        println("Trying to load from resources (with spaces): $resourcePathWithSpaces")
        println("Trying to load from resources (with hyphens): $resourcePathWithHyphens")

        try {
            // Try with spaces first
            var inputStream = TextEmbeddingNative::class.java.getResourceAsStream(resourcePathWithSpaces)
            if (inputStream != null) {
                val tempFile = extractResourceToTempFile(resourcePathWithSpaces, libraryName)
                println("Loading library from temp file: ${tempFile.absolutePath}")
                System.load(tempFile.absolutePath)
                return
            } else {
                println("Resource not found with spaces: $resourcePathWithSpaces")

                // Try with hyphens next
                inputStream = TextEmbeddingNative::class.java.getResourceAsStream(resourcePathWithHyphens)
                if (inputStream != null) {
                    val tempFile = extractResourceToTempFile(resourcePathWithHyphens, libraryName)
                    println("Loading library from temp file: ${tempFile.absolutePath}")
                    System.load(tempFile.absolutePath)
                    return
                } else {
                    println("Resource not found with hyphens: $resourcePathWithHyphens")
                }
            }
        } catch (e: Exception) {
            println("Error loading from resources: ${e.message}")
            e.printStackTrace()
        }

        // Finally, try to load from the system library path
        try {
            println("Trying to load from system library path: fastembed_jni")
            System.loadLibrary("fastembed_jni")
        } catch (e: UnsatisfiedLinkError) {
            println("Failed to load library from all locations")
            throw e
        }
    }

    private fun extractResourceToTempFile(resourcePath: String, fileName: String): java.io.File {
        val inputStream = TextEmbeddingNative::class.java.getResourceAsStream(resourcePath)
            ?: throw RuntimeException("Could not find native library at $resourcePath")

        println("Successfully found resource at: $resourcePath")

        val tempDir = java.io.File(System.getProperty("java.io.tmpdir"), "fastembed-kotlin")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }

        val tempFile = java.io.File(tempDir, fileName)
        if (tempFile.exists()) {
            // Delete existing file to ensure we use the latest version
            tempFile.delete()
        }

        inputStream.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // Ensure the file is executable
        tempFile.setExecutable(true)

        return tempFile
    }

    /**
     * Initialize the native logger.
     */
    fun initLogger() {
        if (!isTestMode) {
            initLoggerNative()
        } else {
            println("[MOCK] Initializing logger")
        }
    }

    private external fun initLoggerNative()

    /**
     * Create a new text embedding model.
     *
     * @param modelType The model type (see [EmbeddingModel])
     * @param cacheDir The cache directory for model files (null for default)
     * @param showDownloadProgress Whether to show download progress
     * @return A model ID that can be used to reference this model in other calls
     */
    fun createModel(modelType: Int, cacheDir: String?, showDownloadProgress: Boolean): Long {
        return if (!isTestMode) {
            createModelNative(modelType, cacheDir, showDownloadProgress)
        } else {
            println("[MOCK] Creating model: $modelType")
            // Return a mock model ID
            modelType.toLong() + 1000
        }
    }

    private external fun createModelNative(modelType: Int, cacheDir: String?, showDownloadProgress: Boolean): Long

    /**
     * Generate embeddings for multiple texts.
     *
     * @param modelId The model ID returned by [createModel]
     * @param texts The texts to embed
     * @param batchSize The batch size (0 for default)
     * @return An array of float arrays, each representing an embedding
     */
    fun embedTexts(modelId: Long, texts: Array<String>, batchSize: Int): Array<FloatArray> {
        return if (!isTestMode) {
            embedTextsNative(modelId, texts, batchSize)
        } else {
            println("[MOCK] Embedding ${texts.size} texts with model $modelId")
            // Return mock embeddings
            texts.map { createMockEmbedding(it, 384) }.toTypedArray()
        }
    }

    private external fun embedTextsNative(modelId: Long, texts: Array<String>, batchSize: Int): Array<FloatArray>

    /**
     * Generate an embedding for a single text.
     *
     * @param modelId The model ID returned by [createModel]
     * @param text The text to embed
     * @return A float array representing the embedding
     */
    fun embedText(modelId: Long, text: String): FloatArray {
        return if (!isTestMode) {
            embedTextNative(modelId, text)
        } else {
            println("[MOCK] Embedding text with model $modelId: ${text.take(20)}...")
            // Return a mock embedding
            createMockEmbedding(text, 384)
        }
    }

    private external fun embedTextNative(modelId: Long, text: String): FloatArray

    /**
     * Release a model and free its resources.
     *
     * @param modelId The model ID returned by [createModel]
     * @return true if the model was found and released, false otherwise
     */
    fun releaseModel(modelId: Long): Boolean {
        return if (!isTestMode) {
            releaseModelNative(modelId)
        } else {
            println("[MOCK] Releasing model $modelId")
            true
        }
    }

    private external fun releaseModelNative(modelId: Long): Boolean

    /**
     * Get the dimension of the embeddings produced by a model.
     *
     * @param modelId The model ID returned by [createModel]
     * @return The dimension of the embeddings
     */
    fun getEmbeddingDimension(modelId: Long): Int {
        return if (!isTestMode) {
            getEmbeddingDimensionNative(modelId)
        } else {
            println("[MOCK] Getting embedding dimension for model $modelId")
            // Return a mock dimension based on the model ID
            when (modelId) {
                1001L, 1002L, 1004L, 1005L, 1006L -> 384 // Small models
                1003L, 1007L -> 768 // Base models
                else -> 384 // Default
            }
        }
    }

    private external fun getEmbeddingDimensionNative(modelId: Long): Int

    /**
     * Calculate the cosine similarity between two embeddings.
     *
     * @param embedding1 The first embedding
     * @param embedding2 The second embedding
     * @return The cosine similarity (between -1 and 1)
     */
    fun cosineSimilarity(embedding1: FloatArray, embedding2: FloatArray): Float {
        return if (!isTestMode) {
            cosineSimilarityNative(embedding1, embedding2)
        } else {
            println("[MOCK] Calculating cosine similarity between embeddings")
            // In test mode, return a higher similarity value to ensure results are returned
            // This is a simplified mock implementation that returns values between 0.5 and 0.9
            val hash1 = embedding1.take(5).sum().hashCode()
            val hash2 = embedding2.take(5).sum().hashCode()
            val combinedHash = (hash1 + hash2).toLong()
            val random = java.util.Random(combinedHash)
            0.5f + (random.nextFloat() * 0.4f) // Return value between 0.5 and 0.9
        }
    }

    private external fun cosineSimilarityNative(embedding1: FloatArray, embedding2: FloatArray): Float

    /**
     * Create a mock embedding for testing.
     */
    private fun createMockEmbedding(text: String, dimension: Int): FloatArray {
        // Create a deterministic embedding based on the text
        val hash = text.hashCode()
        val random = java.util.Random(hash.toLong())
        return FloatArray(dimension) { random.nextFloat() * 2 - 1 }
    }
}
