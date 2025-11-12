package com.kaiqkt.gateway.filter

import com.kaiqkt.gateway.services.AuthenticationService
import com.kaiqkt.gateway.utils.Constants
import com.kaiqkt.gateway.utils.MetricsUtils
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.servlet.function.HandlerFilterFunction
import org.springframework.web.servlet.function.HandlerFunction
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import java.util.UUID

@Component
class SecurityFilter(
    private val authenticationService: AuthenticationService,
) {
    private val log = LoggerFactory.getLogger(SecurityFilter::class.java)

    fun authentication(
        clientId: String,
        resourceServerHost: String,
    ): HandlerFilterFunction<ServerResponse, ServerResponse> {
        return HandlerFilterFunction { request: ServerRequest, next: HandlerFunction<ServerResponse> ->
            val requestId = UUID.randomUUID().toString()

            MDC.put("request_id", requestId)

            val policy = authenticationService.findPolicy(request.method().name(), request.uri().path, clientId)

            if (policy == null) {
                MetricsUtils.counter(
                    Constants.Metrics.SECURITY_FILTER,
                    Constants.Metrics.CLIENT,
                    clientId,
                    Constants.Metrics.RESOURCE_SERVER,
                    resourceServerHost,
                    Constants.Metrics.STATUS,
                    "policy_not_found",
                )
                return@HandlerFilterFunction ServerResponse.status(HttpStatus.UNAUTHORIZED).build()
            }

            if (policy.isPublic) {
                MetricsUtils.counter(
                    Constants.Metrics.SECURITY_FILTER,
                    Constants.Metrics.CLIENT,
                    clientId,
                    Constants.Metrics.RESOURCE_SERVER,
                    resourceServerHost,
                    Constants.Metrics.STATUS,
                    "public_access",
                )

                val modifiedRequest =
                    ServerRequest
                        .from(request)
                        .header("X-Request-Id", requestId)
                        .build()

                return@HandlerFilterFunction next.handle(modifiedRequest)
            }

            val accessToken = request.headers().firstHeader(HttpHeaders.AUTHORIZATION)

            if (accessToken == null) {
                MetricsUtils.counter(
                    Constants.Metrics.SECURITY_FILTER,
                    Constants.Metrics.CLIENT,
                    clientId,
                    Constants.Metrics.RESOURCE_SERVER,
                    resourceServerHost,
                    Constants.Metrics.STATUS,
                    "invalid_access_token",
                )

                return@HandlerFilterFunction ServerResponse.status(HttpStatus.UNAUTHORIZED).build()
            }

            val introspect = authenticationService.introspect(accessToken)

            if (introspect == null) {
                MetricsUtils.counter(
                    Constants.Metrics.SECURITY_FILTER,
                    Constants.Metrics.CLIENT,
                    clientId,
                    Constants.Metrics.RESOURCE_SERVER,
                    resourceServerHost,
                    Constants.Metrics.STATUS,
                    "session_not_found",
                )

                return@HandlerFilterFunction ServerResponse.status(HttpStatus.UNAUTHORIZED).build()
            }

            val hasRoles = introspect.roles.any(policy.roles::contains)
            val hasPermissions = introspect.permissions.any(policy.permissions::contains)

            if (!introspect.active) {
                MetricsUtils.counter(
                    Constants.Metrics.SECURITY_FILTER,
                    Constants.Metrics.CLIENT,
                    clientId,
                    Constants.Metrics.RESOURCE_SERVER,
                    resourceServerHost,
                    Constants.Metrics.STATUS,
                    "inactive_session",
                )

                log.info("Request with inactivated session ${introspect.sid} of user ${introspect.sub}")
                return@HandlerFilterFunction ServerResponse.status(HttpStatus.UNAUTHORIZED).build()
            }

            if (hasRoles || hasPermissions) {
                MetricsUtils.counter(
                    Constants.Metrics.SECURITY_FILTER,
                    Constants.Metrics.CLIENT,
                    clientId,
                    Constants.Metrics.RESOURCE_SERVER,
                    resourceServerHost,
                    Constants.Metrics.STATUS,
                    "protect_access",
                )

                val modifiedRequest =
                    ServerRequest
                        .from(request)
                        .header("X-Request-Id", requestId)
                        .header("X-User-Id", introspect.sub)
                        .build()

                return@HandlerFilterFunction next.handle(modifiedRequest)
            }

            MetricsUtils.counter(
                Constants.Metrics.SECURITY_FILTER,
                Constants.Metrics.CLIENT,
                clientId,
                Constants.Metrics.RESOURCE_SERVER,
                resourceServerHost,
                Constants.Metrics.STATUS,
                "forbidden",
            )

            log.info("Request cancelled due user ${introspect.sub} does not have the correct authorities")
            return@HandlerFilterFunction ServerResponse.status(HttpStatus.FORBIDDEN).build()
        }
    }
}
