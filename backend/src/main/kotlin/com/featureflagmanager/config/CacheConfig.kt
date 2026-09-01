package com.featureflagmanager.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

const val CLIENT_FEATURES_CACHE = "clientFeatures"
const val FLAG_ENABLED_CACHE = "flagEnabled"

@Configuration
@EnableCaching
class CacheConfig {
    @Bean
    fun cacheManager(): CacheManager =
        CaffeineCacheManager(CLIENT_FEATURES_CACHE, FLAG_ENABLED_CACHE).apply {
            setCaffeine(Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS))
        }
}
