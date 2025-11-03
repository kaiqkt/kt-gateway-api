package com.kaiqkt.gateway.services

import com.kaiqkt.gateway.models.Introspect
import com.kaiqkt.gateway.models.Policy
import com.kaiqkt.gateway.resources.AuthenticationClient
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import kotlin.collections.firstOrNull

@Component
class AuthenticationService(
    private val authenticationClient: AuthenticationClient,
) {
    fun introspect(accessToken: String): Introspect? {
        return authenticationClient.introspect(accessToken)
    }

    @Cacheable(value = ["policies"], key = "#method.concat(#resourceServerId)", unless = "#result == null")
    fun findPolicy(method: String, uri: String, resourceServerId: String): Policy? {
        val policies = authenticationClient.findAllPolicies(resourceServerId)

        if (policies.isEmpty()) {
            return null
        }

        return policies.firstOrNull { matchPolicy(method, uri, it) }
    }

    private fun matchPolicy(method: String, uri: String, policy: Policy): Boolean {
        return policy.method == method && (policy.uri == uri || policy.uri.toRegex().matches(uri))
    }
}