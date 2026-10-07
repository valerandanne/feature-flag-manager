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
        flag = featureFlagRepository.save(FeatureFlag(key = "new-checkout", name = "New Checkout", description = "desc"))
        flagEnvRepository.save(FlagEnv(flag = flag, env = prodEnv).apply { enabled = true })
    }

    @Test
    fun `client flags returns enabled flags for a known env`() {
        mockMvc.perform(get("/api/v1/client/flags/environments/production"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.env").value("production"))
            .andExpect(jsonPath("$.flags[0].key").value("new-checkout"))
            .andExpect(jsonPath("$.flags[0].name").value("New Checkout"))
            .andExpect(jsonPath("$.flags[0].enabled").value(true))
    }

    @Test
    fun `creating a flag starts disabled in every environment`() {
        val body = mapOf("key" to "new-flag", "name" to "New Flag")
        mockMvc.perform(
            post("/api/v1/flags")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(body)),
        ).andExpect(status().isCreated)

        mockMvc.perform(get("/api/v1/flags/new-flag/environments/production"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.enabled").value(false))
    }

    @Test
    fun `updating a flag's name persists and does not affect its key`() {
        val body = mapOf("name" to "Renamed Checkout")
        mockMvc.perform(
            patch("/api/v1/flags/new-checkout")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(body)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.key").value("new-checkout"))
            .andExpect(jsonPath("$.name").value("Renamed Checkout"))
    }

    @Test
    fun `client flags rejects unknown env`() {
        mockMvc.perform(get("/api/v1/client/flags/environments/nope"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `updating flag env succeeds and bumps version`() {
        val current = flagEnvRepository.findByFlagIdAndEnvId(flag.id!!, prodEnv.id!!)!!
        val body = mapOf("enabled" to false)
        mockMvc.perform(
            patch("/api/v1/flags/new-checkout/environments/production")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(body)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.enabled").value(false))
            .andExpect(jsonPath("$.version").value(current.version + 1))
    }

    @Test
    fun `updating flag env without an enabled field is rejected, not silently defaulted`() {
        mockMvc.perform(
            patch("/api/v1/flags/new-checkout/environments/production")
                .contentType("application/json")
                .content("{}"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
    }

    @Test
    fun `two simultaneous updates - one succeeds, the loser gets a clean 409 not a 500`() {
        val startBarrier = CountDownLatch(2)
        val pool = Executors.newFixedThreadPool(2)

        fun patchWith(body: Map<String, Any>) = pool.submit<Int> {
            startBarrier.countDown()
            startBarrier.await(5, TimeUnit.SECONDS)
            mockMvc.perform(
                patch("/api/v1/flags/new-checkout/environments/production")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(body)),
            ).andReturn().response.status
        }

        val futureA = patchWith(mapOf("enabled" to false))
        val futureB = patchWith(mapOf("enabled" to false))

        val statuses = listOf(futureA.get(5, TimeUnit.SECONDS), futureB.get(5, TimeUnit.SECONDS))
        pool.shutdown()

        assertThat(statuses).containsExactlyInAnyOrder(200, 409)
    }

    @Test
    fun `two simultaneous creates with the same key - one succeeds, the loser gets a clean rejection not a 500`() {
        // Create is check-then-insert, not atomic, so the loser may be caught by either the
        // service's pre-check (400 FLAG_ALREADY_EXISTS) or the DB unique constraint (409
        // FLAG_ALREADY_EXISTS) depending on thread interleaving - both are acceptable, a 500 is not.
        val startBarrier = CountDownLatch(2)
        val pool = Executors.newFixedThreadPool(2)
        val body = mapOf("key" to "race-flag", "name" to "Race Flag")

        fun createFlag() = pool.submit<Int> {
            startBarrier.countDown()
            startBarrier.await(5, TimeUnit.SECONDS)
            mockMvc.perform(
                post("/api/v1/flags")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(body)),
            ).andReturn().response.status
        }

        val futureA = createFlag()
        val futureB = createFlag()

        val statuses = listOf(futureA.get(5, TimeUnit.SECONDS), futureB.get(5, TimeUnit.SECONDS))
        pool.shutdown()

        assertThat(statuses).contains(201)
        assertThat(statuses.filter { it != 201 }).allMatch { it == 400 || it == 409 }
    }
}
