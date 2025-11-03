package com.kaiqkt.gateway.resources

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.isSuccessful
import com.kaiqkt.gateway.models.Introspect
import com.kaiqkt.gateway.models.Policy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component

@Component
class AuthenticationClient(
    @param:Value("\${authentication-service-url}")
    private val authenticationServiceUrl: String,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(AuthenticationClient::class.java)

    fun findAllPolicies(resourceServerId: String): List<Policy> {
        try {
            log.info("Searching policies for resource server $resourceServerId")

            val (_, response, result) = Fuel.get("$authenticationServiceUrl/v1/resources/$resourceServerId/policies").response()

            if (response.isSuccessful) {
                return objectMapper.readValue(result.get(), object : TypeReference<List<Policy>>() {})
            }

            log.info("Search policies responded for resource $resourceServerId responded with ${response.statusCode}")
        } catch (ex: Exception) {
            log.error("Error occurred trying to get policies", ex)
        }

        return listOf()
    }

    fun introspect(accessToken: String): Introspect? {
        try {
            val (_, response, result) = Fuel.get("$authenticationServiceUrl/v1/oauth/introspect")
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .response()

            if (response.isSuccessful) {
                return objectMapper.readValue(result.get(), Introspect::class.java)
            }
        } catch (ex: Exception) {
            log.error("Error occurred trying introspect", ex)
        }

        return null
    }
}