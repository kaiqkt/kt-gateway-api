package com.kaiqkt.gateway.unit.filter

import com.kaiqkt.gateway.filter.ObservabilityFilter
import io.mockk.every
import io.mockk.mockk
import org.slf4j.MDC
import org.springframework.http.HttpMethod
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import java.net.URI
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ObservabilityFilterTest {
    private val observabilityFilter = ObservabilityFilter()

    @Test
    fun `given a request should put request information in mdc and log`() {
        val request = MockHttpServletRequest("POST", "/user/123")
            .let { ServerRequest.create(it, listOf()) }

        observabilityFilter.logBefore().invoke(request)

        assertNotNull(MDC.get("request_id"))
    }

    @Test
    fun `given a request and response should put information in mdc and log`() {
        val request = MockHttpServletRequest("POST", "/user/123/associate")
            .let { ServerRequest.create(it, listOf()) }
        val response = ServerResponse.created(URI.create("/user/123/associate"))
            .header("X-Session-Id", "123")
            .header("X-User-Id", "123")
            .header("X-Request-Id", "123")
            .build()

        observabilityFilter.logAfter().invoke(request, response)

        assertEquals("123", MDC.get("session_id"))
        assertEquals("123", MDC.get("user_id"))
    }
}