package com.kaiqkt.gateway.utils

import com.kaiqkt.gateway.models.ResourceServer
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "routes")
data class ResourceServersProperties(
    val resourceServers: List<ResourceServer>
)