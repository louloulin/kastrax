package actor.proto.persistence.serialization

import com.google.protobuf.ByteString
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * Serializer is responsible for serializing and deserializing objects.
 */
interface Serializer {
    /**
     * Serialize an object to bytes.
     * @param obj The object to serialize.
     * @return The serialized bytes and the type of the object.
     */
    fun serialize(obj: Any): Pair<ByteString, String>
    
    /**
     * Deserialize bytes to an object.
     * @param bytes The bytes to deserialize.
     * @param type The type of the object.
     * @return The deserialized object.
     */
    fun deserialize(bytes: ByteString, type: String): Any
}

/**
 * JavaSerializer is a serializer that uses Java serialization.
 */
class JavaSerializer : Serializer {
    override fun serialize(obj: Any): Pair<ByteString, String> {
        val baos = ByteArrayOutputStream()
        ObjectOutputStream(baos).use { oos ->
            oos.writeObject(obj)
        }
        return ByteString.copyFrom(baos.toByteArray()) to obj.javaClass.name
    }
    
    override fun deserialize(bytes: ByteString, type: String): Any {
        val bais = ByteArrayInputStream(bytes.toByteArray())
        return ObjectInputStream(bais).use { ois ->
            ois.readObject()
        }
    }
}

/**
 * ProtobufSerializer is a serializer that uses Protobuf serialization.
 * It delegates to JavaSerializer for non-Protobuf objects.
 */
class ProtobufSerializer : Serializer {
    private val javaSerializer = JavaSerializer()
    
    override fun serialize(obj: Any): Pair<ByteString, String> {
        // If the object is a Protobuf message, use Protobuf serialization
        if (obj is com.google.protobuf.MessageLite) {
            return obj.toByteString() to obj.javaClass.name
        }
        
        // Otherwise, use Java serialization
        return javaSerializer.serialize(obj)
    }
    
    override fun deserialize(bytes: ByteString, type: String): Any {
        try {
            // Try to load the class
            val clazz = Class.forName(type)
            
            // If the class is a Protobuf message, use Protobuf deserialization
            if (com.google.protobuf.MessageLite::class.java.isAssignableFrom(clazz)) {
                val method = clazz.getMethod("parseFrom", ByteString::class.java)
                return method.invoke(null, bytes)
            }
        } catch (e: Exception) {
            // Ignore and fall back to Java deserialization
        }
        
        // Otherwise, use Java deserialization
        return javaSerializer.deserialize(bytes, type)
    }
}
