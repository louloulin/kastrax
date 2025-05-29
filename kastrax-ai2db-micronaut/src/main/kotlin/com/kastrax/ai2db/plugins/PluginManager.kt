package com.kastrax.ai2db.plugins

import com.kastrax.ai2db.connection.model.DatabaseType
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Property
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy

/**
 * Plugin configuration properties
 */
@ConfigurationProperties("kastrax.ai2db.plugins")
data class PluginConfig(
    val directory: String = "./plugins",
    val autoDiscovery: Boolean = true,
    val scanPackages: List<String> = listOf("com.kastrax.ai2db.plugins")
)

/**
 * Plugin manager for database connector plugins
 * 
 * Uses ServiceLoader mechanism to discover and load plugins dynamically,
 * inspired by Kestra's plugin architecture.
 */
@Singleton
class PluginManager(
    private val pluginConfig: PluginConfig
) {
    private val logger = LoggerFactory.getLogger(PluginManager::class.java)
    private val loadedPlugins = ConcurrentHashMap<String, DatabaseConnectorPlugin>()
    private val typeToPluginMap = ConcurrentHashMap<DatabaseType, DatabaseConnectorPlugin>()
    
    @PostConstruct
    fun initialize() {
        logger.info("Initializing Plugin Manager with config: $pluginConfig")
        if (pluginConfig.autoDiscovery) {
            discoverAndLoadPlugins()
        }
    }
    
    @PreDestroy
    fun cleanup() {
        logger.info("Cleaning up Plugin Manager")
        loadedPlugins.values.forEach { plugin ->
            try {
                plugin.cleanup()
            } catch (e: Exception) {
                logger.warn("Error cleaning up plugin ${plugin.getName()}", e)
            }
        }
        loadedPlugins.clear()
        typeToPluginMap.clear()
    }
    
    /**
     * Discover and load all available plugins using ServiceLoader
     */
    private fun discoverAndLoadPlugins() {
        logger.info("Discovering database connector plugins...")
        
        try {
            val serviceLoader = ServiceLoader.load(DatabaseConnectorPlugin::class.java)
            var pluginCount = 0
            
            for (plugin in serviceLoader) {
                try {
                    loadPlugin(plugin)
                    pluginCount++
                } catch (e: Exception) {
                    logger.error("Failed to load plugin: ${plugin::class.java.name}", e)
                }
            }
            
            logger.info("Successfully loaded $pluginCount database connector plugins")
            logLoadedPlugins()
            
        } catch (e: Exception) {
            logger.error("Error during plugin discovery", e)
        }
    }
    
    /**
     * Load a specific plugin
     */
    private fun loadPlugin(plugin: DatabaseConnectorPlugin) {
        val pluginName = plugin.getName()
        
        if (loadedPlugins.containsKey(pluginName)) {
            logger.warn("Plugin $pluginName is already loaded, skipping")
            return
        }
        
        logger.info("Loading plugin: $pluginName v${plugin.getVersion()}")
        
        // Initialize the plugin
        plugin.initialize()
        
        // Register the plugin
        loadedPlugins[pluginName] = plugin
        
        // Map supported types to this plugin
        plugin.getSupportedTypes().forEach { type ->
            if (typeToPluginMap.containsKey(type)) {
                logger.warn("Database type $type is already supported by plugin ${typeToPluginMap[type]?.getName()}, overriding with $pluginName")
            }
            typeToPluginMap[type] = plugin
        }
        
        logger.info("Successfully loaded plugin $pluginName supporting types: ${plugin.getSupportedTypes()}")
    }
    
    /**
     * Get a plugin by name
     */
    fun getPlugin(name: String): DatabaseConnectorPlugin? {
        return loadedPlugins[name]
    }
    
    /**
     * Get all loaded plugins
     */
    fun getAllPlugins(): Collection<DatabaseConnectorPlugin> {
        return loadedPlugins.values
    }
    
    /**
     * Get plugin that supports the specified database type
     */
    fun getPluginForType(type: DatabaseType): DatabaseConnectorPlugin? {
        return typeToPluginMap[type]
    }
    
    /**
     * Check if a database type is supported by any loaded plugin
     */
    fun isTypeSupported(type: DatabaseType): Boolean {
        return typeToPluginMap.containsKey(type)
    }
    
    /**
     * Get all supported database types
     */
    fun getSupportedTypes(): Set<DatabaseType> {
        return typeToPluginMap.keys.toSet()
    }
    
    /**
     * Manually register a plugin (for testing or programmatic registration)
     */
    fun registerPlugin(plugin: DatabaseConnectorPlugin) {
        loadPlugin(plugin)
    }
    
    /**
     * Unregister a plugin
     */
    fun unregisterPlugin(pluginName: String): Boolean {
        val plugin = loadedPlugins.remove(pluginName)
        if (plugin != null) {
            // Remove type mappings
            plugin.getSupportedTypes().forEach { type ->
                typeToPluginMap.remove(type)
            }
            
            // Cleanup plugin
            try {
                plugin.cleanup()
            } catch (e: Exception) {
                logger.warn("Error cleaning up plugin $pluginName", e)
            }
            
            logger.info("Unregistered plugin: $pluginName")
            return true
        }
        return false
    }
    
    /**
     * Reload all plugins
     */
    fun reloadPlugins() {
        logger.info("Reloading all plugins...")
        cleanup()
        initialize()
    }
    
    /**
     * Log information about loaded plugins
     */
    private fun logLoadedPlugins() {
        if (loadedPlugins.isEmpty()) {
            logger.warn("No database connector plugins loaded")
            return
        }
        
        logger.info("Loaded plugins summary:")
        loadedPlugins.values.forEach { plugin ->
            logger.info("  - ${plugin.getName()} v${plugin.getVersion()}: ${plugin.getSupportedTypes()}")
        }
    }
    
    /**
     * Get plugin statistics
     */
    fun getPluginStats(): Map<String, Any> {
        return mapOf(
            "totalPlugins" to loadedPlugins.size,
            "supportedTypes" to getSupportedTypes().size,
            "plugins" to loadedPlugins.values.map { plugin ->
                mapOf(
                    "name" to plugin.getName(),
                    "version" to plugin.getVersion(),
                    "supportedTypes" to plugin.getSupportedTypes(),
                    "description" to plugin.getDescription(),
                    "author" to plugin.getAuthor()
                )
            }
        )
    }
}