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
import java.util.concurrent.atomic.AtomicLong

class ClientFlagServiceTest {

    private val featureFlagRepository = mockk<FeatureFlagRepository>()
    private val flagEnvRepository = mockk<FlagEnvRepository>()
    private val environmentRepository = mockk<EnvironmentRepository>()

    private val service = ClientFlagService(featureFlagRepository, flagEnvRepository, environmentRepository)
    private val idGen = AtomicLong(1)

    private fun flag(key: String, name: String = key) =
        FeatureFlag(key = key, name = name).apply { id = idGen.getAndIncrement() }

    private fun env(name: String = "production") = Environment(name = name).apply { id = idGen.getAndIncrement() }

    @Test
    fun `resolveFlags reports enabled state from the FlagEnv config`() {
        val prod = env()
        val f = flag("new-checkout", "New Checkout")
        val fe = FlagEnv(flag = f, env = prod).apply { enabled = true }

        every { environmentRepository.findByName("production") } returns prod
        every { featureFlagRepository.findAll() } returns listOf(f)
        every { flagEnvRepository.findAllByEnvId(prod.id!!) } returns listOf(fe)

        val result = service.resolveFlags("production")

        assertThat(result.env).isEqualTo("production")
        assertThat(result.flags).containsExactly(
            com.featureflagmanager.controller.ClientFlag("new-checkout", "New Checkout", true),
        )
    }

    @Test
    fun `resolveFlags defaults to disabled when no FlagEnv config exists for the env`() {
        val prod = env()
        val f = flag("new-checkout", "New Checkout")

        every { environmentRepository.findByName("production") } returns prod
        every { featureFlagRepository.findAll() } returns listOf(f)
        every { flagEnvRepository.findAllByEnvId(prod.id!!) } returns emptyList()

        val result = service.resolveFlags("production")

        assertThat(result.flags).containsExactly(
            com.featureflagmanager.controller.ClientFlag("new-checkout", "New Checkout", false),
        )
    }

    @Test
    fun `resolveFlags returns empty flags when there are no flags`() {
        val prod = env()
        every { environmentRepository.findByName("production") } returns prod
        every { featureFlagRepository.findAll() } returns emptyList()
        every { flagEnvRepository.findAllByEnvId(prod.id!!) } returns emptyList()

        val result = service.resolveFlags("production")

        assertThat(result.env).isEqualTo("production")
        assertThat(result.flags).isEmpty()
    }

    @Test
    fun `resolveFlags mixes flags with and without config correctly`() {
        val prod = env()
        val onFlag = flag("on-flag", "On Flag")
        val offFlag = flag("off-flag", "Off Flag")
        val onFe = FlagEnv(flag = onFlag, env = prod).apply { enabled = true }

        every { environmentRepository.findByName("production") } returns prod
        every { featureFlagRepository.findAll() } returns listOf(onFlag, offFlag)
        every { flagEnvRepository.findAllByEnvId(prod.id!!) } returns listOf(onFe)

        val result = service.resolveFlags("production")

        assertThat(result.flags).containsExactly(
            com.featureflagmanager.controller.ClientFlag("on-flag", "On Flag", true),
            com.featureflagmanager.controller.ClientFlag("off-flag", "Off Flag", false),
        )
    }

    @Test
    fun `resolveFlags rejects an unknown env`() {
        every { environmentRepository.findByName("nope") } returns null

        assertThrows<InvalidRequestException> { service.resolveFlags("nope") }
    }

    @Test
    fun `getFlag returns the FlagEnv enabled state and the flag's name`() {
        val prod = env()
        val f = flag("new-checkout", "New Checkout")
        val fe = FlagEnv(flag = f, env = prod).apply { enabled = true }

        every { featureFlagRepository.findByKey("new-checkout") } returns f
        every { environmentRepository.findByName("production") } returns prod
        every { flagEnvRepository.findByFlagIdAndEnvId(f.id!!, prod.id!!) } returns fe

        val result = service.getFlag("new-checkout", "production")

        assertThat(result).isEqualTo(com.featureflagmanager.controller.ClientFlag("new-checkout", "New Checkout", true))
    }

    @Test
    fun `getFlag defaults to disabled when no FlagEnv config exists for the env`() {
        val prod = env()
        val f = flag("new-checkout", "New Checkout")

        every { featureFlagRepository.findByKey("new-checkout") } returns f
        every { environmentRepository.findByName("production") } returns prod
        every { flagEnvRepository.findByFlagIdAndEnvId(f.id!!, prod.id!!) } returns null

        val result = service.getFlag("new-checkout", "production")

        assertThat(result.enabled).isFalse()
    }

    @Test
    fun `getFlag rejects an unknown flag`() {
        every { featureFlagRepository.findByKey("nope") } returns null

        assertThrows<NotFoundException> { service.getFlag("nope", "production") }
    }

    @Test
    fun `getFlag rejects an unknown env`() {
        val f = flag("new-checkout", "New Checkout")
        every { featureFlagRepository.findByKey("new-checkout") } returns f
        every { environmentRepository.findByName("nope") } returns null

        assertThrows<InvalidRequestException> { service.getFlag("new-checkout", "nope") }
    }
}
