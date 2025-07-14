package com.shielderrors.utils

import com.shielderrors.annotations.ErrorCode
import org.springframework.http.HttpStatus
import kotlin.reflect.full.findAnnotation

/**
 * Utility class for extracting metadata from annotated exceptions.
 */
object ExceptionMetadataExtractor {

    /**
     * Data class representing extracted exception metadata.
     */
    data class ExceptionMetadata(
        val code: String,
        val status: HttpStatus,
        val doc: String,
        val title: String
    )

    /**
     * Extracts metadata from an exception using reflection.
     * 
     * @param exception The exception to extract metadata from
     * @return ExceptionMetadata containing the extracted information, or null if no annotation present
     */
    fun extractMetadata(exception: Throwable): ExceptionMetadata? {
        val errorCode = exception::class.findAnnotation<ErrorCode>()
        
        return errorCode?.let {
            ExceptionMetadata(
                code = it.code,
                status = it.status,
                doc = it.doc,
                title = if (it.title.isNotBlank()) it.title else (exception.message ?: "An error occurred")
            )
        }
    }

    /**
     * Checks if an exception has the ErrorCode annotation.
     */
    fun hasErrorCodeAnnotation(exception: Throwable): Boolean {
        return exception::class.findAnnotation<ErrorCode>() != null
    }
}