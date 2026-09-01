package com.featureflagmanager.sdk

import java.time.LocalTime
import java.time.format.DateTimeFormatter

private const val FLAG_NAME = "new-payment-flow"
private const val ENV = "staging"
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss")

fun main() {
    val client = FeatureFlagClient(baseUrl = "http://localhost:8080")

    println("Watching '$FLAG_NAME' in '$ENV' every 2s. Ctrl+C to stop.")
    println()
    println("To flip it live, in another terminal:")
    println("  curl -s http://localhost:8080/api/v1/flags/$FLAG_NAME/envs/$ENV   # copy the \"version\" field")
    println("  curl -s -X PATCH http://localhost:8080/api/v1/flags/$FLAG_NAME/envs/$ENV \\")
    println("    -H 'Content-Type: application/json' \\")
    println("    -d '{\"enabled\": false, \"version\": <version-from-above>}'")
    println()

    var lastKnown: Boolean? = null

    while (true) {
        val enabled = try {
            client.isEnabled(FLAG_NAME, ENV)
        } catch (e: FeatureFlagClientException) {
            println("[${now()}] failed to read flag: ${e.message}")
            Thread.sleep(2000)
            continue
        }

        val banner = if (enabled) "ON  -> showing new checkout" else "OFF -> showing old checkout"
        val changed = if (lastKnown != null && enabled != lastKnown) " (changed!)" else ""
        println("[${now()}] $FLAG_NAME = $banner$changed")
        lastKnown = enabled

        Thread.sleep(2000)
    }
}

private fun now(): String = LocalTime.now().format(timeFormat)
