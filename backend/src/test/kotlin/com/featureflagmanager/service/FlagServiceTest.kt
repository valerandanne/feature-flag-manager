package com.featureflagmanager.service

import com.featureflagmanager.entity.Environment
import com.featureflagmanager.entity.FeatureFlag
import com.featureflagmanager.entity.FlagEnv
import com.featureflagmanager.repository.EnvironmentRepository
import com.featureflagmanager.repository.FeatureFlagRepository
import com.featureflagmanager.repository.FlagEnvRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.atomic.AtomicLong

class FlagServiceTest {

    private val featureFlagRepository = mockk<FeatureFlagRepository>()
    private val flagEnvRepository = mockk<FlagEnvRepository>()
    private val environmentRepository = mockk<EnvironmentRepository>()

    private val service = FlagService(featureFlagRepository, flagEnvRepository, environmentRepository)
    private val idGen = AtomicLong(1)

    private fun flag(key: String = "new-checkout", name: String = "New Checkout", description: String? = "desc") =
        FeatureFlag(key = key, name = name, description = description).apply { id = idGen.getAndIncrement() }

    private fun env(name: String = "production") =
        Environment(name = name).apply { id = idGen.getAndIncrement() }

    private fun flagEnv(flag: FeatureFlag, env: Environment, version: Int = 1, persisted: Boolean = true) =
        FlagEnv(flag = flag, env = env).apply {
            if (persisted) id = idGen.getAndIncrement()
            this.version = version
        }

    // -------- createFlag --------

    @Test
    fun `createFlag saves a new flag when key is free`() {
        val prod = env("production")
        val staging = env("staging")

        every { featureFlagRepository.findByKey("new-checkout") } returns null
        val saved = slot<FeatureFlag>()
        every { featureFlagRepository.save(capture(saved)) } answers { saved.captured }
        every { environmentRepository.findAll() } returns listOf(prod, staging)
        val savedFlagEnvs = slot<List<FlagEnv>>()
        every { flagEnvRepository.saveAll(capture(savedFlagEnvs)) } answers { savedFlagEnvs.captured }

        val result = service.createFlag("new-checkout", "New Checkout", "desc")

        assertThat(result.key).isEqualTo("new-checkout")
        assertThat(result.name).isEqualTo("New Checkout")
        assertThat(result.description).isEqualTo("desc")
        assertThat(saved.captured.key).isEqualTo("new-checkout")
        assertThat(saved.captured.description).isEqualTo("desc")
        assertThat(savedFlagEnvs.captured).hasSize(2)
        assertThat(savedFlagEnvs.captured.map { it.env.name }).containsExactlyInAnyOrder("production", "staging")
        assertThat(savedFlagEnvs.captured).allSatisfy { assertThat(it.enabled).isFalse() }
    }

    @Test
    fun `createFlag rejects a duplicate key`() {
        every { featureFlagRepository.findByKey("new-checkout") } returns flag()

        assertThrows<InvalidRequestException> { service.createFlag("new-checkout", "New Checkout", "desc") }
        verify(exactly = 0) { featureFlagRepository.save(any()) }
    }

    // -------- listFlagsWithEnvs --------

    @Test
    fun `listFlagsWithEnvs returns flags with empty envs when no FlagEnv exists`() {
        val f = flag()
        every { featureFlagRepository.findAll() } returns listOf(f)
        every { flagEnvRepository.findAllWithFlagAndEnv() } returns emptyList()

        val result = service.listFlagsWithEnvs()

        assertThat(result).hasSize(1)
        assertThat(result[0].flag).isEqualTo(f)
        assertThat(result[0].envs).isEmpty()
    }

    @Test
    fun `listFlagsWithEnvs groups multiple envs per flag by env name`() {
        val f = flag()
        val prod = env("production")
        val staging = env("staging")
        val prodFe = flagEnv(f, prod)
        val stagingFe = flagEnv(f, staging)

        every { featureFlagRepository.findAll() } returns listOf(f)
        every { flagEnvRepository.findAllWithFlagAndEnv() } returns listOf(prodFe, stagingFe)

        val result = service.listFlagsWithEnvs()

        assertThat(result).hasSize(1)
        assertThat(result[0].envs).containsExactlyInAnyOrderEntriesOf(
            mapOf("production" to prodFe, "staging" to stagingFe),
        )
    }

    @Test
    fun `listFlagsWithEnvs ignores FlagEnv rows whose flag is not in the flags list`() {
        val f = flag()
        val orphanFlag = flag(key = "other-flag", name = "Other Flag")
        val prod = env("production")
        val orphanFe = flagEnv(orphanFlag, prod)

        every { featureFlagRepository.findAll() } returns listOf(f)
        every { flagEnvRepository.findAllWithFlagAndEnv() } returns listOf(orphanFe)

        val result = service.listFlagsWithEnvs()

        assertThat(result).hasSize(1)
        assertThat(result[0].envs).isEmpty()
    }

    // -------- updateFlag --------

    @Test
    fun `updateFlag overwrites description when a new value is given`() {
        val f = flag(description = "old")
        every { featureFlagRepository.findByKey("new-checkout") } returns f

        val result = service.updateFlag("new-checkout", null, "new")

        assertThat(result.description).isEqualTo("new")
    }

    @Test
    fun `updateFlag keeps existing description when null is given`() {
        val f = flag(description = "old")
        every { featureFlagRepository.findByKey("new-checkout") } returns f

        val result = service.updateFlag("new-checkout", null, null)

        assertThat(result.description).isEqualTo("old")
    }

    @Test
    fun `updateFlag overwrites name when a new value is given`() {
        val f = flag(name = "Old Name")
        every { featureFlagRepository.findByKey("new-checkout") } returns f

        val result = service.updateFlag("new-checkout", "New Name", null)

        assertThat(result.name).isEqualTo("New Name")
    }

    @Test
    fun `updateFlag keeps existing name when null is given`() {
        val f = flag(name = "Old Name")
        every { featureFlagRepository.findByKey("new-checkout") } returns f

        val result = service.updateFlag("new-checkout", null, null)

        assertThat(result.name).isEqualTo("Old Name")
    }

    @Test
    fun `updateFlag throws NotFound when flag does not exist`() {
        every { featureFlagRepository.findByKey("missing") } returns null

        assertThrows<NotFoundException> { service.updateFlag("missing", null, "new") }
    }

    // -------- updateFlagEnv --------

    @Test
    fun `updateFlagEnv updates enabled on an existing row`() {
        val f = flag()
        val e = env()
        val existing = flagEnv(f, e, version = 3).apply { enabled = false }
        every { flagEnvRepository.findByFlagKeyAndEnvName("new-checkout", "production") } returns existing
        every { flagEnvRepository.save(any()) } answers { firstArg() }

        val result = service.updateFlagEnv("new-checkout", "production", enabled = true)

        assertThat(result.enabled).isTrue()
    }

    @Test
    fun `updateFlagEnv throws NotFound when there is no config for that flag+env combo`() {
        every { flagEnvRepository.findByFlagKeyAndEnvName("missing", "production") } returns null

        assertThrows<NotFoundException> {
            service.updateFlagEnv("missing", "production", enabled = true)
        }
    }

    // -------- getFlagEnv --------

    @Test
    fun `getFlagEnv returns existing config`() {
        val f = flag()
        val e = env()
        val existing = flagEnv(f, e)
        every { flagEnvRepository.findByFlagKeyAndEnvName("new-checkout", "production") } returns existing

        assertThat(service.getFlagEnv("new-checkout", "production")).isEqualTo(existing)
    }

    @Test
    fun `getFlagEnv throws NotFound when there is no config for that flag+env combo`() {
        every { flagEnvRepository.findByFlagKeyAndEnvName("missing", "production") } returns null

        assertThrows<NotFoundException> { service.getFlagEnv("missing", "production") }
    }
}
