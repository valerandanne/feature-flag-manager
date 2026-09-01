package com.featureflagmanager.repository

import com.featureflagmanager.entity.ApiKey
import com.featureflagmanager.entity.Environment
import com.featureflagmanager.entity.FeatureFlag
import com.featureflagmanager.entity.FlagEnv
import com.featureflagmanager.entity.Project
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProjectRepository : JpaRepository<Project, UUID> {
    fun findByKey(key: String): Project?
}

interface EnvironmentRepository : JpaRepository<Environment, UUID> {
    fun findByName(name: String): Environment?
}

interface FeatureFlagRepository : JpaRepository<FeatureFlag, UUID> {
    fun findByProjectId(projectId: UUID): List<FeatureFlag>
    fun findByProjectIdAndName(projectId: UUID, name: String): FeatureFlag?
}

interface FlagEnvRepository : JpaRepository<FlagEnv, UUID> {
    fun findByFlagIdAndEnvId(flagId: UUID, envId: UUID): FlagEnv?
}

interface ApiKeyRepository : JpaRepository<ApiKey, UUID> {
    fun findByKey(key: String): ApiKey?
}
