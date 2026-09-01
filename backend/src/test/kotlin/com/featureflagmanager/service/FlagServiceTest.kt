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
import java.util.UUID

class FlagServiceTest {

    private val featureFlagRepository = mockk<FeatureFlagRepository>()
    private val flagEnvRepository = mockk<FlagEnvRepository>()
    private val environmentRepository = mockk<EnvironmentRepository>()

    private val service = FlagService(featureFlagRepository, flagEnvRepository, environmentRepository)

    private fun flag(name: String = "new-checkout", description: String? = "desc") =
        FeatureFlag(name = name, description = description).apply { id = UUID.randomUUID() }

    private fun env(name: String = "production") =
        Environment(name = name).apply { id = UUID.randomUUID() }

    private fun flagEnv(flag: FeatureFlag, env: Environment, version: Int = 1, persisted: Boolean = true) =
        FlagEnv(flag = flag, env = env).apply {
            if (persisted) id = UUID.randomUUID()
            this.version = version
        }

    // -------- createFlag --------

    @Test
    fun `createFlag saves a new flag when name is free`() {
        val prod = env("production")
        val staging = env("staging")

        every { featureFlagRepository.findByName("new-checkout") } returns null
        val saved = slot<FeatureFlag>()
        every { featureFlagRepository.save(capture(saved)) } answers { saved.captured }
        every { environmentRepository.findAll() } returns listOf(prod, staging)
        val savedFlagEnvs = slot<List<FlagEnv>>()
        every { flagEnvRepository.saveAll(capture(savedFlagEnvs)) } answers { savedFlagEnvs.captured }

        val result = service.createFlag("new-checkout", "desc")

        assertThat(result.name).isEqualTo("new-checkout")
        assertThat(result.description).isEqualTo("desc")
        assertThat(saved.captured.name).isEqualTo("new-checkout")
        assertThat(saved.captured.description).isEqualTo("desc")
        assertThat(savedFlagEnvs.captured).hasSize(2)
        assertThat(savedFlagEnvs.captured.map { it.env.name }).containsExactlyInAnyOrder("production", "staging")
        assertThat(savedFlagEnvs.captured).allSatisfy { assertThat(it.enabled).isFalse() }
    }

    @Test
    fun `createFlag rejects a duplicate name`() {
        every { featureFlagRepository.findByName("new-checkout") } returns flag()

        assertThrows<InvalidRequestException> { service.createFlag("new-checkout", "desc") }
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
        val orphanFlag = flag(name = "other-flag")
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
        every { featureFlagRepository.findByName("new-checkout") } returns f

        val result = service.updateFlag("new-checkout", "new")

        assertThat(result.description).isEqualTo("new")
    }

    @Test
    fun `updateFlag keeps existing description when null is given`() {
        val f = flag(description = "old")
        every { featureFlagRepository.findByName("new-checkout") } returns f

        val result = service.updateFlag("new-checkout", null)

        assertThat(result.description).isEqualTo("old")
    }

    @Test
    fun `updateFlag throws NotFound when flag does not exist`() {
        every { featureFlagRepository.findByName("missing") } returns null

        assertThrows<NotFoundException> { service.updateFlag("missing", "new") }
    }

    // -------- updateFlagEnv --------

    @Test
    fun `updateFlagEnv creates a FlagEnv when none exists yet, ignoring expectedVersion`() {
        val f = flag()
        val e = env()
        every { featureFlagRepository.findByName("new-checkout") } returns f
        every { environmentRepository.findByName("production") } returns e
        every { flagEnvRepository.findByFlagIdAndEnvId(f.id!!, e.id!!) } returns null
        every { flagEnvRepository.save(any()) } answers { firstArg() }

        val result = service.updateFlagEnv("new-checkout", "production", enabled = true, rollout = 50, expectedVersion = 999)

        assertThat(result.enabled).isTrue()
        assertThat(result.rollout).isEqualTo(50)
    }

    @Test
    fun `updateFlagEnv updates only enabled when rollout is null`() {
        val f = flag()
        val e = env()
        val existing = flagEnv(f, e, version = 3).apply { enabled = false; rollout = 20 }
        every { featureFlagRepository.findByName("new-checkout") } returns f
        every { environmentRepository.findByName("production") } returns e
        every { flagEnvRepository.findByFlagIdAndEnvId(f.id!!, e.id!!) } returns existing
        every { flagEnvRepository.save(any()) } answers { firstArg() }

        val result = service.updateFlagEnv("new-checkout", "production", enabled = true, rollout = null, expectedVersion = 3)

        assertThat(result.enabled).isTrue()
        assertThat(result.rollout).isEqualTo(20)
    }

    @Test
    fun `updateFlagEnv updates only rollout when enabled is null`() {
        val f = flag()
        val e = env()
        val existing = flagEnv(f, e, version = 3).apply { enabled = true; rollout = 20 }
        every { featureFlagRepository.findByName("new-checkout") } returns f
        every { environmentRepository.findByName("production") } returns e
        every { flagEnvRepository.findByFlagIdAndEnvId(f.id!!, e.id!!) } returns existing
        every { flagEnvRepository.save(any()) } answers { firstArg() }

        val result = service.updateFlagEnv("new-checkout", "production", enabled = null, rollout = 77, expectedVersion = 3)

        assertThat(result.enabled).isTrue()
        assertThat(result.rollout).isEqualTo(77)
    }

    @Test
    fun `updateFlagEnv rejects when both enabled and rollout are null`() {
        val ex = assertThrows<InvalidRequestException> {
            service.updateFlagEnv("new-checkout", "production", enabled = null, rollout = null, expectedVersion = 3)
        }
        assertThat(ex.code).isEqualTo("NO_FIELDS_TO_UPDATE")
        verify(exactly = 0) { flagEnvRepository.save(any()) }
    }

    @Test
    fun `updateFlagEnv throws VersionConflict when expectedVersion is stale on an existing row`() {
        val f = flag()
        val e = env()
        val existing = flagEnv(f, e, version = 5)
        every { featureFlagRepository.findByName("new-checkout") } returns f
        every { environmentRepository.findByName("production") } returns e
        every { flagEnvRepository.findByFlagIdAndEnvId(f.id!!, e.id!!) } returns existing

        val ex = assertThrows<VersionConflictException> {
            service.updateFlagEnv("new-checkout", "production", enabled = false, rollout = null, expectedVersion = 4)
        }

        @Suppress("UNCHECKED_CAST")
        val current = ex.current as Map<String, Any?>
        assertThat(current["version"]).isEqualTo(5)
        assertThat(current["env"]).isEqualTo("production")
        verify(exactly = 0) { flagEnvRepository.save(any()) }
    }

    @Test
    fun `updateFlagEnv throws NotFound when flag does not exist`() {
        every { featureFlagRepository.findByName("missing") } returns null

        assertThrows<NotFoundException> {
            service.updateFlagEnv("missing", "production", enabled = true, rollout = null, expectedVersion = 1)
        }
    }

    @Test
    fun `updateFlagEnv throws NotFound when env does not exist`() {
        every { featureFlagRepository.findByName("new-checkout") } returns flag()
        every { environmentRepository.findByName("nope") } returns null

        assertThrows<NotFoundException> {
            service.updateFlagEnv("new-checkout", "nope", enabled = true, rollout = null, expectedVersion = 1)
        }
    }

    // -------- getFlagEnv --------

    @Test
    fun `getFlagEnv returns existing config`() {
        val f = flag()
        val e = env()
        val existing = flagEnv(f, e)
        every { featureFlagRepository.findByName("new-checkout") } returns f
        every { environmentRepository.findByName("production") } returns e
        every { flagEnvRepository.findByFlagIdAndEnvId(f.id!!, e.id!!) } returns existing

        assertThat(service.getFlagEnv("new-checkout", "production")).isEqualTo(existing)
    }

    @Test
    fun `getFlagEnv throws NotFound when flag does not exist`() {
        every { featureFlagRepository.findByName("missing") } returns null

        assertThrows<NotFoundException> { service.getFlagEnv("missing", "production") }
    }

    @Test
    fun `getFlagEnv throws NotFound when env does not exist`() {
        every { featureFlagRepository.findByName("new-checkout") } returns flag()
        every { environmentRepository.findByName("nope") } returns null

        assertThrows<NotFoundException> { service.getFlagEnv("new-checkout", "nope") }
    }

    @Test
    fun `getFlagEnv throws NotFound when flag and env exist but no config row does`() {
        val f = flag()
        val e = env()
        every { featureFlagRepository.findByName("new-checkout") } returns f
        every { environmentRepository.findByName("production") } returns e
        every { flagEnvRepository.findByFlagIdAndEnvId(f.id!!, e.id!!) } returns null

        assertThrows<NotFoundException> { service.getFlagEnv("new-checkout", "production") }
    }
}
