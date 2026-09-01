package com.featureflagmanager.controller

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CreateFlagRequest(
    @field:NotBlank
    @field:Size(max = 100)
    @field:Pattern(
        regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
        message = "must be lowercase kebab-case (e.g. 'my-new-flag')",
    )
    val name: String,
    @field:Size(max = 500) val description: String? = null,
)

data class UpdateFlagRequest(
    @field:Size(max = 500) val description: String? = null,
)

data class FlagEnvSummary(
    val env: String,
    val enabled: Boolean,
    val rollout: Int,
    val version: Int,
)

data class FlagResponse(
    val name: String,
    val description: String?,
    val envs: List<FlagEnvSummary> = emptyList(),
)

data class UpdateFlagEnvRequest(
    val enabled: Boolean? = null,
    @field:Min(0) @field:Max(100) val rollout: Int? = null,
    val version: Int,
)

data class FlagEnvResponse(
    val flagName: String,
    val env: String,
    val enabled: Boolean,
    val rollout: Int,
    val version: Int,
)

data class ClientFeature(
    val name: String,
    val enabled: Boolean,
)

data class ClientFeaturesResponse(
    val env: String,
    val features: List<ClientFeature>,
)

data class ErrorResponse(
    val error: String,
    val code: String,
    val current: Any? = null,
)
