package com.shielderrors

import com.shielderrors.annotations.ErrorCode
import com.shielderrors.utils.ExceptionMetadataExtractor
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExceptionMetadataExtractorTest {

    @ErrorCode(
        code = "LOAN_NOT_FOUND",
        status = HttpStatus.NOT_FOUND,
        doc = "https://example.com/docs/loan-errors",
        title = "Loan Not Found"
    )
    class LoanNotFoundException(message: String) : RuntimeException(message)

    @ErrorCode(
        code = "USER_VALIDATION_ERROR",
        status = HttpStatus.BAD_REQUEST
    )
    class UserValidationException(message: String) : RuntimeException(message)

    class UnannotatedException(message: String) : RuntimeException(message)

    @Test
    fun `should extract metadata from annotated exception`() {
        val exception = LoanNotFoundException("Loan 123 not found")
        val metadata = ExceptionMetadataExtractor.extractMetadata(exception)

        assertNotNull(metadata)
        assertEquals("LOAN_NOT_FOUND", metadata.code)
        assertEquals(HttpStatus.NOT_FOUND, metadata.status)
        assertEquals("https://example.com/docs/loan-errors", metadata.doc)
        assertEquals("Loan Not Found", metadata.title)
    }

    @Test
    fun `should use exception message as title when title is empty`() {
        val exception = UserValidationException("Invalid user data")
        val metadata = ExceptionMetadataExtractor.extractMetadata(exception)

        assertNotNull(metadata)
        assertEquals("USER_VALIDATION_ERROR", metadata.code)
        assertEquals(HttpStatus.BAD_REQUEST, metadata.status)
        assertEquals("Invalid user data", metadata.title)
        assertEquals("", metadata.doc)
    }

    @Test
    fun `should return null for unannotated exception`() {
        val exception = UnannotatedException("Something went wrong")
        val metadata = ExceptionMetadataExtractor.extractMetadata(exception)

        assertNull(metadata)
    }

    @Test
    fun `should detect annotated exception`() {
        val annotatedException = LoanNotFoundException("Test")
        val unannotatedException = UnannotatedException("Test")

        assertTrue(ExceptionMetadataExtractor.hasErrorCodeAnnotation(annotatedException))
        assertTrue(!ExceptionMetadataExtractor.hasErrorCodeAnnotation(unannotatedException))
    }

    @Test
    fun `should handle exception with null message`() {
        @ErrorCode(code = "NULL_MESSAGE_ERROR")
        class NullMessageException : RuntimeException(null as String?)

        val exception = NullMessageException()
        val metadata = ExceptionMetadataExtractor.extractMetadata(exception)

        assertNotNull(metadata)
        assertEquals("An error occurred", metadata.title)
    }
}