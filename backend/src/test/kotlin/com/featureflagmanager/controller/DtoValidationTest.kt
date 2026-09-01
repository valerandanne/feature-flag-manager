package com.featureflagmanager.controller

import jakarta.validation.Validation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DtoValidationTest {

    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `CreateFlagRequest rejects a blank name`() {
        val violations = validator.validate(CreateFlagRequest(name = ""))
        assertThat(violations).isNotEmpty
    }

    @Test
    fun `CreateFlagRequest accepts a valid name`() {
        val violations = validator.validate(CreateFlagRequest(name = "new-checkout"))
        assertThat(violations).isEmpty()
    }

    @Test
    fun `CreateFlagRequest rejects names with spaces or uppercase`() {
        assertThat(validator.validate(CreateFlagRequest(name = "New Checkout"))).isNotEmpty
        assertThat(validator.validate(CreateFlagRequest(name = "new_checkout"))).isNotEmpty
    }

    @Test
    fun `CreateFlagRequest rejects a description over 500 chars`() {
        val violations = validator.validate(CreateFlagRequest(name = "new-checkout", description = "a".repeat(501)))
        assertThat(violations).isNotEmpty
    }

    @Test
    fun `UpdateFlagEnvRequest rejects rollout above 100`() {
        val violations = validator.validate(UpdateFlagEnvRequest(rollout = 101, version = 1))
        assertThat(violations).isNotEmpty
    }

    @Test
    fun `UpdateFlagEnvRequest rejects rollout below 0`() {
        val violations = validator.validate(UpdateFlagEnvRequest(rollout = -1, version = 1))
        assertThat(violations).isNotEmpty
    }

    @Test
    fun `UpdateFlagEnvRequest accepts boundary values 0 and 100`() {
        assertThat(validator.validate(UpdateFlagEnvRequest(rollout = 0, version = 1))).isEmpty()
        assertThat(validator.validate(UpdateFlagEnvRequest(rollout = 100, version = 1))).isEmpty()
    }

    @Test
    fun `UpdateFlagEnvRequest accepts a null rollout`() {
        val violations = validator.validate(UpdateFlagEnvRequest(rollout = null, enabled = true, version = 1))
        assertThat(violations).isEmpty()
    }
}
