package com.featureflagmanager.controller

import com.featureflagmanager.entity.FeatureFlag
import com.featureflagmanager.entity.FlagEnv
import com.featureflagmanager.service.FlagService
import com.featureflagmanager.service.FlagWithEnvs
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Admin endpoints for creating and managing feature flags: flag metadata (key/name/description)
 * and per-environment enabled state. Not used by clients resolving flag values at runtime —
 * see [ClientController] for that.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Admin", description = "Create and manage feature flags and their per-environment enabled state")
class AdminController(
    private val flagService: FlagService,
) {
    /** Creates a new feature flag. A flag's key is unique and immutable once created. */
    @Operation(summary = "Create a feature flag")
    @ApiResponse(responseCode = "201", description = "Flag created")
    @PostMapping("/flags")
    fun createFlag(@Valid @RequestBody body: CreateFlagRequest): ResponseEntity<FlagResponse> {
        val flag = flagService.createFlag(body.key, body.name, body.description)
        return ResponseEntity.status(HttpStatus.CREATED).body(flag.toResponse())
    }

    /** Lists every feature flag along with its current enabled state in each environment. */
    @Operation(summary = "List all feature flags with their per-environment state")
    @GetMapping("/flags")
    fun listFlags(): List<FlagResponse> =
        flagService.listFlagsWithEnvs().map { it.toResponse() }

    /**
     * Updates a flag's metadata (name and/or description). Its key cannot be changed. Does not
     * affect whether the flag is enabled in any environment — use [updateFlagEnvironmentStatus]
     * for that.
     */
    @Operation(summary = "Update a flag's metadata (name and/or description)")
    @ApiResponses(
        ApiResponse(responseCode = "404", description = "Flag not found"),
        ApiResponse(responseCode = "400", description = "Validation error"),
    )
    @PatchMapping("/flags/{flagKey}")
    fun updateFlagMetadata(
        @PathVariable flagKey: String,
        @Valid @RequestBody body: UpdateFlagRequest,
    ): FlagResponse =
        flagService.updateFlag(flagKey, body.name, body.description).toResponse()

    /**
     * Updates a flag's enabled state for a single environment. Concurrent updates to the
     * same environment are guarded by optimistic locking (JPA's `@Version`), which fails with 409
     * if two writes race, so callers must re-fetch and retry on conflict.
     */
    @Operation(summary = "Update a flag's enabled state for one environment")
    @ApiResponses(
        ApiResponse(responseCode = "404", description = "Flag or environment not found"),
        ApiResponse(responseCode = "400", description = "Validation error"),
        ApiResponse(responseCode = "409", description = "Version conflict: a concurrent update raced this one"),
    )
    @PatchMapping("/flags/{flagKey}/environments/{envName}")
    fun updateFlagEnvironmentStatus(
        @PathVariable flagKey: String,
        @PathVariable envName: String,
        @Valid @RequestBody body: UpdateFlagEnvRequest,
    ): FlagEnvResponse =
        flagService.updateFlagEnv(flagKey, envName, body.enabled!!).toResponse()

    /** Fetches a single flag's current enabled state in one environment. */
    @Operation(summary = "Get a flag's state in one environment")
    @ApiResponse(responseCode = "404", description = "Flag or environment not found")
    @GetMapping("/flags/{flagKey}/environments/{envName}")
    fun getFlagEnv(
        @PathVariable flagKey: String,
        @PathVariable envName: String,
    ): FlagEnvResponse =
        flagService.getFlagEnv(flagKey, envName).toResponse()
}

private fun FeatureFlag.toResponse() = FlagResponse(
    key = key,
    name = name,
    description = description,
)

private fun FlagWithEnvs.toResponse() = FlagResponse(
    key = flag.key,
    name = flag.name,
    description = flag.description,
    envs = envs.map { (envName, fe) -> FlagEnvSummary(env = envName, enabled = fe.enabled, version = fe.version) },
)

private fun FlagEnv.toResponse() = FlagEnvResponse(
    flagKey = flag.key,
    env = env.name,
    enabled = enabled,
    version = version,
)
