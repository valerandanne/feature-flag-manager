package com.featureflagmanager.controller

import com.featureflagmanager.service.ClientFlagService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

/**
 * Read-only endpoints used by client applications to resolve flag state for a given environment.
 * Flags are managed separately via [AdminController]; these endpoints never mutate state.
 */
@RestController
@Tag(name = "Client", description = "Resolve feature flag state for an environment at runtime")
class ClientController(
        private val clientFlagService: ClientFlagService,
) {
    /** Resolves the enabled state of every flag for the given environment. */
    @Operation(summary = "List all flags resolved for one environment")
    @GetMapping("/api/v1/client/flags/environments/{env}")
    fun listFlags(
            @PathVariable env: String,
    ): ClientFlagsResponse = clientFlagService.resolveFlags(env)

    /** Resolves the enabled state of a single flag for the given environment. */
    @Operation(summary = "Get one flag's resolved state for one environment")
    @ApiResponse(responseCode = "404", description = "Flag or environment not found")
    @GetMapping("/api/v1/client/flags/{flagKey}/environments/{env}")
    fun getFlag(
            @PathVariable flagKey: String,
            @PathVariable env: String,
    ): ClientFlag = clientFlagService.getFlag(flagKey, env)
}
