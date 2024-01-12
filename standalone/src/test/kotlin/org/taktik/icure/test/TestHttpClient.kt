package org.taktik.icure.test

import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import org.springframework.stereotype.Component

@Component
class TestHttpClient {

    private val httpClient = HttpClient(CIO) {
        engine {
            requestTimeout = 0
        }
        install(ContentNegotiation) {
            json()
        }
    }

    suspend fun makeGetRequest(url: String, headers: Map<String, String> = emptyMap(), expectedResponse: Int = 200, responseHeaders: Map<String, String> = emptyMap()) =
        httpClient.get(url) {
            headers.forEach {
                header(it.key, it.value)
            }
        }.let {
            val t = it.bodyAsText()
            it.status.value shouldBe expectedResponse
            responseHeaders.forEach { h ->
                it.headers[h.key] shouldBe h.value
            }
            t
        }

    suspend fun makePostRequest(url: String, payload: String, headers: Map<String, String> = emptyMap(), expectedResponse: Int = 200, responseHeaders: Map<String, String> = emptyMap()) =
        httpClient.post(url) {
            headers.forEach {
                header(it.key, it.value)
            }
            contentType(ContentType.Application.Json)
            setBody(payload)
        }.let {
            it.status.value shouldBe expectedResponse
            responseHeaders.forEach { h ->
                it.headers[h.key] shouldBe h.value
            }
            it.bodyAsText()
        }

}
