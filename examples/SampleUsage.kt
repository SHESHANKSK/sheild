package com.example

import com.shielderrors.annotations.ErrorCode
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.PositiveOrZero

/**
 * Sample Spring Boot application demonstrating Shield Errors library usage.
 * 
 * To run this example:
 * 1. Add Shield Errors dependency to your project
 * 2. Copy this file to your project
 * 3. Run the application
 * 4. Test the endpoints to see error responses
 */
@SpringBootApplication
class SampleApplication

fun main(args: Array<String>) {
    SpringApplication.run(SampleApplication::class.java, *args)
}

// Domain Exception Examples

@ErrorCode(
    code = "USER_NOT_FOUND",
    status = HttpStatus.NOT_FOUND,
    doc = "https://api.example.com/docs/errors#user-not-found",
    title = "User Not Found"
)
class UserNotFoundException(message: String) : RuntimeException(message)

@ErrorCode(
    code = "INSUFFICIENT_BALANCE",
    status = HttpStatus.UNPROCESSABLE_ENTITY,
    doc = "https://api.example.com/docs/errors#insufficient-balance",
    title = "Insufficient Account Balance"
)
class InsufficientBalanceException(message: String) : RuntimeException(message)

@ErrorCode(
    code = "INVALID_TRANSFER_AMOUNT",
    status = HttpStatus.BAD_REQUEST,
    title = "Invalid Transfer Amount"
)
class InvalidTransferAmountException(message: String) : RuntimeException(message)

@ErrorCode(
    code = "DUPLICATE_EMAIL",
    status = HttpStatus.CONFLICT,
    title = "Email Already Exists"
)
class DuplicateEmailException(message: String) : RuntimeException(message)

// Data Transfer Objects

data class CreateUserRequest(
    @field:NotBlank(message = "Name is required")
    val name: String?,
    
    @field:NotBlank(message = "Email is required") 
    val email: String?
)

data class TransferRequest(
    @field:NotBlank(message = "From account is required")
    val fromAccount: String?,
    
    @field:NotBlank(message = "To account is required")
    val toAccount: String?,
    
    @field:PositiveOrZero(message = "Amount must be positive")
    val amount: Double?
)

data class User(
    val id: String,
    val name: String,
    val email: String,
    val balance: Double
)

// REST Controllers

@RestController
@RequestMapping("/api/users")
class UserController {

    private val users = mutableMapOf(
        "1" to User("1", "John Doe", "john@example.com", 1000.0),
        "2" to User("2", "Jane Smith", "jane@example.com", 500.0)
    )

    @GetMapping("/{id}")
    fun getUser(@PathVariable id: String): User {
        return users[id] ?: throw UserNotFoundException("User with ID $id not found")
    }

    @PostMapping
    fun createUser(@Valid @RequestBody request: CreateUserRequest): User {
        // Check for duplicate email
        users.values.find { it.email == request.email }?.let {
            throw DuplicateEmailException("User with email ${request.email} already exists")
        }

        val newId = (users.size + 1).toString()
        val user = User(newId, request.name!!, request.email!!, 0.0)
        users[newId] = user
        return user
    }

    @PostMapping("/{id}/transfer")
    fun transferMoney(
        @PathVariable id: String,
        @Valid @RequestBody request: TransferRequest
    ): Map<String, Any> {
        val fromUser = users[id] ?: throw UserNotFoundException("User with ID $id not found")
        val toUser = users[request.toAccount] ?: throw UserNotFoundException("Destination user not found")
        
        val amount = request.amount!!
        
        if (amount <= 0) {
            throw InvalidTransferAmountException("Transfer amount must be greater than zero")
        }
        
        if (fromUser.balance < amount) {
            throw InsufficientBalanceException(
                "Insufficient balance. Available: ${fromUser.balance}, Required: $amount"
            )
        }

        // Perform transfer
        users[id] = fromUser.copy(balance = fromUser.balance - amount)
        users[request.toAccount!!] = toUser.copy(balance = toUser.balance + amount)

        return mapOf(
            "message" to "Transfer successful",
            "amount" to amount,
            "fromAccount" to id,
            "toAccount" to request.toAccount,
            "newBalance" to (fromUser.balance - amount)
        )
    }

    @GetMapping("/error/generic")
    fun triggerGenericError(): String {
        // This will be handled as a generic internal server error
        throw RuntimeException("Something unexpected happened in the system")
    }

    @PostMapping("/error/malformed")
    fun triggerMalformedError(@RequestBody body: Map<String, Any>): String {
        // Send malformed JSON to trigger HttpMessageNotReadableException
        return "This won't be reached with malformed JSON"
    }
}

/**
 * Test Scenarios:
 * 
 * 1. User Not Found (404):
 *    GET /api/users/999
 *    Response: {"type":"https://api.example.com/docs/errors#user-not-found","title":"User Not Found","status":404,"detail":"User with ID 999 not found","code":"USER_NOT_FOUND","traceId":"...","timestamp":"..."}
 * 
 * 2. Validation Error (400):
 *    POST /api/users with {"name":"","email":""}
 *    Response: {"title":"Validation Error","status":400,"detail":"Validation failed: name: Name is required; email: Email is required","code":"VALIDATION_ERROR","traceId":"...","timestamp":"..."}
 * 
 * 3. Duplicate Email (409):
 *    POST /api/users with {"name":"Test","email":"john@example.com"}
 *    Response: {"title":"Email Already Exists","status":409,"detail":"User with email john@example.com already exists","code":"DUPLICATE_EMAIL","traceId":"...","timestamp":"..."}
 * 
 * 4. Insufficient Balance (422):
 *    POST /api/users/1/transfer with {"fromAccount":"1","toAccount":"2","amount":2000}
 *    Response: {"type":"https://api.example.com/docs/errors#insufficient-balance","title":"Insufficient Account Balance","status":422,"detail":"Insufficient balance. Available: 1000.0, Required: 2000.0","code":"INSUFFICIENT_BALANCE","traceId":"...","timestamp":"..."}
 * 
 * 5. Invalid Transfer Amount (400):
 *    POST /api/users/1/transfer with {"fromAccount":"1","toAccount":"2","amount":-100}
 *    Response: {"title":"Invalid Transfer Amount","status":400,"detail":"Transfer amount must be greater than zero","code":"INVALID_TRANSFER_AMOUNT","traceId":"...","timestamp":"..."}
 * 
 * 6. Generic Error (500):
 *    GET /api/users/error/generic
 *    Response: {"title":"Internal Server Error","status":500,"detail":"An unexpected error occurred. Please contact support if the problem persists.","code":"INTERNAL_SERVER_ERROR","traceId":"...","timestamp":"..."}
 * 
 * 7. Malformed Request (400):
 *    POST /api/users/error/malformed with malformed JSON: {"invalid": json}
 *    Response: {"title":"Malformed Request","status":400,"detail":"The request body is malformed or contains invalid JSON.","code":"MALFORMED_REQUEST","traceId":"...","timestamp":"..."}
 */