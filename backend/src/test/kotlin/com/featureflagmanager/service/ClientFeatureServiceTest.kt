package com.featureflagmanager.service

import com.featureflagmanager.entity.Environment
import com.featureflagmanager.entity.FeatureFlag
import com.featureflagmanager.entity.FlagEnv
import com.featureflagmanager.repository.EnvironmentRepository
import com.featureflagmanager.repository.FeatureFlagRepository
import com.featureflagmanager.repository.FlagEnvRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class ClientFeatureServiceTest {

    private val featureFlagRepository = mockk<FeatureFlagRepository>()
    private val flagEnvRepository = mockk<FlagEnvRepository>()
    private val environmentRepository = mockk<EnvironmentRepository>()

    private val service = ClientFeatureService(featureFlagRepository, flagEnvRepository, environmentRepository)

    private fun flag(name: String) = FeatureFlag(name = name).apply { id = UUID.randomUUID() }

    private fun env(name: String = "production") = Environment(name = name).apply { id = UUID.randomUUID() }

    @Test
    fun `resolveFeatures reports enabled state from the FlagEnv config`() {
        val prod = env()
        val f = flag("new-checkout")
        val fe = FlagEnv(flag = f, env = prod).apply { enabled = true }

        every { environmentRepository.findByName("production") } returns prod
        every { featureFlagRepository.findAll() } returns listOf(f)
        every { flagEnvRepository.findAllByEnvId(prod.id!!) } returns listOf(fe)

        val result = service.resolveFeatures("production")

        assertThat(result.env).isEqualTo("production")
        assertThat(result.features).containsExactly(com.featureflagmanager.controller.ClientFeature("new-checkout", true))
    }

    @Test
    fun `resolveFeatures defaults to disabled when no FlagEnv config exists for the env`() {
        val prod = env()
        val f = flag("new-checkout")

        every { environmentRepository.findByName("production") } returns prod
        every { featureFlagRepository.findAll() } returns listOf(f)
        every { flagEnvRepository.findAllByEnvId(prod.id!!) } returns emptyList()

        val result = service.resolveFeatures("production")

        assertThat(result.features).containsExactly(com.featureflagmanager.controller.ClientFeature("new-checkout", false))
    }

    @Test
    fun `resolveFeatures returns empty features when there are no flags`() {
        val prod = env()
        every { environmentRepository.findByName("production") } returns prod
        every { featureFlagRepository.findAll() } returns emptyList()
        every { flagEnvRepository.findAllByEnvId(prod.id!!) } returns emptyList()

        val result = service.resolveFeatures("production")

        assertThat(result.env).isEqualTo("production")
        assertThat(result.features).isEmpty()
    }

    @Test
    fun `resolveFeatures mixes flags with and without config correctly`() {
        val prod = env()
        val onFlag = flag("on-flag")
        val offFlag = flag("off-flag")
        val onFe = FlagEnv(flag = onFlag, env = prod).apply { enabled = true }

        every { environmentRepository.findByName("production") } returns prod
        every { featureFlagRepository.findAll() } returns listOf(onFlag, offFlag)
        every { flagEnvRepository.findAllByEnvId(prod.id!!) } returns listOf(onFe)

        val result = service.resolveFeatures("production")

        assertThat(result.features).containsExactly(
            com.featureflagmanager.controller.ClientFeature("on-flag", true),
            com.featureflagmanager.controller.ClientFeature("off-flag", false),
        )
    }

    @Test
    fun `resolveFeatures rejects an unknown env`() {
        every { environmentRepository.findByName("nope") } returns null

        assertThrows<InvalidRequestException> { service.resolveFeatures("nope") }
    }

    @Test
    fun `isEnabled returns the FlagEnv enabled state`() {
        val prod = env()
        val f = flag("new-checkout")
        val fe = FlagEnv(flag = f, env = prod).apply { enabled = true }

        every { featureFlagRepository.findByName("new-checkout") } returns f
        every { environmentRepository.findByName("production") } returns prod
        every { flagEnvRepository.findByFlagIdAndEnvId(f.id!!, prod.id!!) } returns fe

        assertThat(service.isEnabled("new-checkout", "production")).isTrue()
    }

    @Test
    fun `isEnabled defaults to disabled when no FlagEnv config exists for the env`() {
        val prod = env()
        val f = flag("new-checkout")

        every { featureFlagRepository.findByName("new-checkout") } returns f
        every { environmentRepository.findByName("production") } returns prod
        every { flagEnvRepository.findByFlagIdAndEnvId(f.id!!, prod.id!!) } returns null

        assertThat(service.isEnabled("new-checkout", "production")).isFalse()
    }

    @Test
    fun `isEnabled rejects an unknown flag`() {
        every { featureFlagRepository.findByName("nope") } returns null

        assertThrows<NotFoundException> { service.isEnabled("nope", "production") }
    }

    @Test
    fun `isEnabled rejects an unknown env`() {
        val f = flag("new-checkout")
        every { featureFlagRepository.findByName("new-checkout") } returns f
        every { environmentRepository.findByName("nope") } returns null

        assertThrows<InvalidRequestException> { service.isEnabled("new-checkout", "nope") }
    }
}
