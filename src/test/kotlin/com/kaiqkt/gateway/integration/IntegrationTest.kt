package com.kaiqkt.gateway.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.kaiqkt.gateway.unit.resources.helpers.AuthenticationHelper
import com.redis.testcontainers.RedisContainer
import io.restassured.RestAssured
import io.restassured.config.ObjectMapperConfig
import io.restassured.mapper.ObjectMapperType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.cache.CacheManager
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationTest {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var mapper: ObjectMapper

    @Autowired
    private lateinit var cacheManager: CacheManager

    @BeforeAll
    fun beforeAll() {
        RestAssured.config = RestAssured.config()
            .objectMapperConfig(
                ObjectMapperConfig(ObjectMapperType.JACKSON_2)
                    .jackson2ObjectMapperFactory { _, _ -> mapper }
            )

        RestAssured.baseURI = "http://localhost:$port"
    }

    @BeforeEach
    fun beforeEach(){
        AuthenticationHelper.reset()
        cacheManager.cacheNames.stream()
            .forEach { cacheName -> cacheManager.getCache(cacheName)?.clear() }
    }


    companion object {
        @Container
        private val redisContainer = RedisContainer("redis:7.2.4-alpine").apply {
            withExposedPorts(6379)
            start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerRedisProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }
        }
    }
}