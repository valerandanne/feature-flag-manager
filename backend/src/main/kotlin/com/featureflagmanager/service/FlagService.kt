package com.featureflagmanager.service

import com.featureflagmanager.config.CLIENT_FLAGS_CACHE
import com.featureflagmanager.config.FLAG_ENABLED_CACHE
import com.featureflagmanager.entity.FeatureFlag
import com.featureflagmanager.entity.FlagEnv
import com.featureflagmanager.repository.EnvironmentRepository
import com.featureflagmanager.repository.FeatureFlagRepository
import com.featureflagmanager.repository.FlagEnvRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Caching
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FlagService(
    private val featureFlagRepository: FeatureFlagRepository,
    private val flagEnvRepository: FlagEnvRepository,
    private val environmentRepository: EnvironmentRepository,
) {
    private val logger = LoggerFactory.getLogger(FlagService::class.java)

    @Transactional
    @CacheEvict(cacheNames = [CLIENT_FLAGS_CACHE], allEntries = true)
    fun createFlag(key: String, name: String, description: String?): FeatureFlag {
        if (featureFlagRepository.findByKey(key) != null) {
            throw InvalidRequestException("flag '$key' already exists", code = "FLAG_ALREADY_EXISTS")
        }
        val flag = featureFlagRepository.save(FeatureFlag(key = key, name = name, description = description))
        val envs = environmentRepository.findAll()
        flagEnvRepository.saveAll(envs.map { env -> FlagEnv(flag = flag, env = env) })
        logger.info("Created flag '{}' (id={}) with {} env configs", flag.key, flag.id, envs.size)
        return flag
    }

    @Transactional(readOnly = true)
    fun listFlagsWithEnvs(): List<FlagWithEnvs> {
        val flags = featureFlagRepository.findAll()
        val flagEnvsByFlagId = flagEnvRepository.findAllWithFlagAndEnv().groupBy { it.flag.id }
        return flags.map { flag ->
            val envs = flagEnvsByFlagId[flag.id].orEmpty().associate { it.env.name to it }
            FlagWithEnvs(flag, envs)
        }
    }

    @Transactional
    fun updateFlag(flagKey: String, name: String?, description: String?): FeatureFlag {
        val flag = getFlag(flagKey)
        name?.let {
            flag.name = it
            logger.info("Updated flag '{}' name", flagKey)
        }
        description?.let {
            flag.description = it
            logger.info("Updated flag '{}' description", flagKey)
        }
        return flag
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(cacheNames = [CLIENT_FLAGS_CACHE], key = "#envName"),
        CacheEvict(cacheNames = [FLAG_ENABLED_CACHE], key = "#flagKey + ':' + #envName"),
    ])
    fun updateFlagEnv(
        flagKey: String,
        envName: String,
        enabled: Boolean,
    ): FlagEnv {
        val flagEnv = flagEnvRepository.findByFlagKeyAndEnvName(flagKey, envName)
            ?: throw NotFoundException("no config found for flag '$flagKey' in env '$envName'", code = "FLAG_ENV_NOT_FOUND")

        val previousEnabled = flagEnv.enabled
        val previousVersion = flagEnv.version
        flagEnv.enabled = enabled
        val saved = flagEnvRepository.save(flagEnv)
        logger.info(
            "Updated flag '{}' env '{}': enabled {} -> {} (version {} -> {})",
            flagKey, envName, previousEnabled, saved.enabled, previousVersion, saved.version,
        )
        return saved
    }

    @Transactional(readOnly = true)
    fun getFlagEnv(flagKey: String, envName: String): FlagEnv =
        flagEnvRepository.findByFlagKeyAndEnvName(flagKey, envName)
            ?: throw NotFoundException("no config found for flag '$flagKey' in env '$envName'", code = "FLAG_ENV_NOT_FOUND")

    private fun getFlag(flagKey: String): FeatureFlag =
        featureFlagRepository.findByKey(flagKey)
            ?: throw NotFoundException("flag not found", code = "FLAG_NOT_FOUND")
}

data class FlagWithEnvs(val flag: FeatureFlag, val envs: Map<String, FlagEnv>)
