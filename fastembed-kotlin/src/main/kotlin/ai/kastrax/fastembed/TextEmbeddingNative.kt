package ai.kastrax.fastembed

/**
 * Native interface for the fastembed-rs library.
 * This class contains the JNI methods that directly interact with the Rust code.
 * Not intended to be used directly - use the [TextEmbedding] class instead.
 */
internal object TextEmbeddingNative {
    init {
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
    }

    private fun loadNativeLibrary() {
        val osName = System.getProperty("os.name").toLowerCase()
        val osArch = System.getProperty("os.arch").toLowerCase()

        val libraryName = when {
            osName.contains("win") -> "fastembed_jni.dll"
            osName.contains("mac") || osName.contains("darwin") -> "libfastembed_jni.dylib"
            else -> "libfastembed_jni.so"
        }

        val resourcePath = "/native/${osName.replace(" ", "-")}-$osArch/$libraryName"

        // First try to load from the system library path
        try {
            System.loadLibrary("fastembed_jni")
            return
        } catch (e: UnsatisfiedLinkError) {
            // If that fails, try to extract from resources
            val tempFile = extractResourceToTempFile(resourcePath, libraryName)
            System.load(tempFile.absolutePath)
        }
    }

    private fun extractResourceToTempFile(resourcePath: String, fileName: String): java.io.File {
        val inputStream = TextEmbeddingNative::class.java.getResourceAsStream(resourcePath)
            ?: throw RuntimeException("Could not find native library at $resourcePath")

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
    external fun initLogger()

    /**
     * Create a new text embedding model.
     *
     * @param modelType The model type (see [EmbeddingModel])
     * @param cacheDir The cache directory for model files (null for default)
     * @param showDownloadProgress Whether to show download progress
     * @return A model ID that can be used to reference this model in other calls
     */
    external fun createModel(modelType: Int, cacheDir: String?, showDownloadProgress: Boolean): Long

    /**
     * Generate embeddings for multiple texts.
     *
     * @param modelId The model ID returned by [createModel]
     * @param texts The texts to embed
     * @param batchSize The batch size (0 for default)
     * @return An array of float arrays, each representing an embedding
     */
    external fun embedTexts(modelId: Long, texts: Array<String>, batchSize: Int): Array<FloatArray>

    /**
     * Generate an embedding for a single text.
     *
     * @param modelId The model ID returned by [createModel]
     * @param text The text to embed
     * @return A float array representing the embedding
     */
    external fun embedText(modelId: Long, text: String): FloatArray

    /**
     * Release a model and free its resources.
     *
     * @param modelId The model ID returned by [createModel]
     * @return true if the model was found and released, false otherwise
     */
    external fun releaseModel(modelId: Long): Boolean

    /**
     * Get the dimension of the embeddings produced by a model.
     *
     * @param modelId The model ID returned by [createModel]
     * @return The dimension of the embeddings
     */
    external fun getEmbeddingDimension(modelId: Long): Int

    /**
     * Calculate the cosine similarity between two embeddings.
     *
     * @param embedding1 The first embedding
     * @param embedding2 The second embedding
     * @return The cosine similarity (between -1 and 1)
     */
    external fun cosineSimilarity(embedding1: FloatArray, embedding2: FloatArray): Float
}
