package com.kaiqkt.gateway.unit.resources.helpers

import org.mockserver.client.MockServerClient
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.HttpRequest
import org.mockserver.verify.VerificationTimes

abstract class MockServerHolder {

    companion object {
        private const val PORT: Int = 8081
        private val mockServer: ClientAndServer = ClientAndServer.startClientAndServer(PORT)
        private val baseUrl = "http://127.0.0.1:${mockServer.localPort}"
    }

    protected abstract fun domainPath(): String

    fun baseUrl() = "$baseUrl${domainPath()}"

    protected fun mockServer(): ClientAndServer = mockServer

    fun reset(): MockServerClient = mockServer.clear(HttpRequest.request().withPath("${domainPath()}/.*"))

    protected fun verifyRequest(httpRequest: HttpRequest, times: Int) {
        mockServer.verify(
            httpRequest,
            VerificationTimes.exactly(times)
        )
    }
}