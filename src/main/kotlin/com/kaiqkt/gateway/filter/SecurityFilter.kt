package com.kaiqkt.gateway.filter

import com.kaiqkt.gateway.services.AuthenticationService
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
    private val authenticationService: AuthenticationService
) {
    private val log = LoggerFactory.getLogger(SecurityFilter::class.java)

    fun authentication(
        clientId: String
    ): HandlerFilterFunction<ServerResponse, ServerResponse> {
        return HandlerFilterFunction { request: ServerRequest, next: HandlerFunction<ServerResponse> ->
            val requestId = UUID.randomUUID().toString()

            MDC.put("request_id", requestId)

            val policy = authenticationService.findPolicy(request.method().name(), request.uri().path, clientId)

            if (policy == null) {
                //client id
                //policy_not_found
                //resource name
                //resource host
                //policy method
                log.info("Client $clientId does not have the necessary policies associated")
                return@HandlerFilterFunction ServerResponse.status(HttpStatus.UNAUTHORIZED).build()
            }

            if (policy.isPublic) {
                //client id
                //public_access
                //resource name
                //resource host
                //policy uri
                //policy method
                val modifiedRequest = ServerRequest.from(request)
                    .header("X-Request-Id", requestId)
                    .build()

                return@HandlerFilterFunction next.handle(modifiedRequest)
            }

            //client id
            //invalid access token
            //resource name
            //resource host
            //policy uri
            //policy method
            val accessToken = request.headers().firstHeader(HttpHeaders.AUTHORIZATION)

            if (accessToken == null) {
                log.info("Request for client $clientId with invalid access token")
                return@HandlerFilterFunction ServerResponse.status(HttpStatus.UNAUTHORIZED).build()
            }

            //client id
            //invalid session
            //resource name
            //resource host
            //policy uri
            //policy method
            val introspect = authenticationService.introspect(accessToken) ?:
            return@HandlerFilterFunction ServerResponse.status(HttpStatus.UNAUTHORIZED).build()

            val hasRoles = introspect.roles.any(policy.roles::contains)
            val hasPermissions = introspect.permissions.any(policy.permissions::contains)

            //client id
            //inactivated session
            //resource name
            //resource host
            //policy uri
            //policy method
            if (!introspect.active) {
                log.info("Request with inactivated session ${introspect.sid} of user ${introspect.sub}")
                return@HandlerFilterFunction ServerResponse.status(HttpStatus.UNAUTHORIZED).build()
            }

            if (hasRoles || hasPermissions) {
                //client id
                //protected_access
                //resource name
                //resource host
                //policy uri
                //policy method
                val modifiedRequest = ServerRequest.from(request)
                    .header("X-Request-Id", requestId)
                    .header("X-User-Id", introspect.sub)
                    .build()

                return@HandlerFilterFunction next.handle(modifiedRequest)
            }

            //client id
            //invalid_authorities
            //resource name
            //resource host
            //policy uri
            //policy method
            log.info("Request cancelled due user ${introspect.sub} does not have the correct authorities")
            return@HandlerFilterFunction ServerResponse.status(HttpStatus.FORBIDDEN).build()
        }
    }
}
