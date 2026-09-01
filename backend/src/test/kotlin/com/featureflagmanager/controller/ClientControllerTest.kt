package com.featureflagmanager.controller

import com.featureflagmanager.service.ClientFlagService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ClientControllerTest {

    private val clientFlagService = mockk<ClientFlagService>()
    private val controller = ClientController(clientFlagService)

    @Test
    fun `listFlags delegates to the service with the given env`() {
        val response = ClientFlagsResponse(env = "staging", flags = listOf(ClientFlag("f1", "F1", true)))
        every { clientFlagService.resolveFlags("staging") } returns response

        val result = controller.listFlags(env = "staging")

        assertThat(result).isEqualTo(response)
    }

    @Test
    fun `getFlag delegates to the service for the given flag and env`() {
        val flag = ClientFlag("f1", "F1", true)
        every { clientFlagService.getFlag("f1", "production") } returns flag

        val result = controller.getFlag(flagKey = "f1", env = "production")

        assertThat(result).isEqualTo(flag)
        verify { clientFlagService.getFlag("f1", "production") }
    }
}
