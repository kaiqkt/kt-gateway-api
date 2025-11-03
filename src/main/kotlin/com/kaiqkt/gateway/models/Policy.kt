package com.kaiqkt.gateway.models

import java.io.Serializable

data class Policy(
    val uri: String,
    val method: String,
    val isPublic: Boolean,
    val roles: List<String>,
    val permissions: List<String>
): Serializable
