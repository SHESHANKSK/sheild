package com.shielderrors

import com.shielderrors.annotations.ErrorCode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.web.bind.annotation.*

/**
 * Integration test for Shield Errors library with Spring Boot.
 */
@WebMvcTest(ShieldErrorsIntegrationTest.TestController::class)
class ShieldErrorsIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @ErrorCode(
        code = "PRODUCT_NOT_FOUND",
        status = HttpStatus.NOT_FOUND,
        doc = "https://api.example.com/docs/errors#product-not-found",
        title = "Product Not Found"
    )
    class ProductNotFoundException(message: String) : RuntimeException(message)

    @ErrorCode(
        code = "INVALID_PRICE",
        status = HttpStatus.BAD_REQUEST,
        title = "Invalid Product Price"
    )
    class InvalidPriceException(message: String) : RuntimeException(message)

    @RestController
    @RequestMapping("/api/test")
    class TestController {

        @GetMapping("/product/{id}")
        fun getProduct(@PathVariable id: String): Map<String, Any> {
            if (id == "404") {
                throw ProductNotFoundException("Product with ID $id not found")
            }
            if (id == "invalid-price") {
                throw InvalidPriceException("Price cannot be negative")
            }
            if (id == "generic") {
                throw RuntimeException("Something unexpected happened")
            }
            
            return mapOf("id" to id, "name" to "Test Product")
        }

        @PostMapping("/malformed")
        fun handleMalformed(@RequestBody body: Map<String, Any>): String {
            return "OK"
        }
    }

    @Test
    fun `should handle annotated exception with full metadata`() {
        mockMvc.perform(get("/api/test/product/404"))
            .andExpect(status().isNotFound)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(jsonPath("$.type").value("https://api.example.com/docs/errors#product-not-found"))
            .andExpect(jsonPath("$.title").value("Product Not Found"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.detail").value("Product with ID 404 not found"))
            .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
            .andExpect(jsonPath("$.traceId").exists())
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    fun `should handle annotated exception with minimal metadata`() {
        mockMvc.perform(get("/api/test/product/invalid-price"))
            .andExpect(status().isBadRequest)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(jsonPath("$.title").value("Invalid Product Price"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value("Price cannot be negative"))
            .andExpect(jsonPath("$.code").value("INVALID_PRICE"))
            .andExpect(jsonPath("$.traceId").exists())
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    fun `should handle generic exception`() {
        mockMvc.perform(get("/api/test/product/generic"))
            .andExpect(status().isInternalServerError)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(jsonPath("$.title").value("Internal Server Error"))
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
            .andExpect(jsonPath("$.detail").value("An unexpected error occurred. Please contact support if the problem persists."))
            .andExpect(jsonPath("$.traceId").exists())
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    fun `should handle malformed request`() {
        mockMvc.perform(post("/api/test/malformed")
            .contentType("application/json")
            .content("{ invalid json"))
            .andExpect(status().isBadRequest)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(jsonPath("$.title").value("Malformed Request"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
            .andExpect(jsonPath("$.traceId").exists())
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    fun `should return success for valid requests`() {
        mockMvc.perform(get("/api/test/product/123"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("123"))
            .andExpect(jsonPath("$.name").value("Test Product"))
    }

    @SpringBootApplication
    @ComponentScan(basePackages = ["com.shielderrors"])
    class TestApplication
}