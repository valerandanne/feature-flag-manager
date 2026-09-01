package com.featureflagmanager.service

import com.featureflagmanager.config.CLIENT_FEATURES_CACHE
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
    @CacheEvict(cacheNames = [CLIENT_FEATURES_CACHE], allEntries = true)
    fun createFlag(name: String, description: String?): FeatureFlag {
        if (featureFlagRepository.findByName(name) != null) {
            throw InvalidRequestException("flag '$name' already exists", code = "FLAG_ALREADY_EXISTS")
        }
        val flag = featureFlagRepository.save(FeatureFlag(name = name, description = description))
        val envs = environmentRepository.findAll()
        flagEnvRepository.saveAll(envs.map { env -> FlagEnv(flag = flag, env = env) })
        logger.info("Created flag '{}' (id={}) with {} env configs", flag.name, flag.id, envs.size)
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
    fun updateFlag(flagName: String, description: String?): FeatureFlag {
        val flag = getFlag(flagName)
        description?.let {
            flag.description = it
            logger.info("Updated flag '{}' description", flagName)
        }
        return flag
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(cacheNames = [CLIENT_FEATURES_CACHE], key = "#envName"),
        CacheEvict(cacheNames = [FLAG_ENABLED_CACHE], key = "#flagName + ':' + #envName"),
    ])
    fun updateFlagEnv(
        flagName: String,
        envName: String,
        enabled: Boolean?,
        rollout: Int?,
        expectedVersion: Int,
    ): FlagEnv {
        if (enabled == null && rollout == null) {
            throw InvalidRequestException("at least one of 'enabled' or 'rollout' must be provided", code = "NO_FIELDS_TO_UPDATE")
        }
        val flag = getFlag(flagName)
        val env = environmentRepository.findByName(envName) ?: throw NotFoundException("env not found", code = "ENV_NOT_FOUND")
        val flagEnv = flagEnvRepository.findByFlagIdAndEnvId(flag.id!!, env.id!!)
            ?: FlagEnv(flag = flag, env = env)

        if (flagEnv.id != null && flagEnv.version != expectedVersion) {
            throw VersionConflictException("version conflict", toResponseState(flagEnv))
        }

        val previousEnabled = flagEnv.enabled
        val previousRollout = flagEnv.rollout
        enabled?.let { flagEnv.enabled = it }
        rollout?.let { flagEnv.rollout = it }
        val saved = flagEnvRepository.save(flagEnv)
        logger.info(
            "Updated flag '{}' env '{}': enabled {} -> {}, rollout {} -> {} (version {} -> {})",
            flagName, envName, previousEnabled, saved.enabled, previousRollout, saved.rollout, expectedVersion, saved.version,
        )
        return saved
    }

    @Transactional(readOnly = true)
    fun getFlagEnv(flagName: String, envName: String): FlagEnv {
        val flag = getFlag(flagName)
        val env = environmentRepository.findByName(envName) ?: throw NotFoundException("env not found", code = "ENV_NOT_FOUND")
        return flagEnvRepository.findByFlagIdAndEnvId(flag.id!!, env.id!!)
            ?: throw NotFoundException("env config not found", code = "FLAG_ENV_NOT_FOUND")
    }

    private fun getFlag(flagName: String): FeatureFlag =
        featureFlagRepository.findByName(flagName)
            ?: throw NotFoundException("flag not found", code = "FLAG_NOT_FOUND")

    private fun toResponseState(flagEnv: FlagEnv) = mapOf(
        "flagName" to flagEnv.flag.name,
        "env" to flagEnv.env.name,
        "enabled" to flagEnv.enabled,
        "rollout" to flagEnv.rollout,
        "version" to flagEnv.version,
    )
}

data class FlagWithEnvs(val flag: FeatureFlag, val envs: Map<String, FlagEnv>)
