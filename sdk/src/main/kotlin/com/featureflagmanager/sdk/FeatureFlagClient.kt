package com.featureflagmanager.sdk

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Serializable
data class ClientFlag(
    val key: String,
    val name: String,
    val enabled: Boolean,
)

@Serializable
data class ClientFlagsResponse(
    val env: String,
    val flags: List<ClientFlag>,
)

class FeatureFlagClientException(message: String) : RuntimeException(message)

class FeatureFlagClient(
    private val baseUrl: String,
    private val httpClient: HttpClient = HttpClient.newHttpClient(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun isEnabled(key: String, env: String = "production"): Boolean {
        val body = get("/api/v1/client/flags/$key/environments/$env")
        return json.decodeFromString(ClientFlag.serializer(), body).enabled
    }

    fun getFlags(env: String = "production"): List<ClientFlag> {
        val body = get("/api/v1/client/flags/environments/$env")
        return json.decodeFromString(ClientFlagsResponse.serializer(), body).flags
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
