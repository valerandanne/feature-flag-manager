package com.featureflagmanager.repository

import com.featureflagmanager.entity.Environment
import com.featureflagmanager.entity.FeatureFlag
import com.featureflagmanager.entity.FlagEnv
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface EnvironmentRepository : JpaRepository<Environment, UUID> {
    fun findByName(name: String): Environment?
}

interface FeatureFlagRepository : JpaRepository<FeatureFlag, UUID> {
    fun findByName(name: String): FeatureFlag?
}

interface FlagEnvRepository : JpaRepository<FlagEnv, UUID> {
    fun findByFlagIdAndEnvId(flagId: UUID, envId: UUID): FlagEnv?

    @Query("select fe from FlagEnv fe join fetch fe.flag join fetch fe.env")
    fun findAllWithFlagAndEnv(): List<FlagEnv>

    @Query("select fe from FlagEnv fe join fetch fe.flag where fe.env.id = :envId")
    fun findAllByEnvId(envId: UUID): List<FlagEnv>
}
