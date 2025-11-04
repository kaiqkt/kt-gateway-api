package com.kaiqkt.gateway.resources

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.isSuccessful
import com.kaiqkt.gateway.models.Client
import com.kaiqkt.gateway.models.Introspect
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component

@Component
class AuthenticationClient(
    @param:Value($$"${authentication-service-url}")
    private val authenticationServiceUrl: String,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(AuthenticationClient::class.java)

    fun findClientById(clientId: String): Client? {
        try {
            log.info("Searching for client $clientId")

            val (_, response, result) = Fuel.get("$authenticationServiceUrl/v1/clients/$clientId").response()

            if (response.isSuccessful) {
                return objectMapper.readValue(result.get(), Client::class.java)
            }

            log.info("Search client $clientId responded ${response.statusCode}")
        } catch (ex: Exception) {
            log.error("Error occurred trying to get policies", ex)
        }

        return null
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