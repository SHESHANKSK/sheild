package com.shielderrors

import com.shielderrors.utils.ExceptionMetadataExtractor
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.util.*

/**
 * Global exception handler that intercepts all unhandled exceptions
 * and converts them to RFC 7807 compliant ProblemDetail responses.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * Handles annotated domain exceptions using the @ErrorCode annotation.
     */
    @ExceptionHandler(Exception::class)
    fun handleAnnotatedException(ex: Exception): ResponseEntity<ProblemDetail> {
        val traceId = UUID.randomUUID().toString()
        
        // Check if the exception has the ErrorCode annotation
        val metadata = ExceptionMetadataExtractor.extractMetadata(ex)
        
        return if (metadata != null) {
            logger.warn("Annotated exception occurred [traceId={}]: {}", traceId, ex.message, ex)
            
            val problemDetail = ProblemDetailBuilder.create(
                status = metadata.status,
                title = metadata.title,
                detail = sanitizeDetail(ex.message),
                code = metadata.code,
                doc = if (metadata.doc.isNotBlank()) metadata.doc else null,
                traceId = traceId
            )
            
            ResponseEntity.status(metadata.status)
                .header("Content-Type", "application/problem+json")
                .body(problemDetail)
        } else {
            // Handle as generic exception
            handleGenericException(ex, traceId)
        }
    }

    /**
     * Handles validation errors from request binding.
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ProblemDetail> {
        val traceId = UUID.randomUUID().toString()
        logger.warn("Validation exception occurred [traceId={}]: {}", traceId, ex.message)
        
        val validationErrors = ex.bindingResult.fieldErrors
            .joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        
        val problemDetail = ProblemDetailBuilder.createValidationError(
            detail = "Validation failed: $validationErrors",
            traceId = traceId
        )
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .header("Content-Type", "application/problem+json")
            .body(problemDetail)
    }

    /**
     * Handles malformed request body errors.
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMalformedRequestException(ex: HttpMessageNotReadableException): ResponseEntity<ProblemDetail> {
        val traceId = UUID.randomUUID().toString()
        logger.warn("Malformed request exception occurred [traceId={}]: {}", traceId, ex.message)
        
        val problemDetail = ProblemDetailBuilder.createMalformedRequestError(traceId)
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .header("Content-Type", "application/problem+json")
            .body(problemDetail)
    }

    /**
     * Handles data integrity violations.
     */
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolationException(ex: DataIntegrityViolationException): ResponseEntity<ProblemDetail> {
        val traceId = UUID.randomUUID().toString()
        logger.error("Data integrity violation occurred [traceId={}]: {}", traceId, ex.message, ex)
        
        val problemDetail = ProblemDetailBuilder.createDataIntegrityError(traceId)
        
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .header("Content-Type", "application/problem+json")
            .body(problemDetail)
    }

    /**
     * Handles all other unhandled exceptions as generic internal server errors.
     */
    private fun handleGenericException(ex: Exception, traceId: String): ResponseEntity<ProblemDetail> {
        logger.error("Unhandled exception occurred [traceId={}]: {}", traceId, ex.message, ex)
        
        val problemDetail = ProblemDetailBuilder.createGenericError(traceId)
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .header("Content-Type", "application/problem+json")
            .body(problemDetail)
    }

    /**
     * Sanitizes exception details to prevent information leakage.
     * Only returns safe, user-friendly messages.
     */
    private fun sanitizeDetail(message: String?): String? {
        // In a production environment, you might want to be more aggressive
        // about filtering out sensitive information
        return message?.let { msg ->
            // Remove common sensitive patterns
            msg.replace(Regex("password[=:]\\s*\\S+", RegexOption.IGNORE_CASE), "password=[REDACTED]")
                .replace(Regex("token[=:]\\s*\\S+", RegexOption.IGNORE_CASE), "token=[REDACTED]")
                .replace(Regex("secret[=:]\\s*\\S+", RegexOption.IGNORE_CASE), "secret=[REDACTED]")
        }
    }
}