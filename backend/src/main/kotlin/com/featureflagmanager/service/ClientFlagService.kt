package com.featureflagmanager.service


import com.featureflagmanager.config.FLAG_ENABLED_CACHE
import com.featureflagmanager.config.CLIENT_FLAGS_CACHE
import com.featureflagmanager.controller.ClientFlag
import com.featureflagmanager.controller.ClientFlagsResponse
import com.featureflagmanager.repository.EnvironmentRepository
import com.featureflagmanager.repository.FeatureFlagRepository
import com.featureflagmanager.repository.FlagEnvRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ClientFlagService(
    private val featureFlagRepository: FeatureFlagRepository,
    private val flagEnvRepository: FlagEnvRepository,
    private val environmentRepository: EnvironmentRepository,
) {
    // Percentage-based rollout was scoped out here: enforcing it needs a stable per-client id to
    // bucket on, which doesn't exist yet. If added, the design is deterministic bucketing —
    // hash(flagKey + clientId) % 100 < rolloutPercent — applied below alongside `enabled`, so a
    // given client always gets the same result instead of a coin flip per request, and raising
    // the percentage only adds clients rather than reshuffling everyone.
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = [CLIENT_FLAGS_CACHE], key = "#envName")
    fun resolveFlags(envName: String): ClientFlagsResponse {
        val env = environmentRepository.findByName(envName)
            ?: throw InvalidRequestException("invalid env", code = "INVALID_ENV")

        val flags = featureFlagRepository.findAll()
        val enabledByFlagId = flagEnvRepository.findAllByEnvId(env.id!!)
            .associate { it.flag.id to it.enabled }

        val clientFlags = flags.map { flag ->
            ClientFlag(key = flag.key, name = flag.name, enabled = enabledByFlagId[flag.id] ?: false)
        }
        return ClientFlagsResponse(env = envName, flags = clientFlags)
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = [FLAG_ENABLED_CACHE], key = "#flagKey + ':' + #envName")
    fun getFlag(flagKey: String, envName: String): ClientFlag {
        val flag = featureFlagRepository.findByKey(flagKey)
            ?: throw NotFoundException("flag not found", code = "FLAG_NOT_FOUND")
        val env = environmentRepository.findByName(envName)
            ?: throw InvalidRequestException("invalid env", code = "INVALID_ENV")

        val enabled = flagEnvRepository.findByFlagIdAndEnvId(flag.id!!, env.id!!)?.enabled ?: false
        return ClientFlag(key = flag.key, name = flag.name, enabled = enabled)
    }
}
