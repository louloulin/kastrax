package ai.kastrax.datasource.nosql

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MongoDbConnectorTest {

    companion object {
        @Container
        val mongoDBContainer = MongoDBContainer("mongo:6.0")
    }

    private lateinit var connector: MongoDbConnector
    private val testDatabaseName = "test_db"
    private val testCollectionName = "test_collection"

    @BeforeAll
    fun setup() {
        mongoDBContainer.start()
        
        val connectionString = mongoDBContainer.connectionString
        connector = MongoDbConnector(
            name = "test-mongodb",
            connectionString = connectionString,
            databaseName = testDatabaseName
        )
        
        runBlocking {
            connector.connect()
        }
    }

    @AfterAll
    fun tearDown() {
        runBlocking {
            connector.disconnect()
        }
        mongoDBContainer.stop()
    }

    @Test
    fun `test insert and find document`() = runBlocking {
        // Insert a document
        val document = """{"name": "Test User", "age": 30, "email": "test@example.com"}"""
        val id = connector.insertDocument(testCollectionName, document)
        
        // Verify the document was inserted
        assertNotNull(id)
        assertTrue(id.isNotEmpty())
        
        // Find the document
        val filter = """{"name": "Test User"}"""
        val results = connector.findDocuments(testCollectionName, filter)
        
        // Verify the document was found
        assertEquals(1, results.size)
        assertEquals("Test User", results[0]["name"])
        assertEquals(30, results[0]["age"])
        assertEquals("test@example.com", results[0]["email"])
    }

    @Test
    fun `test update document`() = runBlocking {
        // Insert a document
        val document = """{"name": "Update Test", "status": "pending"}"""
        val id = connector.insertDocument(testCollectionName, document)
        
        // Update the document
        val filter = """{"name": "Update Test"}"""
        val update = """{"${'$'}set": {"status": "completed"}}"""
        val updateCount = connector.updateDocuments(testCollectionName, filter, update)
        
        // Verify the document was updated
        assertEquals(1, updateCount)
        
        // Find the updated document
        val results = connector.findDocuments(testCollectionName, filter)
        
        // Verify the document was updated correctly
        assertEquals(1, results.size)
        assertEquals("Update Test", results[0]["name"])
        assertEquals("completed", results[0]["status"])
    }

    @Test
    fun `test delete document`() = runBlocking {
        // Insert a document
        val document = """{"name": "Delete Test", "temporary": true}"""
        val id = connector.insertDocument(testCollectionName, document)
        
        // Delete the document
        val filter = """{"name": "Delete Test"}"""
        val deleteCount = connector.deleteDocuments(testCollectionName, filter, true)
        
        // Verify the document was deleted
        assertEquals(1, deleteCount)
        
        // Try to find the deleted document
        val results = connector.findDocuments(testCollectionName, filter)
        
        // Verify the document was not found
        assertEquals(0, results.size)
    }

    @Test
    fun `test aggregate`() = runBlocking {
        // Insert multiple documents
        val documents = listOf(
            """{"category": "A", "value": 10}""",
            """{"category": "A", "value": 20}""",
            """{"category": "B", "value": 30}""",
            """{"category": "B", "value": 40}"""
        )
        
        documents.forEach { connector.insertDocument(testCollectionName, it) }
        
        // Perform aggregation
        val pipeline = """[
            {"${'$'}match": {"category": {"${'$'}in": ["A", "B"]}}},
            {"${'$'}group": {"_id": "${'$'}category", "total": {"${'$'}sum": "${'$'}value"}}}
        ]"""
        
        val results = connector.aggregate(testCollectionName, pipeline)
        
        // Verify the aggregation results
        assertEquals(2, results.size)
        
        // Find category A result
        val categoryAResult = results.find { it["_id"] == "A" }
        assertNotNull(categoryAResult)
        assertEquals(30.0, categoryAResult["total"])
        
        // Find category B result
        val categoryBResult = results.find { it["_id"] == "B" }
        assertNotNull(categoryBResult)
        assertEquals(70.0, categoryBResult["total"])
    }

    @Test
    fun `test create and drop index`() = runBlocking {
        // Create an index
        val keys = """{"name": 1}"""
        val indexName = connector.createIndex(testCollectionName, keys)
        
        // Verify the index was created
        assertNotNull(indexName)
        assertTrue(indexName.isNotEmpty())
        
        // Get indexes
        val indexes = connector.getIndexes(testCollectionName)
        
        // Verify the index exists
        assertTrue(indexes.any { it["name"] == "name_1" })
        
        // Drop the index
        val dropped = connector.dropIndex(testCollectionName, "name_1")
        
        // Verify the index was dropped
        assertTrue(dropped)
        
        // Get indexes again
        val indexesAfterDrop = connector.getIndexes(testCollectionName)
        
        // Verify the index no longer exists
        assertFalse(indexesAfterDrop.any { it["name"] == "name_1" })
    }

    @Test
    fun `test get collections and databases`() = runBlocking {
        // Get collections
        val collections = connector.getCollections()
        
        // Verify the test collection exists
        assertTrue(collections.contains(testCollectionName))
        
        // Get databases
        val databases = connector.getDatabases()
        
        // Verify the test database exists
        assertTrue(databases.contains(testDatabaseName))
    }
}
