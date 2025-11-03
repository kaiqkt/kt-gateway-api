package com.kaiqkt.gateway.filter

import com.kaiqkt.gateway.services.AuthenticationService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.servlet.function.HandlerFilterFunction
import org.springframework.web.servlet.function.HandlerFunction
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse

@Component
class SecurityFilter(
    private val authenticationService: AuthenticationService
) {
    private val log = LoggerFactory.getLogger(SecurityFilter::class.java)

    fun authentication(
        resourceServerId: String
    ): HandlerFilterFunction<ServerResponse, ServerResponse> {
        return HandlerFilterFunction { request: ServerRequest, next: HandlerFunction<ServerResponse> ->
            //resource server id
            //policy_not_found
            val policy = authenticationService.findPolicy(request.method().name(), request.uri().path, resourceServerId)

            if (policy == null) {
                log.info("Resource server $resourceServerId does not have policies registered")
                return@HandlerFilterFunction ServerResponse.status(HttpStatus.UNAUTHORIZED).build()
            }

            if (policy.isPublic) {
                //resource server id
                //public_access
                //policy uri
                //policy method
                return@HandlerFilterFunction next.handle(request)
            }

            //resource server id
            //invalid access token
            //policy uri
            //policy method
            val accessToken = request.headers().firstHeader(HttpHeaders.AUTHORIZATION)

            if (accessToken == null) {
                log.info("Request for resource server $resourceServerId with invalid access token")
                return@HandlerFilterFunction ServerResponse.status(HttpStatus.UNAUTHORIZED).build()
            }

            //resource server id
            //invalid session
            //policy uri
            //policy method
            val introspect = authenticationService.introspect(accessToken) ?:
            return@HandlerFilterFunction ServerResponse.status(HttpStatus.UNAUTHORIZED).build()

            val hasRoles = introspect.roles.any(policy.roles::contains)
            val hasPermissions = introspect.permissions.any(policy.permissions::contains)

            //resource server id
            //inactivated session
            //policy uri
            //policy method
            if (!introspect.active) {
                log.info("Request with inactivated session ${introspect.sid} of user ${introspect.sub}")
                return@HandlerFilterFunction ServerResponse.status(HttpStatus.UNAUTHORIZED).build()
            }

            if (hasRoles || hasPermissions) {
                val modifiedRequest = ServerRequest.from(request)
                    .header("X-User-Id", introspect.sub)
                    .header("X-Session-Id", introspect.sid)
                    .build()

                //resource server id
                //protected_access
                //policy uri
                //policy method
                return@HandlerFilterFunction next.handle(modifiedRequest)
            }

            //resource server id
            //invalid_authorities
            //policy uri
            //policy method
            log.info("Request cancelled due user ${introspect.sub} does not have the correct authorities")
            return@HandlerFilterFunction ServerResponse.status(HttpStatus.FORBIDDEN).build()
        }
    }
}
