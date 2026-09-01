package com.featureflagmanager.repository

import com.featureflagmanager.entity.Environment
import com.featureflagmanager.entity.FeatureFlag
import com.featureflagmanager.entity.FlagEnv
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface EnvironmentRepository : JpaRepository<Environment, Long> {
    fun findByName(name: String): Environment?
}

interface FeatureFlagRepository : JpaRepository<FeatureFlag, Long> {
    fun findByKey(key: String): FeatureFlag?
}

interface FlagEnvRepository : JpaRepository<FlagEnv, Long> {
    fun findByFlagIdAndEnvId(flagId: Long, envId: Long): FlagEnv?

    @Query("select fe from FlagEnv fe join fetch fe.flag join fetch fe.env")
    fun findAllWithFlagAndEnv(): List<FlagEnv>

    @Query("select fe from FlagEnv fe join fetch fe.flag where fe.env.id = :envId")
    fun findAllByEnvId(envId: Long): List<FlagEnv>

    @Query("select fe from FlagEnv fe join fetch fe.flag f join fetch fe.env e where f.key = :flagKey and e.name = :envName")
    fun findByFlagKeyAndEnvName(flagKey: String, envName: String): FlagEnv?
}
