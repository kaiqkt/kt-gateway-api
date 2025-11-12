package com.kaiqkt.gateway.config

import com.kaiqkt.gateway.utils.MetricsUtils
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Configuration

@Configuration
class MetricsConfig(
    registry: MeterRegistry,
) {
    init {
        MetricsUtils.init(registry)
    }
}
