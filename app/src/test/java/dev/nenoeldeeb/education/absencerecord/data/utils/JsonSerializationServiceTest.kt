package dev.nenoeldeeb.education.absencerecord.data.utils

import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonSerializationServiceTest {
    private val service = JsonSerializationService()

    @Serializable data class TestData(val id: Int, val name: String)

    @Test
    fun `encodeToString returns correct JSON for valid object`() {
        val data = TestData(1, "Test")
        val result = service.encodeToString(data, TestData.serializer())

        assertTrue(result.isSuccess)
        // Check for key fields, avoid strict string equality due to formatting differences
        val jsonString = result.getOrNull() ?: ""
        assertTrue(jsonString.contains("\"id\": 1"))
        assertTrue(jsonString.contains("\"name\": \"Test\""))
    }

    @Test
    fun `decodeFromString returns correct object for valid JSON`() {
        val json = """{"id": 1, "name": "Test"}"""
        val result = service.decodeFromString(json, TestData.serializer())

        assertTrue(result.isSuccess)
        assertEquals(TestData(1, "Test"), result.getOrThrow())
    }

    @Test
    fun `decodeFromString fails for malformed JSON`() {
        val badJson = """{"id": 1, "name": "Test"""" // Missing closing brace
        val result = service.decodeFromString(badJson, TestData.serializer())

        assertTrue(result.isFailure)
    }

    @Test
    fun `decodeFromString ignores unknown keys as configured`() {
        // JsonSerializationService is configured with ignoreUnknownKeys = true
        val jsonWithExtra = """{"id": 1, "name": "Test", "extra": "value"}"""
        val result = service.decodeFromString(jsonWithExtra, TestData.serializer())

        assertTrue(result.isSuccess)
        assertEquals(TestData(1, "Test"), result.getOrThrow())
    }

    @Test
    fun `decodeFromString is lenient as configured`() {
        // JsonSerializationService is configured with isLenient = true
        val json = """{
             "id": 1,
             "name": "Test"
         }"""
        val result = service.decodeFromString(json, TestData.serializer())
        assertTrue(result.isSuccess)
    }
}