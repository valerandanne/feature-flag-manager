package com.featureflagmanager.controller

import com.featureflagmanager.service.ClientFeatureService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ClientController(
    private val clientFeatureService: ClientFeatureService,
) {
    @GetMapping("/api/v1/client/features")
    fun getFeatures(
        @RequestParam(defaultValue = "production") env: String,
    ): ClientFeaturesResponse =
        clientFeatureService.resolveFeatures(env)

    @GetMapping("/api/v1/client/features/{name}")
    fun getFeature(
        @PathVariable name: String,
        @RequestParam(defaultValue = "production") env: String,
    ): ClientFeature =
        ClientFeature(name = name, enabled = clientFeatureService.isEnabled(name, env))
}
