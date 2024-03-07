package org.taktik.icure.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.icure.asyncjacksonhttpclient.net.web.WebClient
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.apache.commons.logging.LogFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.taktik.couchdb.springframework.webclient.SpringWebfluxWebClient
import org.taktik.icure.asyncdao.SAMCouchDBDispatcher
import org.taktik.icure.properties.SAMCouchDBCredentialsProvider
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
@Profile("sam")
@Configuration
class SAMCouchDBConfig(
    private val credentialsProvider: SAMCouchDBCredentialsProvider
) {

    private val log = LogFactory.getLog("org.taktik.icure.config.WebClient")

    @Bean
    fun connectionProvider(): ConnectionProvider {
        return ConnectionProvider.builder("LARGE_POOL")
            .maxConnections(50000)
            .maxIdleTime(Duration.ofSeconds(120))
            .pendingAcquireMaxCount(-1).build()
    }

    @Bean
    fun httpClient(connectionProvider: ConnectionProvider) = SpringWebfluxWebClient(
        ReactorClientHttpConnector(
            HttpClient
                .create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .doOnConnected { connection ->
                    connection
                        .addHandlerLast(ReadTimeoutHandler(30))
                        .addHandlerLast(WriteTimeoutHandler(30))
                }
                .compress(true)
        )
    ) { xff ->
        xff.add(
            ExchangeFilterFunction.ofRequestProcessor { req ->
                if (log.isDebugEnabled) {
                    log.debug("-> ${req.method().name()} ${req.url()}")
                }
                Mono.just(req)
            }
        )
    }

    @Bean
    fun drugCouchDbDispatcher(
        httpClient: WebClient,
        objectMapper: ObjectMapper
    ) = SAMCouchDBDispatcher(
        httpClient,
        objectMapper,
        "icure",
        "drugs",
        credentialsProvider,
        3
    )

    @Bean
    fun chapIVCouchDbDispatcher(
        httpClient: WebClient,
        objectMapper: ObjectMapper
    ) = SAMCouchDBDispatcher(
        httpClient,
        objectMapper,
        "icure",
        "chapiv",
        credentialsProvider,
        3
    )

}
