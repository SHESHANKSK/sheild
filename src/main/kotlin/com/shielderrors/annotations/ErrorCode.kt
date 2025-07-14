package com.shielderrors.annotations

import org.springframework.http.HttpStatus

/**
 * Annotation for domain exceptions to specify error metadata.
 * This annotation provides the necessary information to build structured error responses.
 *
 * @param code Unique business or technical error code (e.g., "LOAN-404", "USER-VALIDATION-001")
 * @param status HTTP status code to return (defaults to INTERNAL_SERVER_ERROR)
 * @param doc URI pointing to error documentation (optional)
 * @param title Short, human-readable summary of the error (optional, defaults to exception message)
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ErrorCode(
    val code: String,
    val status: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    val doc: String = "",
    val title: String = ""
)