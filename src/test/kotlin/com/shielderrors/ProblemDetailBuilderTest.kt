package com.shielderrors

import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProblemDetailBuilderTest {

    @Test
    fun `should create problem detail with all fields`() {
        val problemDetail = ProblemDetailBuilder.create(
            status = HttpStatus.NOT_FOUND,
            title = "Resource not found",
            detail = "The requested resource was not found",
            code = "RESOURCE_NOT_FOUND",
            doc = "https://example.com/docs/errors",
            traceId = "test-trace-id"
        )

        assertEquals(404, problemDetail.status)
        assertEquals("Resource not found", problemDetail.title)
        assertEquals("The requested resource was not found", problemDetail.detail)
        assertEquals("RESOURCE_NOT_FOUND", problemDetail.properties["code"])
        assertEquals("test-trace-id", problemDetail.properties["traceId"])
        assertNotNull(problemDetail.properties["timestamp"])
    }

    @Test
    fun `should create generic error`() {
        val problemDetail = ProblemDetailBuilder.createGenericError()

        assertEquals(500, problemDetail.status)
        assertEquals("Internal Server Error", problemDetail.title)
        assertEquals("INTERNAL_SERVER_ERROR", problemDetail.properties["code"])
        assertNotNull(problemDetail.properties["traceId"])
    }

    @Test
    fun `should create validation error`() {
        val problemDetail = ProblemDetailBuilder.createValidationError("Name is required")

        assertEquals(400, problemDetail.status)
        assertEquals("Validation Error", problemDetail.title)
        assertEquals("Name is required", problemDetail.detail)
        assertEquals("VALIDATION_ERROR", problemDetail.properties["code"])
    }

    @Test
    fun `should create malformed request error`() {
        val problemDetail = ProblemDetailBuilder.createMalformedRequestError()

        assertEquals(400, problemDetail.status)
        assertEquals("Malformed Request", problemDetail.title)
        assertEquals("MALFORMED_REQUEST", problemDetail.properties["code"])
        assertTrue(problemDetail.detail!!.contains("malformed"))
    }

    @Test
    fun `should create data integrity error`() {
        val problemDetail = ProblemDetailBuilder.createDataIntegrityError()

        assertEquals(409, problemDetail.status)
        assertEquals("Data Conflict", problemDetail.title)
        assertEquals("DATA_INTEGRITY_VIOLATION", problemDetail.properties["code"])
        assertTrue(problemDetail.detail!!.contains("conflicts"))
    }

    @Test
    fun `should generate unique trace IDs`() {
        val problem1 = ProblemDetailBuilder.createGenericError()
        val problem2 = ProblemDetailBuilder.createGenericError()

        assertNotNull(problem1.properties["traceId"])
        assertNotNull(problem2.properties["traceId"])
        assertTrue(problem1.properties["traceId"] != problem2.properties["traceId"])
    }
}