package com.featureflagmanager.service

import com.featureflagmanager.entity.FeatureFlag
import com.featureflagmanager.entity.FlagEnv
import com.featureflagmanager.repository.EnvironmentRepository
import com.featureflagmanager.repository.FeatureFlagRepository
import com.featureflagmanager.repository.FlagEnvRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FlagService(
    private val featureFlagRepository: FeatureFlagRepository,
    private val flagEnvRepository: FlagEnvRepository,
    private val environmentRepository: EnvironmentRepository,
    private val projectService: ProjectService,
) {
    @Transactional
    fun createFlag(projectId: UUID, name: String, description: String?): FeatureFlag {
        val project = projectService.getById(projectId)
        if (featureFlagRepository.findByProjectIdAndName(projectId, name) != null) {
            throw InvalidRequestException("flag '$name' already exists for this project")
        }
        return featureFlagRepository.save(FeatureFlag(project = project, name = name, description = description))
    }

    fun listFlags(projectId: UUID): List<FeatureFlag> =
        featureFlagRepository.findByProjectId(projectId)

    @Transactional
    fun updateFlag(projectId: UUID, flagName: String, description: String?): FeatureFlag {
        val flag = getFlag(projectId, flagName)
        description?.let { flag.description = it }
        return flag
    }

    @Transactional
    fun updateFlagEnv(
        projectId: UUID,
        flagName: String,
        envName: String,
        enabled: Boolean?,
        rollout: Int?,
        expectedVersion: Int,
    ): FlagEnv {
        val flag = getFlag(projectId, flagName)
        val env = environmentRepository.findByName(envName) ?: throw NotFoundException("env not found")
        val flagEnv = flagEnvRepository.findByFlagIdAndEnvId(flag.id!!, env.id!!)
            ?: FlagEnv(flag = flag, env = env)

        if (flagEnv.id != null && flagEnv.version != expectedVersion) {
            throw VersionConflictException("version conflict", toResponseState(flagEnv))
        }

        enabled?.let { flagEnv.enabled = it }
        rollout?.let { flagEnv.rollout = it }
        return flagEnvRepository.save(flagEnv)
    }

    fun getFlagEnv(projectId: UUID, flagName: String, envName: String): FlagEnv {
        val flag = getFlag(projectId, flagName)
        val env = environmentRepository.findByName(envName) ?: throw NotFoundException("env not found")
        return flagEnvRepository.findByFlagIdAndEnvId(flag.id!!, env.id!!)
            ?: throw NotFoundException("env config not found")
    }

    private fun getFlag(projectId: UUID, flagName: String): FeatureFlag =
        featureFlagRepository.findByProjectIdAndName(projectId, flagName)
            ?: throw NotFoundException("flag not found")

    private fun toResponseState(flagEnv: FlagEnv) = mapOf(
        "flagId" to flagEnv.flag.id,
        "env" to flagEnv.env.name,
        "enabled" to flagEnv.enabled,
        "rollout" to flagEnv.rollout,
        "version" to flagEnv.version,
    )
}
