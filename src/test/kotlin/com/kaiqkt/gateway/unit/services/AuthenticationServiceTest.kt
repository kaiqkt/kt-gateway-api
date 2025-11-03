package com.kaiqkt.gateway.unit.services

import com.kaiqkt.gateway.unit.models.IntrospectSampler
import com.kaiqkt.gateway.unit.models.PolicySampler
import com.kaiqkt.gateway.resources.AuthenticationClient
import com.kaiqkt.gateway.services.AuthenticationService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AuthenticationServiceTest {
    private val authenticationClient = mockk<AuthenticationClient>()
    private val authenticationService = AuthenticationService(authenticationClient)

    @Test
    fun `given a access token should introspect successfully`() {
        every { authenticationClient.introspect(any()) } returns IntrospectSampler.sample()

        authenticationService.introspect("access-token")

        verify { authenticationService.introspect(any()) }
    }

    @Test
    fun `given a method, uri and resource server id when found a policy with exact uri should return a policy`() {
        val policy = PolicySampler.sample()

        every { authenticationClient.findAllPolicies( any()) } returns listOf(policy)

        val response = authenticationService.findPolicy(
            "GET",
            "/v1/users",
            UUID.randomUUID().toString()
        )

        verify { authenticationClient.findAllPolicies(any()) }

        assertNotNull(response)
        assertEquals(policy, response)
    }

    @Test
    fun `given a method, uri and resource server id when found a policy that matches a uri should return a policy`() {
        val policy = PolicySampler.sample(uri = "/v1/users/[^/]+/associate")

        every { authenticationClient.findAllPolicies( any()) } returns listOf(policy)

        val response = authenticationService.findPolicy(
            "GET",
            "/v1/users/1234/associate",
            UUID.randomUUID().toString()
        )

        verify { authenticationClient.findAllPolicies(any()) }

        assertNotNull(response)
        assertEquals(policy, response)
    }

    @Test
    fun `given a method, uri and resource server id when found a policy that matches a uri with query params should return a policy`() {
        val policy = PolicySampler.sample(uri = "/v1/users/[^/]+/associate(?:\\?.*)?$")

        every { authenticationClient.findAllPolicies( any()) } returns listOf(policy)

        val response = authenticationService.findPolicy(
            "GET",
            "/v1/users/1234/associate?role_id=1234",
            UUID.randomUUID().toString()
        )

        verify { authenticationClient.findAllPolicies(any()) }

        assertNotNull(response)
        assertEquals(policy, response)
    }

    @Test
    fun `given a method, uri and resource server id when found not policy that matches a uri with query params should return null`() {
        val policy = PolicySampler.sample(uri = "/v1/users/[^/]+/associate(?:\\?.*)?$")

        every { authenticationClient.findAllPolicies( any()) } returns listOf(policy)

        val response = authenticationService.findPolicy(
            "GET",
            "/v1/users/1234/associate/123",
            UUID.randomUUID().toString()
        )

        verify { authenticationClient.findAllPolicies(any()) }

        assertNull(response)
    }

    @Test
    fun `given a method, uri and resource server id when not found a policy with method or uri should return null`() {
        val policy = PolicySampler.sample()

        every { authenticationClient.findAllPolicies( any()) } returns listOf(policy)

        val response = authenticationService.findPolicy(
            "POST",
            "/v1/users/test",
            UUID.randomUUID().toString()
        )

        verify { authenticationClient.findAllPolicies(any()) }

        assertNull(response)
    }

    @Test
    fun `given a method, uri and resource server id when not found any policies should return null`() {

        every { authenticationClient.findAllPolicies( any()) } returns listOf()

        val response = authenticationService.findPolicy(
            "POST",
            "/v1/users",
            UUID.randomUUID().toString()
        )

        verify { authenticationClient.findAllPolicies(any()) }

        assertNull(response)
    }
}