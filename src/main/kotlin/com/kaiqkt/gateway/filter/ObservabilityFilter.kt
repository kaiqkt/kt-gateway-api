package com.kaiqkt.gateway.filter

import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.web.servlet.function.remoteAddressOrNull
import java.util.*
import kotlin.jvm.optionals.getOrNull


@Component
class ObservabilityFilter {
    private val log = LoggerFactory.getLogger(ObservabilityFilter::class.java)

    fun logBefore(): (ServerRequest) -> ServerRequest {
        return { request ->
            val requestId = UUID.randomUUID().toString()

            MDC.put("request_id", requestId)

            ServerRequest.from(request).header("X-Request-Id", requestId).build()
        }
    }

    fun logAfter(): (ServerRequest, ServerResponse) -> ServerResponse {
        return { _, response ->
            //resource server id
            //status code
            //historigram resoose

            log.info("Downstream request responded with status code ${response.statusCode()}")

            response
        }
    }
}