package com.featureflagmanager

import com.featureflagmanager.entity.ApiKey
import com.featureflagmanager.entity.Environment
import com.featureflagmanager.entity.FeatureFlag
import com.featureflagmanager.entity.FlagEnv
import com.featureflagmanager.entity.Project
import com.featureflagmanager.repository.ApiKeyRepository
import com.featureflagmanager.repository.EnvironmentRepository
import com.featureflagmanager.repository.FeatureFlagRepository
import com.featureflagmanager.repository.FlagEnvRepository
import com.featureflagmanager.repository.ProjectRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FeatureFlagIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var projectRepository: ProjectRepository
    @Autowired lateinit var environmentRepository: EnvironmentRepository
    @Autowired lateinit var featureFlagRepository: FeatureFlagRepository
    @Autowired lateinit var flagEnvRepository: FlagEnvRepository
    @Autowired lateinit var apiKeyRepository: ApiKeyRepository

    lateinit var project: Project
    lateinit var prodEnv: Environment
    lateinit var flag: FeatureFlag

    @BeforeEach
    fun setUp() {
        flagEnvRepository.deleteAll()
        featureFlagRepository.deleteAll()
        apiKeyRepository.deleteAll()
        projectRepository.deleteAll()
        environmentRepository.deleteAll()

        project = projectRepository.save(Project(key = "payments", name = "Payments"))
        prodEnv = environmentRepository.save(Environment(name = "production"))
        flag = featureFlagRepository.save(FeatureFlag(project = project, name = "new-checkout", description = "desc"))
        flagEnvRepository.save(FlagEnv(flag = flag, env = prodEnv).apply { enabled = true })
        apiKeyRepository.save(ApiKey(key = "payments-mvp-key", project = project))
    }

    @Test
    fun `client features returns enabled flags for a valid key`() {
        mockMvc.perform(get("/api/v1/client/features?env=production").header("Authorization", "Bearer payments-mvp-key"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.project").value("payments"))
            .andExpect(jsonPath("$.env").value("production"))
            .andExpect(jsonPath("$.features[0].name").value("new-checkout"))
            .andExpect(jsonPath("$.features[0].enabled").value(true))
    }

    @Test
    fun `client features rejects invalid key`() {
        mockMvc.perform(get("/api/v1/client/features?env=production").header("Authorization", "Bearer nope"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `client features rejects missing key`() {
        mockMvc.perform(get("/api/v1/client/features?env=production"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `updating flag env with stale version returns 409 with current state`() {
        val body = mapOf("enabled" to false, "version" to 999)
        mockMvc.perform(
            patch("/api/v1/projects/${project.id}/flags/new-checkout/env/production")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(body)),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.current.version").exists())
    }

    @Test
    fun `updating flag env with correct version succeeds and bumps version`() {
        val current = flagEnvRepository.findByFlagIdAndEnvId(flag.id!!, prodEnv.id!!)!!
        val body = mapOf("enabled" to false, "version" to current.version)
        mockMvc.perform(
            patch("/api/v1/projects/${project.id}/flags/new-checkout/env/production")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(body)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.enabled").value(false))
            .andExpect(jsonPath("$.version").value(current.version + 1))
    }
}
