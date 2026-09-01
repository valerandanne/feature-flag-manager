package com.featureflagmanager.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class EntityLifecycleTest {

    @Test
    fun `FeatureFlag onUpdate bumps updatedAt`() {
        val flag = FeatureFlag(name = "f").apply { updatedAt = Instant.EPOCH }

        flag.onUpdate()

        assertThat(flag.updatedAt).isAfter(Instant.EPOCH)
    }

    @Test
    fun `FlagEnv onUpdate bumps updatedAt`() {
        val flagEnv = FlagEnv(flag = FeatureFlag(name = "f"), env = Environment(name = "production"))
            .apply { updatedAt = Instant.EPOCH }

        flagEnv.onUpdate()

        assertThat(flagEnv.updatedAt).isAfter(Instant.EPOCH)
    }
}
