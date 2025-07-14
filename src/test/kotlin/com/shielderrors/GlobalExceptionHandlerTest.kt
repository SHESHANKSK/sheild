package com.shielderrors

import com.shielderrors.annotations.ErrorCode
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @ErrorCode(
        code = "USER_NOT_FOUND",
        status = HttpStatus.NOT_FOUND,
        title = "User Not Found",
        doc = "https://example.com/docs/user-errors"
    )
    class UserNotFoundException(message: String) : RuntimeException(message)

    @ErrorCode(
        code = "BUSINESS_RULE_VIOLATION",
        status = HttpStatus.UNPROCESSABLE_ENTITY
    )
    class BusinessRuleException(message: String) : RuntimeException(message)

    @Test
    fun `should handle annotated exception with full metadata`() {
        val exception = UserNotFoundException("User with ID 123 not found")
        val response = handler.handleAnnotatedException(exception)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals("application/problem+json", response.headers.getFirst("Content-Type"))
        
        val problemDetail = response.body as ProblemDetail
        assertEquals("User Not Found", problemDetail.title)
        assertEquals("User with ID 123 not found", problemDetail.detail)
        assertEquals("USER_NOT_FOUND", problemDetail.properties["code"])
        assertEquals("https://example.com/docs/user-errors", problemDetail.type.toString())
        assertNotNull(problemDetail.properties["traceId"])
        assertNotNull(problemDetail.properties["timestamp"])
    }

    @Test
    fun `should handle annotated exception with minimal metadata`() {
        val exception = BusinessRuleException("Business rule violated")
        val response = handler.handleAnnotatedException(exception)

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.statusCode)
        
        val problemDetail = response.body as ProblemDetail
        assertEquals("Business rule violated", problemDetail.title)
        assertEquals("BUSINESS_RULE_VIOLATION", problemDetail.properties["code"])
    }

    @Test
    fun `should handle generic exception`() {
        val exception = RuntimeException("Something went wrong")
        val response = handler.handleAnnotatedException(exception)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        
        val problemDetail = response.body as ProblemDetail
        assertEquals("Internal Server Error", problemDetail.title)
        assertEquals("INTERNAL_SERVER_ERROR", problemDetail.properties["code"])
        assertTrue(problemDetail.detail!!.contains("unexpected error"))
    }

    @Test
    fun `should handle validation exception`() {
        val bindingResult = BeanPropertyBindingResult(Any(), "testObject")
        bindingResult.addError(FieldError("testObject", "name", "Name is required"))
        bindingResult.addError(FieldError("testObject", "email", "Email is invalid"))
        
        val exception = MethodArgumentNotValidException(null, bindingResult)
        val response = handler.handleValidationException(exception)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        
        val problemDetail = response.body as ProblemDetail
        assertEquals("Validation Error", problemDetail.title)
        assertEquals("VALIDATION_ERROR", problemDetail.properties["code"])
        assertTrue(problemDetail.detail!!.contains("name: Name is required"))
        assertTrue(problemDetail.detail!!.contains("email: Email is invalid"))
    }

    @Test
    fun `should handle malformed request exception`() {
        val exception = HttpMessageNotReadableException("Invalid JSON")
        val response = handler.handleMalformedRequestException(exception)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        
        val problemDetail = response.body as ProblemDetail
        assertEquals("Malformed Request", problemDetail.title)
        assertEquals("MALFORMED_REQUEST", problemDetail.properties["code"])
        assertTrue(problemDetail.detail!!.contains("malformed"))
    }

    @Test
    fun `should handle data integrity violation exception`() {
        val exception = DataIntegrityViolationException("Constraint violation")
        val response = handler.handleDataIntegrityViolationException(exception)

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        
        val problemDetail = response.body as ProblemDetail
        assertEquals("Data Conflict", problemDetail.title)
        assertEquals("DATA_INTEGRITY_VIOLATION", problemDetail.properties["code"])
        assertTrue(problemDetail.detail!!.contains("conflicts"))
    }

    @Test
    fun `should sanitize sensitive information in exception messages`() {
        @ErrorCode(code = "SENSITIVE_ERROR")
        class SensitiveException(message: String) : RuntimeException(message)

        val exception = SensitiveException("Error with password=secret123 and token=abc456")
        val response = handler.handleAnnotatedException(exception)

        val problemDetail = response.body as ProblemDetail
        val detail = problemDetail.detail!!
        assertTrue(detail.contains("password=[REDACTED]"))
        assertTrue(detail.contains("token=[REDACTED]"))
        assertTrue(!detail.contains("secret123"))
        assertTrue(!detail.contains("abc456"))
    }
}