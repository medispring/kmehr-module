package org.taktik.icure.db.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.taktik.icure.db.CouchDbConfigurationClient
import reactor.core.publisher.Mono
import java.io.Serializable

class CouchDbConfigurationClientImpl(
    private val url: String,
    private val username: String,
    private val password: String
) : CouchDbConfigurationClient {
    private val client = WebClient.builder()
        .codecs { clientDefaultCodecsConfigurer ->
            val mapper = ObjectMapper().registerKotlinModule()
            clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(mapper, MediaType.APPLICATION_JSON))
            clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(mapper, MediaType.APPLICATION_JSON))
        }
        .build()

    override suspend fun getNodes(): Set<String> =
        client.get()
            .uri("$url/_membership")
            .headers { it.setBasicAuth(username, password) }
            .exchangeToMono { response ->
                when (response.statusCode()) {
                    HttpStatus.OK ->
                        response.bodyToMono<MembershipResponse>()
                    HttpStatus.UNAUTHORIZED ->
                        Mono.error(CouchDbConfigurationClient.UnauthorizedException("Can't access membership information for cluster $url"))
                    else ->
                        response.createException().flatMap { Mono.error(it) }
                }
            }
            .awaitSingle()
            .let { it.all_nodes + it.cluster_nodes }

    override suspend fun getAdmins(node: String): Map<String, String> =
        client.get()
            .uri("$url/_node/$node/_config/admins")
            .headers { it.setBasicAuth(username, password) }
            .exchangeToMono { response ->
                when (response.statusCode()) {
                    HttpStatus.OK ->
                        response.bodyToMono<Map<String, String>>()
                    HttpStatus.UNAUTHORIZED ->
                        Mono.error(CouchDbConfigurationClient.UnauthorizedException("Can't access admin information for node $node@$url"))
                    else ->
                        response.createException().flatMap { Mono.error(it) }
                }
            }
            .awaitSingle()



    private data class MembershipResponse(
        val all_nodes: Set<String>,
        val cluster_nodes: Set<String>
    ) : Serializable
}
