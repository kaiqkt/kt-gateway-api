package com.kaiqkt.gateway.unit.resources.helpers

import com.kaiqkt.gateway.config.ObjectMapperConfig
import com.kaiqkt.gateway.models.Introspect
import com.kaiqkt.gateway.models.Policy
import com.kaiqkt.gateway.unit.models.PolicySampler
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType

object AuthenticationHelper : MockServerHolder() {
    override fun domainPath(): String = "/auth"

    private val objectMapper = ObjectMapperConfig().objectMapper()

    fun mockSuccessDownstream(){
        mockServer().`when`(
            HttpRequest.request()
                .withMethod(HttpMethod.GET.name())
                .withPath("${domainPath()}/v1/users")
        ).respond(
            HttpResponse.response()
                .withStatusCode(200)
        )
    }

    fun mockSuccessfullyIntrospect(accessToken: String, introspect: Introspect) {
        mockServer().`when`(
            HttpRequest.request()
                .withMethod(HttpMethod.GET.name())
                .withPath("${domainPath()}/v1/oauth/introspect")
                .withHeader(HttpHeaders.AUTHORIZATION, accessToken)
        ).respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(introspect))
        )
    }

    fun mockNotFoundIntrospect(accessToken: String) {
        mockServer().`when`(
            HttpRequest.request()
                .withMethod(HttpMethod.GET.name())
                .withPath("${domainPath()}/v1/oauth/introspect")
                .withHeader(HttpHeaders.AUTHORIZATION, accessToken)
        ).respond(
            HttpResponse.response()
                .withStatusCode(404)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        )
    }

    fun mockInvalidIntrospectResponse(accessToken: String) {

        mockServer().`when`(
            HttpRequest.request()
                .withMethod(HttpMethod.GET.name())
                .withPath("${domainPath()}/v1/oauth/introspect")
                .withHeader(HttpHeaders.AUTHORIZATION, accessToken)
        ).respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString("string"))
        )
    }

    fun mockSuccessfullyFoundPolicies(resourceServerId: String, policy: Policy = PolicySampler.sample()) {
        mockServer().`when`(
            HttpRequest.request()
                .withMethod(HttpMethod.GET.name())
                .withPath("${domainPath()}/v1/resources/$resourceServerId/policies")
        ).respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(listOf(policy)))
        )
    }

    fun mockNotFoundPolicies(resourceServerId: String) {
        mockServer().`when`(
            HttpRequest.request()
                .withMethod(HttpMethod.GET.name())
                .withPath("${domainPath()}/v1/resources/$resourceServerId/policies")
        ).respond(
            HttpResponse.response()
                .withStatusCode(404)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        )
    }

    fun mockInvalidPoliciesResponse(resourceServerId: String) {
        mockServer().`when`(
            HttpRequest.request()
                .withMethod(HttpMethod.GET.name())
                .withPath("${domainPath()}/v1/resources/$resourceServerId/policies")
        ).respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(PolicySampler.sample()))
        )
    }

    fun verifyDownstreamRequest(times: Int = 1) {
        val httpRequest = HttpRequest.request()
            .withMethod(HttpMethod.GET.name())
            .withPath("${domainPath()}/v1/users")

        verifyRequest(httpRequest, times)
    }

    fun verifyIntrospectRequest() {
        val httpRequest = HttpRequest.request()
            .withMethod(HttpMethod.GET.name())
            .withPath("${domainPath()}/v1/oauth/introspect")

        verifyRequest(httpRequest, 1)
    }

    fun verifyPoliciesRequest(resourceServerId: String) {
        val httpRequest = HttpRequest.request()
            .withMethod(HttpMethod.GET.name())
            .withPath("${domainPath()}/v1/resources/$resourceServerId/policies")

        verifyRequest(httpRequest, 1)
    }
}