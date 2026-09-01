package com.featureflagmanager.controller

import jakarta.validation.Validation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DtoValidationTest {

    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `CreateFlagRequest rejects a blank key`() {
        val violations = validator.validate(CreateFlagRequest(key = "", name = "New Checkout"))
        assertThat(violations).isNotEmpty
    }

    @Test
    fun `CreateFlagRequest accepts a valid key`() {
        val violations = validator.validate(CreateFlagRequest(key = "new-checkout", name = "New Checkout"))
        assertThat(violations).isEmpty()
    }

    @Test
    fun `CreateFlagRequest rejects keys with spaces or uppercase`() {
        assertThat(validator.validate(CreateFlagRequest(key = "New Checkout", name = "New Checkout"))).isNotEmpty
        assertThat(validator.validate(CreateFlagRequest(key = "new_checkout", name = "New Checkout"))).isNotEmpty
    }

    @Test
    fun `CreateFlagRequest rejects a description over 500 chars`() {
        val violations = validator.validate(
            CreateFlagRequest(key = "new-checkout", name = "New Checkout", description = "a".repeat(501)),
        )
        assertThat(violations).isNotEmpty
    }

    @Test
    fun `CreateFlagRequest rejects a blank name`() {
        val violations = validator.validate(CreateFlagRequest(key = "new-checkout", name = ""))
        assertThat(violations).isNotEmpty
    }

    @Test
    fun `CreateFlagRequest rejects a name over 25 chars`() {
        val violations = validator.validate(CreateFlagRequest(key = "new-checkout", name = "a".repeat(26)))
        assertThat(violations).isNotEmpty
    }

    @Test
    fun `CreateFlagRequest accepts a name of exactly 25 chars`() {
        val violations = validator.validate(CreateFlagRequest(key = "new-checkout", name = "a".repeat(25)))
        assertThat(violations).isEmpty()
    }

    @Test
    fun `UpdateFlagRequest rejects a name over 25 chars`() {
        val violations = validator.validate(UpdateFlagRequest(name = "a".repeat(26)))
        assertThat(violations).isNotEmpty
    }

    @Test
    fun `UpdateFlagEnvRequest accepts a valid enabled value`() {
        val violations = validator.validate(UpdateFlagEnvRequest(enabled = true))
        assertThat(violations).isEmpty()
    }

    @Test
    fun `UpdateFlagEnvRequest rejects a null enabled value`() {
        val violations = validator.validate(UpdateFlagEnvRequest(enabled = null))
        assertThat(violations).isNotEmpty
    }
}
