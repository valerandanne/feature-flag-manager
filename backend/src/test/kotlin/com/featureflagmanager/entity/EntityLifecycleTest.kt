package com.featureflagmanager.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class EntityLifecycleTest {

    @Test
    fun `FeatureFlag onUpdate bumps updatedAt`() {
        val flag = FeatureFlag(key = "f", name = "F").apply { updatedAt = Instant.EPOCH }

        flag.onUpdate()

        assertThat(flag.updatedAt).isAfter(Instant.EPOCH)
    }

    @Test
    fun `FlagEnv onUpdate bumps updatedAt`() {
        val flagEnv = FlagEnv(flag = FeatureFlag(key = "f", name = "F"), env = Environment(name = "production"))
            .apply { updatedAt = Instant.EPOCH }

        flagEnv.onUpdate()

        assertThat(flagEnv.updatedAt).isAfter(Instant.EPOCH)
    }
}
