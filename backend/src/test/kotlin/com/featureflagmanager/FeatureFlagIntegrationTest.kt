package com.featureflagmanager

import com.featureflagmanager.entity.Environment
import com.featureflagmanager.entity.FeatureFlag
import com.featureflagmanager.entity.FlagEnv
import com.featureflagmanager.repository.EnvironmentRepository
import com.featureflagmanager.repository.FeatureFlagRepository
import com.featureflagmanager.repository.FlagEnvRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FeatureFlagIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var environmentRepository: EnvironmentRepository
    @Autowired lateinit var featureFlagRepository: FeatureFlagRepository
    @Autowired lateinit var flagEnvRepository: FlagEnvRepository

    lateinit var prodEnv: Environment
    lateinit var flag: FeatureFlag

    @BeforeEach
    fun setUp() {
        flagEnvRepository.deleteAll()
        featureFlagRepository.deleteAll()
        environmentRepository.deleteAll()

        prodEnv = environmentRepository.save(Environment(name = "production"))
        flag = featureFlagRepository.save(FeatureFlag(name = "new-checkout", description = "desc"))
        flagEnvRepository.save(FlagEnv(flag = flag, env = prodEnv).apply { enabled = true })
    }

    @Test
    fun `client features returns enabled flags for a known env`() {
        mockMvc.perform(get("/api/v1/client/features?env=production"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.env").value("production"))
            .andExpect(jsonPath("$.features[0].name").value("new-checkout"))
            .andExpect(jsonPath("$.features[0].enabled").value(true))
    }

    @Test
    fun `client features rejects unknown env`() {
        mockMvc.perform(get("/api/v1/client/features?env=nope"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `updating flag env with stale version returns 409 with current state`() {
        val body = mapOf("enabled" to false, "version" to 999)
        mockMvc.perform(
            patch("/api/v1/flags/new-checkout/envs/production")
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
            patch("/api/v1/flags/new-checkout/envs/production")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(body)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.enabled").value(false))
            .andExpect(jsonPath("$.version").value(current.version + 1))
    }

    @Test
    fun `two simultaneous updates - one succeeds, the loser gets a clean 409 not a 500`() {
        val current = flagEnvRepository.findByFlagIdAndEnvId(flag.id!!, prodEnv.id!!)!!
        val startBarrier = CountDownLatch(2)
        val pool = Executors.newFixedThreadPool(2)

        fun patchWith(body: Map<String, Any>) = pool.submit<Int> {
            startBarrier.countDown()
            startBarrier.await(5, TimeUnit.SECONDS)
            mockMvc.perform(
                patch("/api/v1/flags/new-checkout/envs/production")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(body)),
            ).andReturn().response.status
        }

        val futureA = patchWith(mapOf("enabled" to false, "version" to current.version))
        val futureB = patchWith(mapOf("rollout" to 42, "version" to current.version))

        val statuses = listOf(futureA.get(5, TimeUnit.SECONDS), futureB.get(5, TimeUnit.SECONDS))
        pool.shutdown()

        assertThat(statuses).containsExactlyInAnyOrder(200, 409)
    }
}
