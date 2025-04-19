package ai.kastrax.zod

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SchemaObjectsTest {

    @Test
    fun `ObjectSchema should validate objects`() {
        val nameField = ObjectField("name", StringSchema(), true)
        val ageField = ObjectField("age", NumberSchema(), true)
        val fields = mapOf("name" to nameField, "age" to ageField)
        val schema = ObjectSchema<Map<String, Any?>, Map<String, Any?>>(fields)
        
        // Valid object
        val validObject = mapOf("name" to "John", "age" to 30)
        val result1 = schema.safeParse(validObject)
        assertTrue(result1 is SchemaResult.Success)
        val data1 = (result1 as SchemaResult.Success).data
        assertEquals("John", data1["name"])
        assertEquals(30.0, data1["age"])
        
        // Missing required field
        val invalidObject1 = mapOf("name" to "John")
        val result2 = schema.safeParse(invalidObject1)
        assertTrue(result2 is SchemaResult.Failure)
        val error2 = (result2 as SchemaResult.Failure).error
        assertEquals(SchemaIssueCode.REQUIRED, error2.issues[0].code)
        
        // Invalid field type
        val invalidObject2 = mapOf("name" to "John", "age" to "thirty")
        val result3 = schema.safeParse(invalidObject2)
        assertTrue(result3 is SchemaResult.Failure)
        val error3 = (result3 as SchemaResult.Failure).error
        assertEquals(SchemaIssueCode.INVALID_TYPE, error3.issues[0].code)
    }
    
    @Test
    fun `ObjectSchema should handle strict mode`() {
        val nameField = ObjectField("name", StringSchema(), true)
        val fields = mapOf("name" to nameField)
        val schema = ObjectSchema<Map<String, Any?>, Map<String, Any?>>(
            fields, 
            strict = true, 
            unknownKeys = ObjectSchema.UnknownKeysStrategy.STRICT
        )
        
        // Valid object
        val validObject = mapOf("name" to "John")
        val result1 = schema.safeParse(validObject)
        assertTrue(result1 is SchemaResult.Success)
        
        // Object with unknown field
        val invalidObject = mapOf("name" to "John", "age" to 30)
        val result2 = schema.safeParse(invalidObject)
        assertTrue(result2 is SchemaResult.Failure)
        val error = (result2 as SchemaResult.Failure).error
        assertEquals(SchemaIssueCode.CUSTOM, error.issues[0].code)
    }
    
    @Test
    fun `ObjectSchema should handle passthrough mode`() {
        val nameField = ObjectField("name", StringSchema(), true)
        val fields = mapOf("name" to nameField)
        val schema = ObjectSchema<Map<String, Any?>, Map<String, Any?>>(
            fields, 
            strict = false, 
            unknownKeys = ObjectSchema.UnknownKeysStrategy.PASSTHROUGH
        )
        
        // Object with unknown field
        val object1 = mapOf("name" to "John", "age" to 30)
        val result1 = schema.safeParse(object1)
        assertTrue(result1 is SchemaResult.Success)
        val data1 = (result1 as SchemaResult.Success).data
        assertEquals("John", data1["name"])
        assertEquals(30, data1["age"])
    }
    
    @Test
    fun `ObjectSchema should handle strip mode`() {
        val nameField = ObjectField("name", StringSchema(), true)
        val fields = mapOf("name" to nameField)
        val schema = ObjectSchema<Map<String, Any?>, Map<String, Any?>>(
            fields, 
            strict = false, 
            unknownKeys = ObjectSchema.UnknownKeysStrategy.STRIP
        )
        
        // Object with unknown field
        val object1 = mapOf("name" to "John", "age" to 30)
        val result1 = schema.safeParse(object1)
        assertTrue(result1 is SchemaResult.Success)
        val data1 = (result1 as SchemaResult.Success).data
        assertEquals("John", data1["name"])
        assertEquals(null, data1["age"])
    }
    
    @Test
    fun `ObjectSchema should handle catchall`() {
        val nameField = ObjectField("name", StringSchema(), true)
        val fields = mapOf("name" to nameField)
        val schema = ObjectSchema<Map<String, Any?>, Map<String, Any?>>(
            fields, 
            catchall = StringSchema()
        )
        
        // Valid object with catchall fields
        val object1 = mapOf("name" to "John", "city" to "New York", "country" to "USA")
        val result1 = schema.safeParse(object1)
        assertTrue(result1 is SchemaResult.Success)
        val data1 = (result1 as SchemaResult.Success).data
        assertEquals("John", data1["name"])
        assertEquals("New York", data1["city"])
        assertEquals("USA", data1["country"])
        
        // Invalid catchall field
        val object2 = mapOf("name" to "John", "age" to 30)
        val result2 = schema.safeParse(object2)
        assertTrue(result2 is SchemaResult.Failure)
        val error = (result2 as SchemaResult.Failure).error
        assertEquals(SchemaIssueCode.INVALID_TYPE, error.issues[0].code)
    }
    
    @Test
    fun `ObjectSchema should support extend`() {
        val nameField = ObjectField("name", StringSchema(), true)
        val fields1 = mapOf("name" to nameField)
        val schema1 = ObjectSchema<Map<String, Any?>, Map<String, Any?>>(fields1)
        
        val ageField = ObjectField("age", NumberSchema(), true)
        val fields2 = mapOf("age" to ageField)
        
        val extendedSchema = schema1.extend(fields2)
        
        // Valid extended object
        val object1 = mapOf("name" to "John", "age" to 30)
        val result1 = extendedSchema.safeParse(object1)
        assertTrue(result1 is SchemaResult.Success)
        val data1 = (result1 as SchemaResult.Success).data
        assertEquals("John", data1["name"])
        assertEquals(30.0, data1["age"])
    }
    
    @Test
    fun `ObjectSchema should support merge`() {
        val nameField = ObjectField("name", StringSchema(), true)
        val fields1 = mapOf("name" to nameField)
        val schema1 = ObjectSchema<Map<String, Any?>, Map<String, Any?>>(fields1)
        
        val ageField = ObjectField("age", NumberSchema(), true)
        val fields2 = mapOf("age" to ageField)
        val schema2 = ObjectSchema<Map<String, Any?>, Map<String, Any?>>(fields2)
        
        val mergedSchema = schema1.merge(schema2)
        
        // Valid merged object
        val object1 = mapOf("name" to "John", "age" to 30)
        val result1 = mergedSchema.safeParse(object1)
        assertTrue(result1 is SchemaResult.Success)
        val data1 = (result1 as SchemaResult.Success).data
        assertEquals("John", data1["name"])
        assertEquals(30.0, data1["age"])
    }
    
    @Test
    fun `ObjectSchema should support pick`() {
        val nameField = ObjectField("name", StringSchema(), true)
        val ageField = ObjectField("age", NumberSchema(), true)
        val cityField = ObjectField("city", StringSchema(), true)
        val fields = mapOf("name" to nameField, "age" to ageField, "city" to cityField)
        val schema = ObjectSchema<Map<String, Any?>, Map<String, Any?>>(fields)
        
        val pickedSchema = schema.pick(listOf("name", "age"))
        
        // Valid picked object
        val object1 = mapOf("name" to "John", "age" to 30)
        val result1 = pickedSchema.safeParse(object1)
        assertTrue(result1 is SchemaResult.Success)
        
        // Missing city is ok because it was picked out
        val data1 = (result1 as SchemaResult.Success).data
        assertEquals("John", data1["name"])
        assertEquals(30.0, data1["age"])
    }
    
    @Test
    fun `ObjectSchema should support omit`() {
        val nameField = ObjectField("name", StringSchema(), true)
        val ageField = ObjectField("age", NumberSchema(), true)
        val cityField = ObjectField("city", StringSchema(), true)
        val fields = mapOf("name" to nameField, "age" to ageField, "city" to cityField)
        val schema = ObjectSchema<Map<String, Any?>, Map<String, Any?>>(fields)
        
        val omittedSchema = schema.omit(listOf("city"))
        
        // Valid omitted object
        val object1 = mapOf("name" to "John", "age" to 30)
        val result1 = omittedSchema.safeParse(object1)
        assertTrue(result1 is SchemaResult.Success)
        
        // Missing city is ok because it was omitted
        val data1 = (result1 as SchemaResult.Success).data
        assertEquals("John", data1["name"])
        assertEquals(30.0, data1["age"])
    }
    
    @Test
    fun `ObjectSchema should support partial`() {
        val nameField = ObjectField("name", StringSchema(), true)
        val ageField = ObjectField("age", NumberSchema(), true)
        val fields = mapOf("name" to nameField, "age" to ageField)
        val schema = ObjectSchema<Map<String, Any?>, Map<String, Any?>>(fields)
        
        val partialSchema = schema.partial()
        
        // Partial object with only one field
        val object1 = mapOf("name" to "John")
        val result1 = partialSchema.safeParse(object1)
        assertTrue(result1 is SchemaResult.Success)
        val data1 = (result1 as SchemaResult.Success).data
        assertEquals("John", data1["name"])
    }
    
    @Test
    fun `ObjectSchema should support required`() {
        val nameField = ObjectField("name", StringSchema(), false)
        val ageField = ObjectField("age", NumberSchema(), false)
        val fields = mapOf("name" to nameField, "age" to ageField)
        val schema = ObjectSchema<Map<String, Any?>, Map<String, Any?>>(fields)
        
        val requiredSchema = schema.required()
        
        // Missing required field
        val object1 = mapOf("name" to "John")
        val result1 = requiredSchema.safeParse(object1)
        assertTrue(result1 is SchemaResult.Failure)
        val error = (result1 as SchemaResult.Failure).error
        assertEquals(SchemaIssueCode.REQUIRED, error.issues[0].code)
    }
}
