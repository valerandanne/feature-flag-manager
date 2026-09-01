package com.featureflagmanager.controller

import com.featureflagmanager.entity.Environment
import com.featureflagmanager.entity.FeatureFlag
import com.featureflagmanager.entity.FlagEnv
import com.featureflagmanager.service.FlagService
import com.featureflagmanager.service.FlagWithEnvs
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.util.UUID

class AdminControllerTest {

    private val flagService = mockk<FlagService>()
    private val controller = AdminController(flagService)

    private fun flag(name: String = "new-checkout", description: String? = "desc") =
        FeatureFlag(name = name, description = description).apply { id = UUID.randomUUID() }

    private fun env(name: String = "production") = Environment(name = name).apply { id = UUID.randomUUID() }

    @Test
    fun `createFlag returns 201 with the created flag`() {
        val f = flag()
        every { flagService.createFlag("new-checkout", "desc") } returns f

        val response = controller.createFlag(CreateFlagRequest(name = "new-checkout", description = "desc"))

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body?.name).isEqualTo("new-checkout")
        assertThat(response.body?.description).isEqualTo("desc")
        assertThat(response.body?.envs).isEmpty()
    }

    @Test
    fun `listFlags maps envs by env name into FlagEnvSummary`() {
        val f = flag()
        val e = env()
        val fe = FlagEnv(flag = f, env = e).apply { enabled = true; rollout = 42; version = 3 }
        every { flagService.listFlagsWithEnvs() } returns listOf(FlagWithEnvs(f, mapOf("production" to fe)))

        val result = controller.listFlags()

        assertThat(result).hasSize(1)
        val summary = result[0].envs.find { it.env == "production" }
        assertThat(summary?.enabled).isTrue()
        assertThat(summary?.rollout).isEqualTo(42)
        assertThat(summary?.version).isEqualTo(3)
    }

    @Test
    fun `updateFlag delegates to service and maps the response`() {
        val f = flag(description = "updated")
        every { flagService.updateFlag("new-checkout", "updated") } returns f

        val result = controller.updateFlag("new-checkout", UpdateFlagRequest(description = "updated"))

        assertThat(result.description).isEqualTo("updated")
    }

    @Test
    fun `updateFlagEnv delegates arguments in order and maps the response`() {
        val f = flag()
        val e = env()
        val fe = FlagEnv(flag = f, env = e).apply { enabled = true; rollout = 10; version = 2 }
        every { flagService.updateFlagEnv("new-checkout", "production", true, 10, 1) } returns fe

        val result = controller.updateFlagEnv(
            "new-checkout",
            "production",
            UpdateFlagEnvRequest(enabled = true, rollout = 10, version = 1),
        )

        assertThat(result.flagName).isEqualTo(f.name)
        assertThat(result.env).isEqualTo("production")
        assertThat(result.enabled).isTrue()
        assertThat(result.rollout).isEqualTo(10)
        assertThat(result.version).isEqualTo(2)
    }

    @Test
    fun `getFlagEnv delegates to service and maps the response`() {
        val f = flag()
        val e = env()
        val fe = FlagEnv(flag = f, env = e).apply { enabled = false; rollout = 100; version = 1 }
        every { flagService.getFlagEnv("new-checkout", "production") } returns fe

        val result = controller.getFlagEnv("new-checkout", "production")

        assertThat(result.enabled).isFalse()
        assertThat(result.rollout).isEqualTo(100)
    }
}
