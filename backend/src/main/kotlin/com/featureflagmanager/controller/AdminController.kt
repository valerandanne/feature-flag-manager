package com.featureflagmanager.controller

import com.featureflagmanager.entity.FeatureFlag
import com.featureflagmanager.entity.FlagEnv
import com.featureflagmanager.entity.Project
import com.featureflagmanager.service.FlagService
import com.featureflagmanager.service.ProjectService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class AdminController(
    private val projectService: ProjectService,
    private val flagService: FlagService,
) {
    @PostMapping("/projects")
    fun createProject(@Valid @RequestBody body: CreateProjectRequest): ResponseEntity<ProjectResponse> {
        val project = projectService.create(body.key, body.name)
        return ResponseEntity.status(HttpStatus.CREATED).body(project.toResponse())
    }

    @GetMapping("/projects")
    fun listProjects(): List<ProjectResponse> =
        projectService.list().map { it.toResponse() }

    @PostMapping("/projects/{projectId}/flags")
    fun createFlag(
        @PathVariable projectId: UUID,
        @Valid @RequestBody body: CreateFlagRequest,
    ): ResponseEntity<FlagResponse> {
        val flag = flagService.createFlag(projectId, body.name, body.description)
        return ResponseEntity.status(HttpStatus.CREATED).body(flag.toResponse())
    }

    @GetMapping("/projects/{projectId}/flags")
    fun listFlags(@PathVariable projectId: UUID): List<FlagResponse> =
        flagService.listFlags(projectId).map { it.toResponse() }

    @PatchMapping("/projects/{projectId}/flags/{flagName}")
    fun updateFlag(
        @PathVariable projectId: UUID,
        @PathVariable flagName: String,
        @RequestBody body: UpdateFlagRequest,
    ): FlagResponse =
        flagService.updateFlag(projectId, flagName, body.description).toResponse()

    @PatchMapping("/projects/{projectId}/flags/{flagName}/env/{envName}")
    fun updateFlagEnv(
        @PathVariable projectId: UUID,
        @PathVariable flagName: String,
        @PathVariable envName: String,
        @Valid @RequestBody body: UpdateFlagEnvRequest,
    ): FlagEnvResponse =
        flagService.updateFlagEnv(projectId, flagName, envName, body.enabled, body.rollout, body.version).toResponse()

    @GetMapping("/projects/{projectId}/flags/{flagName}/env/{envName}")
    fun getFlagEnv(
        @PathVariable projectId: UUID,
        @PathVariable flagName: String,
        @PathVariable envName: String,
    ): FlagEnvResponse =
        flagService.getFlagEnv(projectId, flagName, envName).toResponse()
}

private fun Project.toResponse() = ProjectResponse(id = id!!, key = key, name = name, createdAt = createdAt)

private fun FeatureFlag.toResponse() = FlagResponse(
    id = id!!,
    projectId = project.id!!,
    name = name,
    description = description,
)

private fun FlagEnv.toResponse() = FlagEnvResponse(
    flagId = flag.id!!,
    env = env.name,
    enabled = enabled,
    rollout = rollout,
    version = version,
)
