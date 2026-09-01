package com.featureflagmanager.sdk

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Serializable
data class ClientFeature(
    val name: String,
    val enabled: Boolean,
)

@Serializable
data class ClientFeaturesResponse(
    val env: String,
    val features: List<ClientFeature>,
)

class FeatureFlagClientException(message: String) : RuntimeException(message)

class FeatureFlagClient(
    private val baseUrl: String,
    private val httpClient: HttpClient = HttpClient.newHttpClient(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun isEnabled(name: String, env: String = "production"): Boolean {
        val body = get("/api/v1/client/features/$name?env=$env")
        return json.decodeFromString(ClientFeature.serializer(), body).enabled
    }

    fun getFeatures(env: String = "production"): List<ClientFeature> {
        val body = get("/api/v1/client/features?env=$env")
        return json.decodeFromString(ClientFeaturesResponse.serializer(), body).features
    }

    private fun get(path: String): String {
        val request = HttpRequest.newBuilder(URI.create("$baseUrl$path"))
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw FeatureFlagClientException("Request to $path failed: ${response.statusCode()} ${response.body()}")
        }
        return response.body()
    }
}
