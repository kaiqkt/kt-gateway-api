package com.kaiqkt.gateway.integration.gateway

import com.kaiqkt.gateway.integration.IntegrationTest
import com.kaiqkt.gateway.unit.models.IntrospectSampler
import com.kaiqkt.gateway.unit.models.PolicySampler
import com.kaiqkt.gateway.unit.resources.helpers.AuthenticationHelper
import io.restassured.RestAssured
import org.springframework.http.HttpHeaders
import kotlin.test.Test

class GatewayIntegrationTest : IntegrationTest() {

    @Test
    fun `given a request when does not have policies in cache should call auth service to get them`() {
        AuthenticationHelper.mockSuccessDownstream()
        AuthenticationHelper.mockSuccessfullyFoundPolicies(
            resourceServerId = "1",
            policy = PolicySampler.sample(isPublic = true, uri = "/auth/v1/users")
        )

        RestAssured.given()
            .get("/api/first/auth/v1/users")
            .then()
            .statusCode(200)

        RestAssured.given()
            .get("/api/first/auth/v1/users")
            .then()
            .statusCode(200)

        AuthenticationHelper.verifyDownstreamRequest(times = 2)
        AuthenticationHelper.verifyPoliciesRequest("1")
    }

    @Test
    fun `given a request to resource server should continue with downstream`() {
        AuthenticationHelper.mockSuccessfullyFoundPolicies(
            resourceServerId = "2",
            policy = PolicySampler.sample(isPublic = true, uri = "/auth/v1/users")
        )
        AuthenticationHelper.mockSuccessDownstream()

        RestAssured.given()
            .get("/api/second/auth/v1/users")
            .then()
            .statusCode(200)

        AuthenticationHelper.verifyDownstreamRequest(times = 1)
        AuthenticationHelper.verifyPoliciesRequest("2")
    }

    @Test
    fun `given a request when user has correct permissions should continue with downstream`() {
        AuthenticationHelper.mockSuccessfullyFoundPolicies(
            resourceServerId = "1",
            policy = PolicySampler.sample(uri = "/auth/v1/users", roles = listOf("USER"))
        )
        AuthenticationHelper.mockSuccessfullyIntrospect(
            "access-token",
            IntrospectSampler.sample(active = true, roles = listOf("USER"))
        )
        AuthenticationHelper.mockSuccessDownstream()

        RestAssured.given()
            .header(HttpHeaders.AUTHORIZATION, "access-token")
            .get("/api/first/auth/v1/users")
            .then()
            .statusCode(200)

        AuthenticationHelper.verifyDownstreamRequest(times = 1)
        AuthenticationHelper.verifyIntrospectRequest()
        AuthenticationHelper.verifyPoliciesRequest("1")

    }

    @Test
    fun `given a request when not found any policies should return http status 401`() {
        RestAssured.given()
            .get("/api/first/auth/v1/users")
            .then()
            .statusCode(401)

        AuthenticationHelper.verifyPoliciesRequest("1")
    }

    @Test
    fun `given a request when access token is invalid should return http status 401`() {
        AuthenticationHelper.mockSuccessfullyFoundPolicies(resourceServerId = "1", policy = PolicySampler.sample())

        RestAssured.given()
            .get("/api/first/auth/v1/users")
            .then()
            .statusCode(401)
            .extract()
            .headers()

        AuthenticationHelper.verifyPoliciesRequest("1")
    }

    @Test
    fun `given a request when session not found for introspection should return http status 401`() {
        AuthenticationHelper.mockSuccessfullyFoundPolicies(resourceServerId = "1", policy = PolicySampler.sample(uri = "/auth/v1/users"))

        RestAssured.given()
            .header(HttpHeaders.AUTHORIZATION, "access-token")
            .get("/api/first/auth/v1/users")
            .then()
            .statusCode(401)

        AuthenticationHelper.verifyPoliciesRequest("1")
        AuthenticationHelper.verifyIntrospectRequest()
    }

    @Test
    fun `given a request when session is inactivated should return http status 401`() {
        AuthenticationHelper.mockSuccessfullyFoundPolicies(resourceServerId = "1", policy = PolicySampler.sample(uri = "/auth/v1/users"))
        AuthenticationHelper.mockSuccessfullyIntrospect("access-token", IntrospectSampler.sample())

        RestAssured.given()
            .header(HttpHeaders.AUTHORIZATION, "access-token")
            .get("/api/first/auth/v1/users")
            .then()
            .statusCode(401)

        AuthenticationHelper.verifyPoliciesRequest("1")
        AuthenticationHelper.verifyIntrospectRequest()
    }

    @Test
    fun `given a request when user does not have the necessary authorities should return http status 401`() {
        AuthenticationHelper.mockSuccessfullyFoundPolicies(resourceServerId = "1", policy = PolicySampler.sample(uri = "/auth/v1/users"))
        AuthenticationHelper.mockSuccessfullyIntrospect("access-token", IntrospectSampler.sample(true))

        RestAssured.given()
            .header(HttpHeaders.AUTHORIZATION, "access-token")
            .get("/api/first/auth/v1/users")
            .then()
            .statusCode(403)

        AuthenticationHelper.verifyIntrospectRequest()
        AuthenticationHelper.verifyPoliciesRequest("1")
    }
}