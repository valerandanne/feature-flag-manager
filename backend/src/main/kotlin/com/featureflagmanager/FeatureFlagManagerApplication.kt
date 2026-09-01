package com.featureflagmanager

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FeatureFlagManagerApplication

fun main(args: Array<String>) {
    runApplication<FeatureFlagManagerApplication>(*args)
}
