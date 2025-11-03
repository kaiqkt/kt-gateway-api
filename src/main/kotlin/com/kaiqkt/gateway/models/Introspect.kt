package com.kaiqkt.gateway.models

data class Introspect(
    val active: Boolean,
    val sid: String,
    val sub: String,
    val roles: List<String>,
    val permissions: List<String>
)