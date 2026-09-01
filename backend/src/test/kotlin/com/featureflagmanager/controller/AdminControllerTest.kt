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
import java.util.concurrent.atomic.AtomicLong

class AdminControllerTest {

    private val flagService = mockk<FlagService>()
    private val controller = AdminController(flagService)
    private val idGen = AtomicLong(1)

    private fun flag(key: String = "new-checkout", name: String = "New Checkout", description: String? = "desc") =
        FeatureFlag(key = key, name = name, description = description).apply { id = idGen.getAndIncrement() }

    private fun env(name: String = "production") = Environment(name = name).apply { id = idGen.getAndIncrement() }

    @Test
    fun `createFlag returns 201 with the created flag`() {
        val f = flag()
        every { flagService.createFlag("new-checkout", "New Checkout", "desc") } returns f

        val response = controller.createFlag(
            CreateFlagRequest(key = "new-checkout", name = "New Checkout", description = "desc"),
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body?.key).isEqualTo("new-checkout")
        assertThat(response.body?.name).isEqualTo("New Checkout")
        assertThat(response.body?.description).isEqualTo("desc")
        assertThat(response.body?.envs).isEmpty()
    }

    @Test
    fun `listFlags maps envs by env name into FlagEnvSummary`() {
        val f = flag()
        val e = env()
        val fe = FlagEnv(flag = f, env = e).apply { enabled = true; version = 3 }
        every { flagService.listFlagsWithEnvs() } returns listOf(FlagWithEnvs(f, mapOf("production" to fe)))

        val result = controller.listFlags()

        assertThat(result).hasSize(1)
        val summary = result[0].envs.find { it.env == "production" }
        assertThat(summary?.enabled).isTrue()
        assertThat(summary?.version).isEqualTo(3)
    }

    @Test
    fun `updateFlagMetadata delegates to service and maps the response`() {
        val f = flag(description = "updated")
        every { flagService.updateFlag("new-checkout", null, "updated") } returns f

        val result = controller.updateFlagMetadata("new-checkout", UpdateFlagRequest(description = "updated"))

        assertThat(result.description).isEqualTo("updated")
    }

    @Test
    fun `updateFlagMetadata passes name through to the service`() {
        val f = flag(name = "New Name")
        every { flagService.updateFlag("new-checkout", "New Name", null) } returns f

        val result = controller.updateFlagMetadata("new-checkout", UpdateFlagRequest(name = "New Name"))

        assertThat(result.name).isEqualTo("New Name")
    }

    @Test
    fun `updateFlagEnvironmentStatus delegates arguments in order and maps the response`() {
        val f = flag()
        val e = env()
        val fe = FlagEnv(flag = f, env = e).apply { enabled = true; version = 2 }
        every { flagService.updateFlagEnv("new-checkout", "production", true) } returns fe

        val result = controller.updateFlagEnvironmentStatus(
            "new-checkout",
            "production",
            UpdateFlagEnvRequest(enabled = true),
        )

        assertThat(result.flagKey).isEqualTo(f.key)
        assertThat(result.env).isEqualTo("production")
        assertThat(result.enabled).isTrue()
        assertThat(result.version).isEqualTo(2)
    }

    @Test
    fun `getFlagEnv delegates to service and maps the response`() {
        val f = flag()
        val e = env()
        val fe = FlagEnv(flag = f, env = e).apply { enabled = false; version = 1 }
        every { flagService.getFlagEnv("new-checkout", "production") } returns fe

        val result = controller.getFlagEnv("new-checkout", "production")

        assertThat(result.enabled).isFalse()
    }
}
