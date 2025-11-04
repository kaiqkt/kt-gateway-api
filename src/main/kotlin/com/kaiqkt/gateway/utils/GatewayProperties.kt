package com.kaiqkt.gateway.utils

import com.kaiqkt.gateway.models.ResourceServer
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "gateway")
data class GatewayProperties(
    val clientId: String,
    val resourceServers: List<ResourceServer>
)