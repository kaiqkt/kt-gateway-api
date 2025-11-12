package com.kaiqkt.gateway.config

import com.kaiqkt.gateway.filter.SecurityFilter
import com.kaiqkt.gateway.models.ResourceServer
import com.kaiqkt.gateway.utils.GatewayProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
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
@EnableConfigurationProperties(GatewayProperties::class)
class GatewayConfig(
    private val securityFilter: SecurityFilter,
    properties: GatewayProperties,
    context: GenericWebApplicationContext,
) {
    init {
        for (resourceServer in properties.resourceServers) {
            context.registerBean(resourceServer.name, RouterFunction::class.java, Supplier { router(resourceServer, properties.clientId) })
        }
    }

    private fun router(
        resourceServer: ResourceServer,
        clientId: String,
    ): RouterFunction<ServerResponse> =
        route(resourceServer.name)
            .route(path("${resourceServer.uri}/**"), http())
            .before(uri(resourceServer.host))
            .before(rewritePath("${resourceServer.uri}/(?<segment>.*)", $$"/${segment}"))
            .filter(securityFilter.authentication(clientId, resourceServer.host))
            .build()
}
