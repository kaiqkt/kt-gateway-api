package com.kaiqkt.gateway.unit.resources

import com.kaiqkt.gateway.config.ObjectMapperConfig
import com.kaiqkt.gateway.resources.AuthenticationClient
import com.kaiqkt.gateway.unit.models.IntrospectSampler
import com.kaiqkt.gateway.unit.resources.helpers.AuthenticationHelper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertNull
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthenticationClientTest {

    private val authenticationUrl = AuthenticationHelper.baseUrl()
    private val objectMapper = ObjectMapperConfig().objectMapper()
    private val authenticationClient = AuthenticationClient(authenticationUrl, objectMapper)

    @BeforeEach
    fun beforeEach() {
        AuthenticationHelper.reset()
    }

    @Test
    fun `given a client id should return successfully`() {
        AuthenticationHelper.mockSuccessfullyFoundClient("client-id")

        val client = authenticationClient.findClientById("client-id")

        assertNotNull(client)
        assertTrue(client.policies.isNotEmpty())

        AuthenticationHelper.verifyClientsRequest("client-id")
    }

    @Test
    fun `given a client id when not found should return null`() {
        AuthenticationHelper.mockNotFoundClient("client-id")

        val client = authenticationClient.findClientById("client-id")

        assertNull(client)

        AuthenticationHelper.verifyClientsRequest("client-id")
    }

    @Test
    fun `given a client id when response is invalid should return empty list`() {
        AuthenticationHelper.mockInvalidClientResponse("client-id")

        val client = authenticationClient.findClientById("client-id")

        assertNull(client)

        AuthenticationHelper.verifyClientsRequest("client-id")
    }

    @Test
    fun `given a access token should return session introspection`() {
        val introspect = IntrospectSampler.sample()
        AuthenticationHelper.mockSuccessfullyIntrospect("access-token", introspect)

        val result = authenticationClient.introspect("access-token")

        assertNotNull(result)

        AuthenticationHelper.verifyIntrospectRequest()
    }

    @Test
    fun `given a access token when not found should return null`() {
        AuthenticationHelper.mockNotFoundIntrospect("access-token")

        val introspect = authenticationClient.introspect("access-token")

        assertNull(introspect)

        AuthenticationHelper.verifyIntrospectRequest()
    }

    @Test
    fun `given a access token when response body is invalid should return null`() {
        AuthenticationHelper.mockInvalidIntrospectResponse("access-token")

        val introspect = authenticationClient.introspect("access-token")

        assertNull(introspect)

        AuthenticationHelper.verifyIntrospectRequest()
    }
}