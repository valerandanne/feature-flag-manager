package com.featureflagmanager.controller

import com.featureflagmanager.entity.FeatureFlag
import com.featureflagmanager.entity.FlagEnv
import com.featureflagmanager.service.FlagService
import com.featureflagmanager.service.FlagWithEnvs
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class AdminController(
    private val flagService: FlagService,
) {
    @PostMapping("/flags")
    fun createFlag(@Valid @RequestBody body: CreateFlagRequest): ResponseEntity<FlagResponse> {
        val flag = flagService.createFlag(body.name, body.description)
        return ResponseEntity.status(HttpStatus.CREATED).body(flag.toResponse())
    }

    @GetMapping("/flags")
    fun listFlags(): List<FlagResponse> =
        flagService.listFlagsWithEnvs().map { it.toResponse() }

    @PatchMapping("/flags/{flagName}")
    fun updateFlag(
        @PathVariable flagName: String,
        @Valid @RequestBody body: UpdateFlagRequest,
    ): FlagResponse =
        flagService.updateFlag(flagName, body.description).toResponse()

    @PatchMapping("/flags/{flagName}/envs/{envName}")
    fun updateFlagEnv(
        @PathVariable flagName: String,
        @PathVariable envName: String,
        @Valid @RequestBody body: UpdateFlagEnvRequest,
    ): FlagEnvResponse =
        flagService.updateFlagEnv(flagName, envName, body.enabled, body.rollout, body.version).toResponse()

    @GetMapping("/flags/{flagName}/envs/{envName}")
    fun getFlagEnv(
        @PathVariable flagName: String,
        @PathVariable envName: String,
    ): FlagEnvResponse =
        flagService.getFlagEnv(flagName, envName).toResponse()
}

private fun FeatureFlag.toResponse() = FlagResponse(
    name = name,
    description = description,
)

private fun FlagWithEnvs.toResponse() = FlagResponse(
    name = flag.name,
    description = flag.description,
    envs = envs.map { (envName, fe) -> FlagEnvSummary(env = envName, enabled = fe.enabled, rollout = fe.rollout, version = fe.version) },
)

private fun FlagEnv.toResponse() = FlagEnvResponse(
    flagName = flag.name,
    env = env.name,
    enabled = enabled,
    rollout = rollout,
    version = version,
)
