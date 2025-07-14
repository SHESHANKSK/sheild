package com.shielderrors

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan

/**
 * Auto-configuration for Shield Errors library.
 * This configuration will be automatically loaded by Spring Boot applications
 * that include this library as a dependency.
 */
@AutoConfiguration
@ConditionalOnWebApplication
@ComponentScan(basePackages = ["com.shielderrors"])
class ShieldErrorsAutoConfiguration {

    /**
     * Creates the GlobalExceptionHandler bean if one doesn't already exist.
     */
    @Bean
    @ConditionalOnMissingBean
    fun globalExceptionHandler(): GlobalExceptionHandler {
        return GlobalExceptionHandler()
    }
}