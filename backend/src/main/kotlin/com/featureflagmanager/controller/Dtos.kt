package com.featureflagmanager.controller

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.time.Instant
import java.util.UUID

data class CreateProjectRequest(
    @field:NotBlank val key: String,
    @field:NotBlank val name: String,
)

data class ProjectResponse(
    val id: UUID,
    val key: String,
    val name: String,
    val createdAt: Instant,
)

data class CreateFlagRequest(
    @field:NotBlank val name: String,
    val description: String? = null,
)

data class UpdateFlagRequest(
    val description: String? = null,
)

data class FlagResponse(
    val id: UUID,
    val projectId: UUID,
    val name: String,
    val description: String?,
)

data class UpdateFlagEnvRequest(
    val enabled: Boolean? = null,
    @field:Min(0) @field:Max(100) val rollout: Int? = null,
    val version: Int,
)

data class FlagEnvResponse(
    val flagId: UUID,
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
    val project: String,
    val env: String,
    val features: List<ClientFeature>,
)

data class ErrorResponse(
    val error: String,
    val current: Any? = null,
)
