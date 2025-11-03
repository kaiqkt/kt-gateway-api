package com.kaiqkt.gateway.unit.models

import com.kaiqkt.gateway.models.Introspect
import java.util.*

object IntrospectSampler {
    fun sample(
        active: Boolean = false,
        roles: List<String> = emptyList(),
        permissions: List<String> = emptyList()
    ): Introspect = Introspect(
        active = active,
        sid = UUID.randomUUID().toString(),
        sub = "sub",
        roles = roles,
        permissions = permissions
    )
}