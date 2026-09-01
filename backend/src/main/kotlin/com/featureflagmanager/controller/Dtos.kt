package com.featureflagmanager.controller

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CreateFlagRequest(
    @field:Schema(description = "Unique, immutable flag identifier used in API paths", example = "new-checkout")
    @field:NotBlank
    @field:Size(max = 100)
    @field:Pattern(
        regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
        message = "must be lowercase kebab-case (e.g. 'my-new-flag')",
    )
    val key: String,
    @field:Schema(description = "Human-readable display name", example = "New Checkout")
    @field:NotBlank
    @field:Size(max = 25)
    val name: String,
    @field:Schema(description = "Human-readable description of what the flag controls")
    @field:Size(max = 500) val description: String? = null,
)

data class UpdateFlagRequest(
    @field:Schema(description = "New display name for the flag; does not affect its key or enabled state")
    @field:Size(max = 25) val name: String? = null,
    @field:Schema(description = "New description for the flag; does not affect enabled state in any environment")
    @field:Size(max = 500) val description: String? = null,
)

data class FlagEnvSummary(
    val env: String,
    val enabled: Boolean,
    val version: Int,
)

data class FlagResponse(
    val key: String,
    val name: String,
    val description: String?,
    val envs: List<FlagEnvSummary> = emptyList(),
)

data class UpdateFlagEnvRequest(
    @field:Schema(description = "Whether the flag is enabled in this environment")
    @field:NotNull
    val enabled: Boolean?,
)

data class FlagEnvResponse(
    val flagKey: String,
    val env: String,
    val enabled: Boolean,
    val version: Int,
)

data class ClientFlag(
    val key: String,
    val name: String,
    val enabled: Boolean,
)

data class ClientFlagsResponse(
    val env: String,
    val flags: List<ClientFlag>,
)

data class ErrorResponse(
    val error: String,
    val code: String,
    val current: Any? = null,
)
