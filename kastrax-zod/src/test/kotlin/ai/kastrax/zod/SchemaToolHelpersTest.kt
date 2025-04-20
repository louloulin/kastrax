package ai.kastrax.zod

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SchemaToolHelpersTest {
    
    @Test
    fun `stringInput should create StringSchema`() {
        val schema = stringInput("A test string")
        
        assertTrue(schema is StringSchema)
        assertEquals("A test string", schema.description)
    }
    
    @Test
    fun `numberInput should create NumberSchema`() {
        val schema = numberInput("A test number")
        
        assertTrue(schema is NumberSchema)
        assertEquals("A test number", schema.description)
    }
    
    @Test
    fun `booleanInput should create BooleanSchema`() {
        val schema = booleanInput("A test boolean")
        
        assertTrue(schema is BooleanSchema)
        assertEquals("A test boolean", schema.description)
    }
    
    @Test
    fun `objectInput should create ObjectSchema`() {
        val schema = objectInput("A test object") {
            stringField("name", "Person's name")
            numberField("age", "Person's age")
            booleanField("active", "Is the person active")
        }
        
        assertTrue(schema is ObjectSchema<*, *>)
        assertEquals("A test object", schema.description)
        assertEquals(3, schema.fields.size)
        assertTrue(schema.fields["name"]?.schema is StringSchema)
        assertTrue(schema.fields["age"]?.schema is NumberSchema)
        assertTrue(schema.fields["active"]?.schema is BooleanSchema)
    }
    
    @Test
    fun `arrayInput should create ArraySchema`() {
        val schema = arrayInput(stringInput("Array element"), "A test array")
        
        assertTrue(schema is ArraySchema<*, *>)
        assertEquals("A test array", schema.description)
        assertTrue(schema.elementSchema is StringSchema)
    }
    
    @Test
    fun `tupleInput should create TupleSchema`() {
        val schema = tupleInput(
            stringInput("First element"),
            numberInput("Second element"),
            booleanInput("Third element"),
            description = "A test tuple"
        )
        
        assertTrue(schema is TupleSchema)
        assertEquals("A test tuple", schema.description)
        assertEquals(3, schema.schemas.size)
        assertTrue(schema.schemas[0] is StringSchema)
        assertTrue(schema.schemas[1] is NumberSchema)
        assertTrue(schema.schemas[2] is BooleanSchema)
    }
    
    @Test
    fun `unionInput should create UnionSchema`() {
        val schema = unionInput(
            stringInput("String option"),
            numberInput("Number option"),
            description = "A test union"
        )
        
        assertTrue(schema is UnionSchema)
        assertEquals("A test union", schema.description)
        assertEquals(2, schema.schemas.size)
        assertTrue(schema.schemas[0] is StringSchema)
        assertTrue(schema.schemas[1] is NumberSchema)
    }
    
    @Test
    fun `enumInput should create EnumSchema`() {
        enum class TestEnum { A, B, C }
        
        val schema = enumInput(TestEnum::class.java, "A test enum")
        
        assertTrue(schema is EnumSchema<*>)
        assertEquals("A test enum", schema.description)
        assertEquals(TestEnum::class.java, schema.enumClass)
    }
    
    @Test
    fun `literalInput should create LiteralSchema`() {
        val schema = literalInput("test", "A test literal")
        
        assertTrue(schema is LiteralSchema<*>)
        assertEquals("A test literal", schema.description)
        assertEquals("test", schema.value)
    }
    
    @Test
    fun `ObjectSchemaBuilder extensions should work`() {
        val schema = objectInput("A test object") {
            // Test stringField
            stringField("name", "Person's name") {
                minLength = 3
                maxLength = 50
            }
            
            // Test numberField
            numberField("age", "Person's age") {
                min = 0.0
                max = 120.0
            }
            
            // Test booleanField
            booleanField("active", "Is the person active")
            
            // Test objectField
            objectField("address", "Person's address") {
                stringField("street", "Street name")
                stringField("city", "City name")
                stringField("country", "Country name")
            }
            
            // Test arrayField
            arrayField("hobbies", stringInput(), "Person's hobbies") {
                minLength = 1
                maxLength = 5
            }
            
            // Test enumField
            enum class TestEnum { A, B, C }
            enumField("enum", TestEnum::class.java, "Test enum")
            
            // Test literalField
            literalField("literal", "test", "Test literal")
        }
        
        assertTrue(schema is ObjectSchema<*, *>)
        assertEquals(7, schema.fields.size)
        
        // Verify stringField
        val nameField = schema.fields["name"]
        assertNotNull(nameField)
        assertTrue(nameField.schema is StringSchema)
        assertEquals(3, (nameField.schema as StringSchema).minLength)
        assertEquals(50, (nameField.schema as StringSchema).maxLength)
        
        // Verify numberField
        val ageField = schema.fields["age"]
        assertNotNull(ageField)
        assertTrue(ageField.schema is NumberSchema)
        assertEquals(0.0, (ageField.schema as NumberSchema).min)
        assertEquals(120.0, (ageField.schema as NumberSchema).max)
        
        // Verify booleanField
        val activeField = schema.fields["active"]
        assertNotNull(activeField)
        assertTrue(activeField.schema is BooleanSchema)
        
        // Verify objectField
        val addressField = schema.fields["address"]
        assertNotNull(addressField)
        assertTrue(addressField.schema is ObjectSchema<*, *>)
        val addressSchema = addressField.schema as ObjectSchema<*, *>
        assertEquals(3, addressSchema.fields.size)
        assertTrue(addressSchema.fields["street"]?.schema is StringSchema)
        assertTrue(addressSchema.fields["city"]?.schema is StringSchema)
        assertTrue(addressSchema.fields["country"]?.schema is StringSchema)
        
        // Verify arrayField
        val hobbiesField = schema.fields["hobbies"]
        assertNotNull(hobbiesField)
        assertTrue(hobbiesField.schema is ArraySchema<*, *>)
        val hobbiesSchema = hobbiesField.schema as ArraySchema<*, *>
        assertEquals(1, hobbiesSchema.minLength)
        assertEquals(5, hobbiesSchema.maxLength)
        assertTrue(hobbiesSchema.elementSchema is StringSchema)
        
        // Verify enumField
        val enumField = schema.fields["enum"]
        assertNotNull(enumField)
        assertTrue(enumField.schema is EnumSchema<*>)
        
        // Verify literalField
        val literalField = schema.fields["literal"]
        assertNotNull(literalField)
        assertTrue(literalField.schema is LiteralSchema<*>)
        assertEquals("test", (literalField.schema as LiteralSchema<*>).value)
    }
}
