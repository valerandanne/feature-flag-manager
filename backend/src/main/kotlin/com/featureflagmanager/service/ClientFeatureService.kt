package com.featureflagmanager.service

import com.featureflagmanager.controller.ClientFeature
import com.featureflagmanager.controller.ClientFeaturesResponse
import com.featureflagmanager.entity.Project
import com.featureflagmanager.repository.EnvironmentRepository
import com.featureflagmanager.repository.FeatureFlagRepository
import com.featureflagmanager.repository.FlagEnvRepository
import org.springframework.stereotype.Service

@Service
class ClientFeatureService(
    private val featureFlagRepository: FeatureFlagRepository,
    private val flagEnvRepository: FlagEnvRepository,
    private val environmentRepository: EnvironmentRepository,
) {
    fun resolveFeatures(project: Project, envName: String): ClientFeaturesResponse {
        val env = environmentRepository.findByName(envName)
            ?: throw InvalidRequestException("invalid env")

        val flags = featureFlagRepository.findByProjectId(project.id!!)
        val features = flags.map { flag ->
            val cfg = flagEnvRepository.findByFlagIdAndEnvId(flag.id!!, env.id!!)
            ClientFeature(name = flag.name, enabled = cfg?.enabled ?: false)
        }
        return ClientFeaturesResponse(project = project.key, env = envName, features = features)
    }
}
