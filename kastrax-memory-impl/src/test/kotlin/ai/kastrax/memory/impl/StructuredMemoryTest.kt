package ai.kastrax.memory.impl

import ai.kastrax.memory.api.RelatedData
import ai.kastrax.memory.api.RelationDirection
import ai.kastrax.memory.api.StructuredData
import ai.kastrax.memory.api.StructuredMemory
import ai.kastrax.memory.api.StructuredMemoryConfig
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull

class StructuredMemoryTest {
    
    private lateinit var structuredMemory: StructuredMemory
    
    @BeforeEach
    fun setup() {
        structuredMemory = InMemoryStructuredMemory(
            StructuredMemoryConfig(
                enableRelations = true,
                defaultNamespace = "test",
                maxPropertiesPerData = 50,
                maxRelationsPerData = 100
            )
        )
    }
    
    @Test
    fun `test save and get data`() = runBlocking {
        // 创建结构化数据
        val data = StructuredData(
            id = "person1",
            type = "person",
            properties = mapOf(
                "name" to "张三",
                "age" to 30,
                "email" to "zhangsan@example.com"
            ),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        // 保存数据
        val id = structuredMemory.saveData(data, "test")
        assertEquals("person1", id)
        
        // 获取数据
        val retrievedData = structuredMemory.getData("person1", "test")
        assertNotNull(retrievedData)
        assertEquals("person", retrievedData.type)
        assertEquals("张三", retrievedData.properties["name"])
        assertEquals(30, retrievedData.properties["age"])
    }
    
    @Test
    fun `test get data by type`() = runBlocking {
        // 创建多个不同类型的数据
        val person1 = StructuredData(
            id = "person1",
            type = "person",
            properties = mapOf("name" to "张三"),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val person2 = StructuredData(
            id = "person2",
            type = "person",
            properties = mapOf("name" to "李四"),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val company = StructuredData(
            id = "company1",
            type = "company",
            properties = mapOf("name" to "示例公司"),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        // 保存数据
        structuredMemory.saveData(person1, "test")
        structuredMemory.saveData(person2, "test")
        structuredMemory.saveData(company, "test")
        
        // 按类型获取数据
        val persons = structuredMemory.getDataByType("person", "test")
        assertEquals(2, persons.size)
        
        val companies = structuredMemory.getDataByType("company", "test")
        assertEquals(1, companies.size)
        assertEquals("示例公司", companies[0].properties["name"])
    }
    
    @Test
    fun `test query data`() = runBlocking {
        // 创建多个具有不同属性的数据
        val person1 = StructuredData(
            id = "person1",
            type = "person",
            properties = mapOf(
                "name" to "张三",
                "age" to 30,
                "city" to "北京"
            ),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val person2 = StructuredData(
            id = "person2",
            type = "person",
            properties = mapOf(
                "name" to "李四",
                "age" to 25,
                "city" to "上海"
            ),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val person3 = StructuredData(
            id = "person3",
            type = "person",
            properties = mapOf(
                "name" to "王五",
                "age" to 30,
                "city" to "广州"
            ),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        // 保存数据
        structuredMemory.saveData(person1, "test")
        structuredMemory.saveData(person2, "test")
        structuredMemory.saveData(person3, "test")
        
        // 查询数据
        val age30 = structuredMemory.queryData(mapOf("age" to 30), "test")
        assertEquals(2, age30.size)
        
        val beijingPeople = structuredMemory.queryData(mapOf("city" to "北京"), "test")
        assertEquals(1, beijingPeople.size)
        assertEquals("张三", beijingPeople[0].properties["name"])
    }
    
    @Test
    fun `test update data`() = runBlocking {
        // 创建数据
        val person = StructuredData(
            id = "person1",
            type = "person",
            properties = mapOf(
                "name" to "张三",
                "age" to 30,
                "city" to "北京"
            ),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        // 保存数据
        structuredMemory.saveData(person, "test")
        
        // 更新数据
        val updateSuccess = structuredMemory.updateData(
            "person1",
            mapOf(
                "age" to 31,
                "city" to "上海",
                "phone" to "12345678901"
            ),
            "test"
        )
        
        assertTrue(updateSuccess)
        
        // 获取更新后的数据
        val updatedPerson = structuredMemory.getData("person1", "test")
        assertNotNull(updatedPerson)
        assertEquals("张三", updatedPerson.properties["name"])
        assertEquals(31, updatedPerson.properties["age"])
        assertEquals("上海", updatedPerson.properties["city"])
        assertEquals("12345678901", updatedPerson.properties["phone"])
    }
    
    @Test
    fun `test delete data`() = runBlocking {
        // 创建数据
        val person = StructuredData(
            id = "person1",
            type = "person",
            properties = mapOf("name" to "张三"),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        // 保存数据
        structuredMemory.saveData(person, "test")
        
        // 验证数据已保存
        val savedPerson = structuredMemory.getData("person1", "test")
        assertNotNull(savedPerson)
        
        // 删除数据
        val deleteSuccess = structuredMemory.deleteData("person1", "test")
        assertTrue(deleteSuccess)
        
        // 验证数据已删除
        val deletedPerson = structuredMemory.getData("person1", "test")
        assertNull(deletedPerson)
    }
    
    @Test
    fun `test list data`() = runBlocking {
        // 创建多个数据
        for (i in 1..20) {
            val data = StructuredData(
                id = "item$i",
                type = "item",
                properties = mapOf("value" to i),
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )
            structuredMemory.saveData(data, "test")
        }
        
        // 列出数据（分页）
        val page1 = structuredMemory.listData("test", 10, 0)
        assertEquals(10, page1.size)
        
        val page2 = structuredMemory.listData("test", 10, 10)
        assertEquals(10, page2.size)
        
        // 验证分页数据不重复
        val allIds = page1.map { it.id } + page2.map { it.id }
        assertEquals(20, allIds.toSet().size)
    }
    
    @Test
    fun `test get data types`() = runBlocking {
        // 创建不同类型的数据
        val person = StructuredData(
            id = "person1",
            type = "person",
            properties = mapOf("name" to "张三"),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val company = StructuredData(
            id = "company1",
            type = "company",
            properties = mapOf("name" to "示例公司"),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val product = StructuredData(
            id = "product1",
            type = "product",
            properties = mapOf("name" to "示例产品"),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        // 保存数据
        structuredMemory.saveData(person, "test")
        structuredMemory.saveData(company, "test")
        structuredMemory.saveData(product, "test")
        
        // 获取数据类型
        val types = structuredMemory.getDataTypes("test")
        assertEquals(3, types.size)
        assertTrue(types.contains("person"))
        assertTrue(types.contains("company"))
        assertTrue(types.contains("product"))
    }
    
    @Test
    fun `test create and get relations`() = runBlocking {
        // 创建数据
        val person = StructuredData(
            id = "person1",
            type = "person",
            properties = mapOf("name" to "张三"),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val company = StructuredData(
            id = "company1",
            type = "company",
            properties = mapOf("name" to "示例公司"),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        // 保存数据
        structuredMemory.saveData(person, "test")
        structuredMemory.saveData(company, "test")
        
        // 创建关系
        val relationId = structuredMemory.createRelation(
            sourceId = "person1",
            targetId = "company1",
            relationType = "WORKS_AT",
            properties = mapOf(
                "position" to "工程师",
                "startDate" to "2020-01-01"
            ),
            namespace = "test"
        )
        
        // 获取相关数据（从人到公司）
        val personToCompany = structuredMemory.getRelatedData(
            id = "person1",
            relationType = "WORKS_AT",
            direction = RelationDirection.OUT,
            namespace = "test"
        )
        
        assertEquals(1, personToCompany.size)
        assertEquals("company1", personToCompany[0].data.id)
        assertEquals("示例公司", personToCompany[0].data.properties["name"])
        assertEquals("工程师", personToCompany[0].relation.properties["position"])
        assertEquals(RelationDirection.OUT, personToCompany[0].direction)
        
        // 获取相关数据（从公司到人）
        val companyToPerson = structuredMemory.getRelatedData(
            id = "company1",
            relationType = "WORKS_AT",
            direction = RelationDirection.IN,
            namespace = "test"
        )
        
        assertEquals(1, companyToPerson.size)
        assertEquals("person1", companyToPerson[0].data.id)
        assertEquals("张三", companyToPerson[0].data.properties["name"])
        assertEquals(RelationDirection.IN, companyToPerson[0].direction)
    }
    
    @Test
    fun `test delete relation`() = runBlocking {
        // 创建数据
        val person = StructuredData(
            id = "person1",
            type = "person",
            properties = mapOf("name" to "张三"),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val company = StructuredData(
            id = "company1",
            type = "company",
            properties = mapOf("name" to "示例公司"),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        // 保存数据
        structuredMemory.saveData(person, "test")
        structuredMemory.saveData(company, "test")
        
        // 创建关系
        val relationId = structuredMemory.createRelation(
            sourceId = "person1",
            targetId = "company1",
            relationType = "WORKS_AT",
            namespace = "test"
        )
        
        // 验证关系已创建
        val relations = structuredMemory.getRelatedData("person1", namespace = "test")
        assertEquals(1, relations.size)
        
        // 删除关系
        val deleteSuccess = structuredMemory.deleteRelation(relationId, "test")
        assertTrue(deleteSuccess)
        
        // 验证关系已删除
        val relationsAfterDelete = structuredMemory.getRelatedData("person1", namespace = "test")
        assertEquals(0, relationsAfterDelete.size)
    }
    
    @Test
    fun `test delete data with relations`() = runBlocking {
        // 创建数据
        val person = StructuredData(
            id = "person1",
            type = "person",
            properties = mapOf("name" to "张三"),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val company = StructuredData(
            id = "company1",
            type = "company",
            properties = mapOf("name" to "示例公司"),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        // 保存数据
        structuredMemory.saveData(person, "test")
        structuredMemory.saveData(company, "test")
        
        // 创建关系
        structuredMemory.createRelation(
            sourceId = "person1",
            targetId = "company1",
            relationType = "WORKS_AT",
            namespace = "test"
        )
        
        // 删除数据
        val deleteSuccess = structuredMemory.deleteData("person1", "test")
        assertTrue(deleteSuccess)
        
        // 验证关系也被删除
        val relationsAfterDelete = structuredMemory.getRelatedData("company1", namespace = "test")
        assertEquals(0, relationsAfterDelete.size)
    }
    
    @Test
    fun `test multiple namespaces`() = runBlocking {
        // 在不同命名空间创建数据
        val data1 = StructuredData(
            id = "item1",
            type = "item",
            properties = mapOf("value" to "namespace1"),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val data2 = StructuredData(
            id = "item1", // 相同ID，不同命名空间
            type = "item",
            properties = mapOf("value" to "namespace2"),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        // 保存数据
        structuredMemory.saveData(data1, "namespace1")
        structuredMemory.saveData(data2, "namespace2")
        
        // 获取数据
        val item1 = structuredMemory.getData("item1", "namespace1")
        assertNotNull(item1)
        assertEquals("namespace1", item1.properties["value"])
        
        val item2 = structuredMemory.getData("item1", "namespace2")
        assertNotNull(item2)
        assertEquals("namespace2", item2.properties["value"])
    }
}
