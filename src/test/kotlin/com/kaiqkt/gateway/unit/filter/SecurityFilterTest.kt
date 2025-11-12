package com.kaiqkt.gateway.unit.filter

import com.kaiqkt.gateway.filter.SecurityFilter
import com.kaiqkt.gateway.services.AuthenticationService
import com.kaiqkt.gateway.unit.models.IntrospectSampler
import com.kaiqkt.gateway.unit.models.PolicySampler
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.servlet.function.HandlerFunction
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SecurityFilterTest {
    private val authenticationService = mockk<AuthenticationService>()
    private val nextHandler = mockk<HandlerFunction<ServerResponse>> {}
    private val securityFilter = SecurityFilter(authenticationService)

    @Test
    fun `given a request for a resource server when a policy does not exist should return a http response 401`() {
        val request =
            MockHttpServletRequest("POST", "/user/123/associate")
                .apply {
                    addHeader("X-Request-Id", UUID.randomUUID())
                }.let { ServerRequest.create(it, listOf()) }

        val nextHandler = mockk<HandlerFunction<ServerResponse>> {}
        every { authenticationService.findPolicy(any(), any(), any()) } returns null

        val response =
            securityFilter
                .authentication("resource_server_id", "resource_server_name")
                .filter(request, nextHandler)

        verify { authenticationService.findPolicy(any(), any(), any()) }

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode())
    }

    @Test
    fun `given a request for a resource server when a policy exists and is public should continue the request`() {
        val policy = PolicySampler.sample(isPublic = true)

        val request =
            MockHttpServletRequest(policy.method, policy.uri)
                .let { ServerRequest.create(it, listOf()) }

        val modifiedRequest = slot<ServerRequest>()

        every { authenticationService.findPolicy(any(), any(), any()) } returns policy
        every { nextHandler.handle(capture(modifiedRequest)) } returns ServerResponse.ok().build()

        securityFilter.authentication("resource_server_id", "resource_server_name").filter(request, nextHandler)

        verify { authenticationService.findPolicy(any(), any(), any()) }
        verify { nextHandler.handle(modifiedRequest.captured) }
    }

    @Test
    fun `given a request for a resource server when access token is not provided should return a http response 401`() {
        val policy = PolicySampler.sample()

        val request =
            MockHttpServletRequest(policy.method, policy.uri)
                .let { ServerRequest.create(it, listOf()) }

        every { authenticationService.findPolicy(any(), any(), any()) } returns policy
        every { nextHandler.handle(any()) } returns ServerResponse.ok().build()

        val response = securityFilter.authentication("resource_server_id", "resource_server_name").filter(request, nextHandler)

        verify { authenticationService.findPolicy(any(), any(), any()) }

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode())
    }

    @Test
    fun `given a request for a resource server when introspecting a access token and return null should return a http status 401`() {
        val policy = PolicySampler.sample()

        val request =
            MockHttpServletRequest(policy.method, policy.uri)
                .apply {
                    addHeader(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                }.let { ServerRequest.create(it, listOf()) }

        every { authenticationService.findPolicy(any(), any(), any()) } returns policy
        every { authenticationService.introspect(any()) } returns null

        val response = securityFilter.authentication("resource_server_id", "resource_server_name").filter(request, nextHandler)

        verify { authenticationService.findPolicy(any(), any(), any()) }
        verify { authenticationService.introspect(any()) }

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode())
    }

    @Test
    fun `given a request for a resource server when session is not active should return http status 401`() {
        val policy = PolicySampler.sample()

        val introspect = IntrospectSampler.sample()

        val request =
            MockHttpServletRequest(policy.method, policy.uri)
                .apply {
                    addHeader(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                }.let { ServerRequest.create(it, listOf()) }

        every { authenticationService.findPolicy(any(), any(), any()) } returns policy
        every { authenticationService.introspect(any()) } returns introspect

        val response = securityFilter.authentication("resource_server_id", "resource_server_name").filter(request, nextHandler)

        verify { authenticationService.findPolicy(any(), any(), any()) }
        verify { authenticationService.introspect(any()) }

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode())
    }

    @Test
    fun `given a request for a resource server when user does not have the necessary roles or permissions should return http status 403`() {
        val policy = PolicySampler.sample()
        val introspect = IntrospectSampler.sample(true)

        val request =
            MockHttpServletRequest(policy.method, policy.uri)
                .apply {
                    addHeader(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                }.let { ServerRequest.create(it, listOf()) }

        every { authenticationService.findPolicy(any(), any(), any()) } returns policy
        every { authenticationService.introspect(any()) } returns introspect

        val response = securityFilter.authentication("resource_server_id", "resource_server_name").filter(request, nextHandler)

        verify { authenticationService.findPolicy(any(), any(), any()) }
        verify { authenticationService.introspect(any()) }

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode())
    }

    @Test
    fun `given a request for a resource server when is not public and user has the necessary roles should continue the request`() {
        val policy =
            PolicySampler.sample(permissions = listOf("itinerary.view", "user.write"), roles = listOf("USER", "ADMIN"))
        val introspect = IntrospectSampler.sample(active = true, roles = listOf("USER"))

        val request =
            MockHttpServletRequest(policy.method, policy.uri)
                .apply {
                    addHeader(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                }.let { ServerRequest.create(it, listOf()) }
        val nextHandler = mockk<HandlerFunction<ServerResponse>> {}
        val modifiedRequest = slot<ServerRequest>()

        every { authenticationService.findPolicy(any(), any(), any()) } returns policy
        every { authenticationService.introspect(any()) } returns introspect
        every { nextHandler.handle(capture(modifiedRequest)) } returns ServerResponse.ok().build()

        securityFilter.authentication("resource_server_id", "resource_server_name").filter(request, nextHandler)

        verify { authenticationService.findPolicy(any(), any(), any()) }
        verify { authenticationService.introspect(any()) }
        verify { nextHandler.handle(modifiedRequest.captured) }

        assertEquals(introspect.sub, modifiedRequest.captured.headers().firstHeader("X-User-Id"))
        assertNotNull(modifiedRequest.captured.headers().firstHeader("X-Request-Id"))
    }

    @Test
    fun `given a request for a resource server when is not public and user has the necessary permissions should continue the request`() {
        val policy =
            PolicySampler.sample(permissions = listOf("itinerary.view", "user.write"), roles = listOf("USER", "ADMIN"))
        val introspect = IntrospectSampler.sample(active = true, permissions = listOf("itinerary.view"))

        val request =
            MockHttpServletRequest(policy.method, policy.uri)
                .apply {
                    addHeader(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                }.let { ServerRequest.create(it, listOf()) }
        val nextHandler = mockk<HandlerFunction<ServerResponse>> {}
        val modifiedRequest = slot<ServerRequest>()

        every { authenticationService.findPolicy(any(), any(), any()) } returns policy
        every { authenticationService.introspect(any()) } returns introspect
        every { nextHandler.handle(capture(modifiedRequest)) } returns ServerResponse.ok().build()

        securityFilter.authentication("resource_server_id", "resource_server_name").filter(request, nextHandler)

        verify { authenticationService.findPolicy(any(), any(), any()) }
        verify { authenticationService.introspect(any()) }
        verify { nextHandler.handle(modifiedRequest.captured) }

        assertEquals(introspect.sub, modifiedRequest.captured.headers().firstHeader("X-User-Id"))
        assertNotNull(modifiedRequest.captured.headers().firstHeader("X-Request-Id"))
    }
}
