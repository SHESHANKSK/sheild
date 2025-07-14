# Shield Errors

A Kotlin-based, Spring Boot-compatible library for consistent, secure, and standardized API error responses.

## Features

- 🛡️ **Secure**: Prevents internal server error leakage
- 📋 **Standardized**: RFC 7807 ProblemDetail format  
- 🏷️ **Annotation-based**: Simple `@ErrorCode` annotation for domain exceptions
- 🚀 **Auto-configured**: Plug-and-play with Spring Boot
- 🔍 **Traceable**: Built-in trace IDs and timestamps
- ⚡ **Lightweight**: < 1MB with minimal dependencies

## Quick Start

### 1. Add Dependency

**Maven:**
```xml
<dependency>
    <groupId>com.shielderrors</groupId>
    <artifactId>shield-errors</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Gradle:**
```kotlin
implementation("com.shielderrors:shield-errors:1.0.0")
```

### 2. Annotate Your Exceptions

```kotlin
import com.shielderrors.annotations.ErrorCode
import org.springframework.http.HttpStatus

@ErrorCode(
    code = "LOAN_NOT_FOUND",
    status = HttpStatus.NOT_FOUND,
    doc = "https://docs.mybank.com/errors/loan-not-found",
    title = "Loan Not Found"
)
class LoanNotFoundException(message: String) : RuntimeException(message)
```

### 3. That's It!

All exceptions will now return structured responses:

```json
{
  "type": "https://docs.mybank.com/errors/loan-not-found",
  "title": "Loan Not Found", 
  "status": 404,
  "detail": "Loan with ID 12345 was not found",
  "code": "LOAN_NOT_FOUND",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2025-01-14T10:30:00Z"
}
```

## Exception Handling

The library automatically handles:

- **Annotated Exceptions**: Uses `@ErrorCode` metadata
- **Validation Errors**: `MethodArgumentNotValidException`
- **Malformed Requests**: `HttpMessageNotReadableException`
- **Data Conflicts**: `DataIntegrityViolationException`
- **Generic Exceptions**: All other unhandled exceptions

### Annotation Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `code` | String | ✅ | - | Unique error identifier |
| `status` | HttpStatus | ❌ | INTERNAL_SERVER_ERROR | HTTP status code |
| `doc` | String | ❌ | "" | Documentation URL |
| `title` | String | ❌ | Exception message | Human-readable summary |

### Example Usage

```kotlin
// Minimal annotation
@ErrorCode(code = "USER_NOT_FOUND")
class UserNotFoundException(message: String) : RuntimeException(message)

// Full annotation
@ErrorCode(
    code = "INSUFFICIENT_FUNDS",
    status = HttpStatus.UNPROCESSABLE_ENTITY,
    doc = "https://api.mybank.com/docs/errors#insufficient-funds",
    title = "Insufficient Account Balance"
)
class InsufficientFundsException(message: String) : RuntimeException(message)

// Business validation error
@ErrorCode(
    code = "INVALID_TRANSFER_AMOUNT", 
    status = HttpStatus.BAD_REQUEST,
    title = "Invalid Transfer Amount"
)
class InvalidTransferAmountException(message: String) : RuntimeException(message)
```

## Error Response Format

All error responses follow the RFC 7807 ProblemDetail standard:

| Field | Type | Description |
|-------|------|-------------|
| `type` | URI | Link to error documentation |
| `title` | String | Human-readable summary |
| `status` | Integer | HTTP status code |
| `detail` | String | Specific error details |
| `code` | String | Unique error identifier |
| `traceId` | String | UUID for request tracing |
| `timestamp` | String | ISO 8601 UTC timestamp |

### Response Examples

**Domain Exception:**
```json
{
  "type": "https://api.example.com/docs/errors#user-not-found",
  "title": "User Not Found",
  "status": 404,
  "detail": "User with email john@example.com was not found",
  "code": "USER_NOT_FOUND",
  "traceId": "550e8400-e29b-41d4-a716-446655440000", 
  "timestamp": "2025-01-14T10:30:00Z"
}
```

**Validation Error:**
```json
{
  "title": "Validation Error",
  "status": 400,
  "detail": "Validation failed: name: Name is required; email: Email format is invalid",
  "code": "VALIDATION_ERROR",
  "traceId": "550e8400-e29b-41d4-a716-446655440001",
  "timestamp": "2025-01-14T10:30:01Z"
}
```

**Generic Error:**
```json
{
  "title": "Internal Server Error",
  "status": 500,
  "detail": "An unexpected error occurred. Please contact support if the problem persists.",
  "code": "INTERNAL_SERVER_ERROR", 
  "traceId": "550e8400-e29b-41d4-a716-446655440002",
  "timestamp": "2025-01-14T10:30:02Z"
}
```

## Configuration

The library is auto-configured and requires no additional setup. If you need to customize the behavior, you can override the `GlobalExceptionHandler` bean:

```kotlin
@Configuration
class CustomErrorConfiguration {
    
    @Bean
    @Primary
    fun customGlobalExceptionHandler(): GlobalExceptionHandler {
        // Your custom implementation
        return CustomGlobalExceptionHandler()
    }
}
```

### Disabling Auto-Configuration

To disable the auto-configuration:

```kotlin
@SpringBootApplication(exclude = [ShieldErrorsAutoConfiguration::class])
class MyApplication
```

Or in `application.properties`:
```properties
spring.autoconfigure.exclude=com.shielderrors.ShieldErrorsAutoConfiguration
```

## Security Features

### Information Sanitization

The library automatically redacts sensitive data patterns:

- `password=secret` → `password=[REDACTED]`
- `token=abc123` → `token=[REDACTED]`  
- `secret=xyz789` → `secret=[REDACTED]`

### Stack Trace Suppression

Internal errors never leak implementation details to clients. All unhandled exceptions return generic "Internal Server Error" messages while logging full details server-side.

### Safe Defaults

- Unknown exceptions return HTTP 500 with generic messages
- Exception details are sanitized before client responses
- Trace IDs enable correlation without exposing sensitive data

## Testing

### Running Tests

```bash
mvn test
```

### Test Coverage

The library includes comprehensive test coverage:

- Unit tests for all core components
- Integration tests with Spring Boot
- Edge case handling (null messages, unannotated exceptions)
- Security validation (sensitive data redaction)

### Testing Your Exceptions

```kotlin
@Test
fun `should handle custom domain exception`() {
    @ErrorCode(
        code = "CUSTOM_ERROR",
        status = HttpStatus.BAD_REQUEST,
        title = "Custom Error"
    )
    class CustomException(message: String) : RuntimeException(message)
    
    val handler = GlobalExceptionHandler()
    val response = handler.handleAnnotatedException(CustomException("Test message"))
    
    assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    val problemDetail = response.body as ProblemDetail
    assertEquals("CUSTOM_ERROR", problemDetail.properties["code"])
}
```

## Building from Source

### Prerequisites

- JDK 17+
- Maven 3.6+

### Build Commands

```bash
# Clone the repository
git clone https://github.com/shielderrors/shield-errors.git
cd shield-errors

# Compile the project
mvn clean compile

# Run tests
mvn test

# Package the library
mvn clean package

# Install to local repository
mvn clean install
```

## Integration Examples

### Spring Boot REST Controller

```kotlin
@RestController
@RequestMapping("/api/loans")
class LoanController {

    @GetMapping("/{id}")
    fun getLoan(@PathVariable id: String): LoanDto {
        // This exception will be automatically handled by Shield Errors
        throw LoanNotFoundException("Loan $id not found")
    }
}
```

### Service Layer

```kotlin
@Service
class UserService {

    fun validateUser(user: User) {
        if (user.age < 18) {
            throw UserValidationException("User must be at least 18 years old")
        }
    }
}
```

## Troubleshooting

### Common Issues

**Auto-configuration not working:**
- Ensure `@SpringBootApplication` is present
- Check that the library is on the classpath
- Verify Spring Boot version compatibility (3.0+)

**Custom exceptions not handled:**
- Verify the `@ErrorCode` annotation is present
- Check that the exception extends from a supported base class
- Ensure the exception is actually being thrown

**Tests failing:**
- Check Kotlin version compatibility (1.9+)
- Verify test dependencies are present
- Ensure proper Spring Boot test configuration

### Debug Logging

Enable debug logging to troubleshoot issues:

```properties
logging.level.com.shielderrors=DEBUG
```

## Contributing

### Development Setup

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass: `mvn test`
6. Submit a pull request

### Code Style

- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add KDoc comments for public APIs
- Include unit tests for new features

## Version History

### 1.0.0 (Current)
- Initial release
- RFC 7807 ProblemDetail support
- Annotation-based exception metadata
- Spring Boot auto-configuration
- Comprehensive test coverage

## License

MIT License. See [LICENSE](LICENSE) for details.

## Support

- 📖 [Documentation](https://github.com/shielderrors/shield-errors/wiki)
- 🐛 [Issue Tracker](https://github.com/shielderrors/shield-errors/issues)
- 💬 [Discussions](https://github.com/shielderrors/shield-errors/discussions)
- 📧 Email: support@shielderrors.com

## Acknowledgments

- Spring Boot team for the excellent auto-configuration framework
- RFC 7807 working group for the ProblemDetail specification
- Kotlin community for the amazing language and ecosystem