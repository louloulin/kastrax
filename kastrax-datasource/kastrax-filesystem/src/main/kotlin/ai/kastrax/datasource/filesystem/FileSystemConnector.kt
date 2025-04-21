package ai.kastrax.datasource.filesystem

import ai.kastrax.datasource.common.DataSource
import ai.kastrax.datasource.common.DataSourceBase
import ai.kastrax.datasource.common.DataSourceType
import ai.kastrax.datasource.common.FileSystemConnector
import ai.kastrax.datasource.common.FileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes

// FileSystemConnector interface is now imported from ai.kastrax.datasource.common

/**
 * 文件系统连接器基类，提供了通用的实现。
 */
abstract class FileSystemConnectorBase(
    name: String
) : DataSourceBase(name, DataSourceType.FILESYSTEM), FileSystemConnector {

    /**
     * 根目录路径。
     */
    protected abstract val rootPath: String

    /**
     * 获取完整路径。
     *
     * @param path 相对路径。
     * @return 完整路径。
     */
    protected fun getFullPath(path: String): Path {
        return Paths.get(rootPath, path)
    }

    override suspend fun readFile(path: String): ByteArray {
        logger.debug { "Reading file: $path" }
        return withContext(Dispatchers.IO) {
            Files.readAllBytes(getFullPath(path))
        }
    }

    override suspend fun readTextFile(path: String, charset: String): String {
        logger.debug { "Reading text file: $path with charset: $charset" }
        return withContext(Dispatchers.IO) {
            Files.readString(getFullPath(path), java.nio.charset.Charset.forName(charset))
        }
    }

    override suspend fun writeFile(path: String, content: ByteArray, overwrite: Boolean): Boolean {
        logger.debug { "Writing file: $path, overwrite: $overwrite" }
        return withContext(Dispatchers.IO) {
            val fullPath = getFullPath(path)

            if (Files.exists(fullPath) && !overwrite) {
                logger.warn { "File already exists and overwrite is false: $path" }
                return@withContext false
            }

            try {
                Files.createDirectories(fullPath.parent)
                Files.write(fullPath, content)
                true
            } catch (e: Exception) {
                logger.error(e) { "Error writing file: $path" }
                false
            }
        }
    }

    override suspend fun writeTextFile(path: String, content: String, charset: String, overwrite: Boolean): Boolean {
        logger.debug { "Writing text file: $path, charset: $charset, overwrite: $overwrite" }
        return withContext(Dispatchers.IO) {
            val fullPath = getFullPath(path)

            if (Files.exists(fullPath) && !overwrite) {
                logger.warn { "File already exists and overwrite is false: $path" }
                return@withContext false
            }

            try {
                Files.createDirectories(fullPath.parent)
                Files.writeString(fullPath, content, java.nio.charset.Charset.forName(charset))
                true
            } catch (e: Exception) {
                logger.error(e) { "Error writing text file: $path" }
                false
            }
        }
    }

    override suspend fun delete(path: String, recursive: Boolean): Boolean {
        logger.debug { "Deleting: $path, recursive: $recursive" }
        return withContext(Dispatchers.IO) {
            val fullPath = getFullPath(path)

            if (!Files.exists(fullPath)) {
                logger.warn { "Path does not exist: $path" }
                return@withContext false
            }

            try {
                if (Files.isDirectory(fullPath) && recursive) {
                    Files.walk(fullPath)
                        .sorted(Comparator.reverseOrder())
                        .forEach { Files.delete(it) }
                } else {
                    Files.delete(fullPath)
                }
                true
            } catch (e: Exception) {
                logger.error(e) { "Error deleting: $path" }
                false
            }
        }
    }

    override suspend fun createDirectory(path: String, createParents: Boolean): Boolean {
        logger.debug { "Creating directory: $path, createParents: $createParents" }
        return withContext(Dispatchers.IO) {
            val fullPath = getFullPath(path)

            try {
                if (createParents) {
                    Files.createDirectories(fullPath)
                } else {
                    Files.createDirectory(fullPath)
                }
                true
            } catch (e: Exception) {
                logger.error(e) { "Error creating directory: $path" }
                false
            }
        }
    }

    override suspend fun listDirectory(path: String): List<FileInfo> {
        logger.debug { "Listing directory: $path" }
        return withContext(Dispatchers.IO) {
            val fullPath = getFullPath(path)

            if (!Files.exists(fullPath) || !Files.isDirectory(fullPath)) {
                logger.warn { "Path does not exist or is not a directory: $path" }
                return@withContext emptyList<FileInfo>()
            }

            try {
                Files.list(fullPath)
                    .map { createFileInfo(it) }
                    .toList()
            } catch (e: Exception) {
                logger.error(e) { "Error listing directory: $path" }
                emptyList()
            }
        }
    }

    override suspend fun exists(path: String): Boolean {
        logger.debug { "Checking if exists: $path" }
        return withContext(Dispatchers.IO) {
            Files.exists(getFullPath(path))
        }
    }

    override suspend fun getInfo(path: String): FileInfo {
        logger.debug { "Getting info for: $path" }
        return withContext(Dispatchers.IO) {
            val fullPath = getFullPath(path)

            if (!Files.exists(fullPath)) {
                throw NoSuchFileException(File(path), null, "Path does not exist: $path")
            }

            createFileInfo(fullPath)
        }
    }

    override suspend fun copy(source: String, destination: String, overwrite: Boolean, recursive: Boolean): Boolean {
        logger.debug { "Copying from $source to $destination, overwrite: $overwrite, recursive: $recursive" }
        return withContext(Dispatchers.IO) {
            val sourcePath = getFullPath(source)
            val destinationPath = getFullPath(destination)

            if (!Files.exists(sourcePath)) {
                logger.warn { "Source path does not exist: $source" }
                return@withContext false
            }

            if (Files.exists(destinationPath) && !overwrite) {
                logger.warn { "Destination path already exists and overwrite is false: $destination" }
                return@withContext false
            }

            try {
                if (Files.isDirectory(sourcePath)) {
                    if (!recursive) {
                        logger.warn { "Source is a directory but recursive is false: $source" }
                        return@withContext false
                    }

                    Files.createDirectories(destinationPath)

                    Files.walk(sourcePath)
                        .forEach { source ->
                            val relativePath = sourcePath.relativize(source)
                            val target = destinationPath.resolve(relativePath)

                            if (Files.isDirectory(source)) {
                                Files.createDirectories(target)
                            } else {
                                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
                            }
                        }
                } else {
                    Files.createDirectories(destinationPath.parent)
                    Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING)
                }
                true
            } catch (e: Exception) {
                logger.error(e) { "Error copying from $source to $destination" }
                false
            }
        }
    }

    override suspend fun move(source: String, destination: String, overwrite: Boolean): Boolean {
        logger.debug { "Moving from $source to $destination, overwrite: $overwrite" }
        return withContext(Dispatchers.IO) {
            val sourcePath = getFullPath(source)
            val destinationPath = getFullPath(destination)

            if (!Files.exists(sourcePath)) {
                logger.warn { "Source path does not exist: $source" }
                return@withContext false
            }

            if (Files.exists(destinationPath) && !overwrite) {
                logger.warn { "Destination path already exists and overwrite is false: $destination" }
                return@withContext false
            }

            try {
                Files.createDirectories(destinationPath.parent)
                Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING)
                true
            } catch (e: Exception) {
                logger.error(e) { "Error moving from $source to $destination" }
                false
            }
        }
    }

    /**
     * 创建文件信息对象。
     *
     * @param path 文件路径。
     * @return 文件信息对象。
     */
    private fun createFileInfo(path: Path): FileInfo {
        val attributes = Files.readAttributes(path, BasicFileAttributes::class.java)

        return FileInfo(
            path = path.toString(),
            name = path.fileName.toString(),
            isDirectory = attributes.isDirectory,
            size = attributes.size(),
            lastModified = attributes.lastModifiedTime().toMillis(),
            creationTime = attributes.creationTime().toMillis(),
            lastAccessTime = attributes.lastAccessTime().toMillis()
        )
    }
}
