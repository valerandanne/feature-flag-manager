package com.featureflagmanager.service

import com.featureflagmanager.config.CLIENT_FEATURES_CACHE
import com.featureflagmanager.config.FLAG_ENABLED_CACHE
import com.featureflagmanager.controller.ClientFeature
import com.featureflagmanager.controller.ClientFeaturesResponse
import com.featureflagmanager.repository.EnvironmentRepository
import com.featureflagmanager.repository.FeatureFlagRepository
import com.featureflagmanager.repository.FlagEnvRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ClientFeatureService(
    private val featureFlagRepository: FeatureFlagRepository,
    private val flagEnvRepository: FlagEnvRepository,
    private val environmentRepository: EnvironmentRepository,
) {
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = [CLIENT_FEATURES_CACHE], key = "#envName")
    fun resolveFeatures(envName: String): ClientFeaturesResponse {
        val env = environmentRepository.findByName(envName)
            ?: throw InvalidRequestException("invalid env", code = "INVALID_ENV")

        val flags = featureFlagRepository.findAll()
        val enabledByFlagId = flagEnvRepository.findAllByEnvId(env.id!!)
            .associate { it.flag.id to it.enabled }

        val features = flags.map { flag ->
            ClientFeature(name = flag.name, enabled = enabledByFlagId[flag.id] ?: false)
        }
        return ClientFeaturesResponse(env = envName, features = features)
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = [FLAG_ENABLED_CACHE], key = "#flagName + ':' + #envName")
    fun isEnabled(flagName: String, envName: String): Boolean {
        val flag = featureFlagRepository.findByName(flagName)
            ?: throw NotFoundException("flag not found", code = "FLAG_NOT_FOUND")
        val env = environmentRepository.findByName(envName)
            ?: throw InvalidRequestException("invalid env", code = "INVALID_ENV")

        return flagEnvRepository.findByFlagIdAndEnvId(flag.id!!, env.id!!)?.enabled ?: false
    }
}
