package com.kaiqkt.gateway.unit.models

import com.kaiqkt.gateway.models.Client
import com.kaiqkt.gateway.models.Policy

object ClientSampler {
    fun sample(policy: Policy? = PolicySampler.sample()): Client = Client(
        policies = policy?.let { listOf(it) } ?: listOf()
    )
}