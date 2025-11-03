package com.kaiqkt.gateway.unit.models

import com.kaiqkt.gateway.models.Policy

object PolicySampler {
    fun sample(
        uri: String = "/v1/users",
        isPublic: Boolean = false,
        method: String = "GET",
        roles: List<String> = emptyList(),
        permissions: List<String> = emptyList()
    ): Policy = Policy(
        uri = uri,
        method = method,
        isPublic = isPublic,
        roles = roles,
        permissions = permissions
    )
}