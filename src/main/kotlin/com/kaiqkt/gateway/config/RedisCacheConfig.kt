package com.kaiqkt.gateway.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import java.time.Duration


@Configuration
@EnableCaching
class RedisCacheConfig(
    @param:Value("\${policies-cache-ttl}")
    private val cacheTtl: Long
) {
    @Bean
    fun cacheConfiguration(): RedisCacheConfiguration {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(cacheTtl))
            .disableCachingNullValues()
    }
}