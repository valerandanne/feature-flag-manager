package com.featureflagmanager.controller

import com.featureflagmanager.service.ClientFeatureService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ClientControllerTest {

    private val clientFeatureService = mockk<ClientFeatureService>()
    private val controller = ClientController(clientFeatureService)

    @Test
    fun `getFeatures delegates to the service with the given env`() {
        val response = ClientFeaturesResponse(env = "staging", features = listOf(ClientFeature("f1", true)))
        every { clientFeatureService.resolveFeatures("staging") } returns response

        val result = controller.getFeatures(env = "staging")

        assertThat(result).isEqualTo(response)
    }

    @Test
    fun `getFeatures defaults to production when env is not supplied`() {
        val response = ClientFeaturesResponse(env = "production", features = emptyList())
        every { clientFeatureService.resolveFeatures("production") } returns response

        controller.getFeatures(env = "production")

        verify { clientFeatureService.resolveFeatures("production") }
    }
}
