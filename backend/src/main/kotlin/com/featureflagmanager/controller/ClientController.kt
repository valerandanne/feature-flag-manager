package com.featureflagmanager.controller

import com.featureflagmanager.repository.ProjectRepository
import com.featureflagmanager.security.PROJECT_REQUEST_ATTRIBUTE
import com.featureflagmanager.service.ClientFeatureService
import com.featureflagmanager.service.UnauthorizedException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class ClientController(
    private val clientFeatureService: ClientFeatureService,
    private val projectRepository: ProjectRepository,
) {
    @GetMapping("/api/v1/client/features")
    fun getFeatures(
        request: HttpServletRequest,
        @RequestParam(defaultValue = "production") env: String,
    ): ClientFeaturesResponse {
        val projectId = request.getAttribute(PROJECT_REQUEST_ATTRIBUTE) as? UUID
            ?: throw UnauthorizedException("no project for key")
        val project = projectRepository.findById(projectId)
            .orElseThrow { UnauthorizedException("no project for key") }
        return clientFeatureService.resolveFeatures(project, env)
    }
}
