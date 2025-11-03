package com.kaiqkt.gateway.config

import com.kaiqkt.gateway.filter.ObservabilityFilter
import com.kaiqkt.gateway.filter.SecurityFilter
import com.kaiqkt.gateway.models.ResourceServer
import com.kaiqkt.gateway.utils.ResourceServersProperties
import org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.rewritePath
import org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http
import org.springframework.context.annotation.Configuration
import org.springframework.web.context.support.GenericWebApplicationContext
import org.springframework.web.servlet.function.RequestPredicates.path
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.ServerResponse
import java.util.function.Supplier


@Configuration
class GatewayConfig(
    private val securityFilter: SecurityFilter,
    private val observabilityFilter: ObservabilityFilter,
    properties: ResourceServersProperties,
    context: GenericWebApplicationContext
) {

    init {
        for (resourceServer in properties.resourceServers) {
            context.registerBean(resourceServer.id, RouterFunction::class.java, Supplier { router(resourceServer) })
        }
    }


    private fun router(resourceServer: ResourceServer): RouterFunction<ServerResponse> {
        return route(resourceServer.id)
            .route(path("${resourceServer.uri}/**"), http())
            .before(uri(resourceServer.host))
            .before(observabilityFilter.logBefore())
            .before(rewritePath("${resourceServer.uri}/(?<segment>.*)", "/\${segment}"))
            .filter(securityFilter.authentication(resourceServer.id))
            .after(observabilityFilter.logAfter())
            .build()
    }
}