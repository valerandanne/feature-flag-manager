package com.featureflagmanager.security

import com.featureflagmanager.repository.ApiKeyRepository
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

const val CLIENT_API_PATH = "/api/v1/client/features"
const val PROJECT_REQUEST_ATTRIBUTE = "authenticatedProjectId"

@Component
class ApiKeyAuthFilter(
    private val apiKeyRepository: ApiKeyRepository,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.requestURI != CLIENT_API_PATH

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val auth = request.getHeader("Authorization")
        if (auth == null || !auth.startsWith("Bearer ")) {
            respondUnauthorized(response, "Missing api key")
            return
        }
        val key = auth.removePrefix("Bearer ")
        val apiKey = apiKeyRepository.findByKey(key)
        if (apiKey == null) {
            respondUnauthorized(response, "Invalid api key")
            return
        }
        request.setAttribute(PROJECT_REQUEST_ATTRIBUTE, apiKey.project.id)
        filterChain.doFilter(request, response)
    }

    private fun respondUnauthorized(response: HttpServletResponse, message: String) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json"
        response.writer.write(objectMapper.writeValueAsString(mapOf("error" to message)))
    }
}
