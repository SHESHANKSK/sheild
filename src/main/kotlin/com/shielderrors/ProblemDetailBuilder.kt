package com.shielderrors

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import java.net.URI
import java.time.Instant
import java.util.*

/**
 * Builder utility for creating RFC 7807 compliant ProblemDetail objects.
 */
object ProblemDetailBuilder {

    /**
     * Creates a ProblemDetail object with the specified parameters.
     *
     * @param status HTTP status code
     * @param title Short, human-readable summary
     * @param detail Optional technical detail (should be safe for external consumption)
     * @param code Unique business or technical error code
     * @param doc URI pointing to documentation (optional)
     * @param traceId Correlation ID for tracing (auto-generated if not provided)
     * @return ProblemDetail object ready for JSON serialization
     */
    fun create(
        status: HttpStatus,
        title: String,
        detail: String? = null,
        code: String,
        doc: String? = null,
        traceId: String = UUID.randomUUID().toString()
    ): ProblemDetail {
        val problemDetail = ProblemDetail.forStatus(status)
        
        // Set standard RFC 7807 fields
        problemDetail.title = title
        problemDetail.detail = detail
        
        // Set type URI if documentation is provided
        if (!doc.isNullOrBlank()) {
            problemDetail.type = URI.create(doc)
        }
        
        // Add custom properties
        problemDetail.setProperty("code", code)
        problemDetail.setProperty("traceId", traceId)
        problemDetail.setProperty("timestamp", Instant.now().toString())
        
        return problemDetail
    }

    /**
     * Creates a generic internal server error ProblemDetail.
     */
    fun createGenericError(traceId: String = UUID.randomUUID().toString()): ProblemDetail {
        return create(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            title = "Internal Server Error",
            detail = "An unexpected error occurred. Please contact support if the problem persists.",
            code = "INTERNAL_SERVER_ERROR",
            traceId = traceId
        )
    }

    /**
     * Creates a validation error ProblemDetail.
     */
    fun createValidationError(
        detail: String,
        traceId: String = UUID.randomUUID().toString()
    ): ProblemDetail {
        return create(
            status = HttpStatus.BAD_REQUEST,
            title = "Validation Error",
            detail = detail,
            code = "VALIDATION_ERROR",
            traceId = traceId
        )
    }

    /**
     * Creates a malformed request ProblemDetail.
     */
    fun createMalformedRequestError(traceId: String = UUID.randomUUID().toString()): ProblemDetail {
        return create(
            status = HttpStatus.BAD_REQUEST,
            title = "Malformed Request",
            detail = "The request body is malformed or contains invalid JSON.",
            code = "MALFORMED_REQUEST",
            traceId = traceId
        )
    }

    /**
     * Creates a data integrity violation ProblemDetail.
     */
    fun createDataIntegrityError(traceId: String = UUID.randomUUID().toString()): ProblemDetail {
        return create(
            status = HttpStatus.CONFLICT,
            title = "Data Conflict",
            detail = "The operation conflicts with existing data constraints.",
            code = "DATA_INTEGRITY_VIOLATION",
            traceId = traceId
        )
    }
}