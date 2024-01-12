package org.taktik.icure.asyncdao

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.benmanes.caffeine.cache.Caffeine
import io.icure.asyncjacksonhttpclient.net.web.WebClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.apache.http.client.utils.URIBuilder
import org.springframework.cache.caffeine.CaffeineCache
import org.taktik.couchdb.Client
import org.taktik.couchdb.ClientImpl
import org.taktik.icure.asynccache.AsyncSafeCache
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.datastore.impl.SAMDatastoreInformation
import org.taktik.icure.properties.SAMCouchDBCredentialsProvider
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class SAMCouchDBDispatcher (
    private val httpClient: WebClient,
    private val objectMapper: ObjectMapper,
    private val prefix: String,
    private val dbFamily: String,
    private val couchDbCredentialsProvider: SAMCouchDBCredentialsProvider,
    private val createdReplicasIfNotExists: Int? = null
): CouchDbDispatcher {


    private val connectors = AsyncSafeCache<CouchDbConnectorReference, Client>(
        CaffeineCache(
            "Connectors",
            Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterAccess(240, TimeUnit.MINUTES)
                .build()
        )
    )

    private suspend fun attemptConnection(datastoreInformation: IDatastoreInformation, trials: Int): Client = try {
        connectors.get(
            CouchDbConnectorReference(datastoreInformation),
            object : AsyncSafeCache.AsyncValueProvider<CouchDbConnectorReference, Client> {
                override suspend fun getValue(key: CouchDbConnectorReference): Client {
                    return ClientImpl(
                        httpClient = httpClient,
                        dbURI = URIBuilder((datastoreInformation as SAMDatastoreInformation).dbInstanceUrl).setPath("$prefix-$dbFamily").build(),
                        objectMapper = objectMapper,
                        credentialsProvider = couchDbCredentialsProvider
                    ).also {
                        if (createdReplicasIfNotExists != null) {
                            if (!it.exists()) {
                                it.create(3, createdReplicasIfNotExists)
                            }
                        }
                    }
                }
            }
        ) ?: if (trials > 1) attemptConnection(datastoreInformation, trials - 1) else throw IllegalStateException("Cannot instantiate client")
    } catch (e: Exception) {
        if (trials > 1) attemptConnection(datastoreInformation, trials - 1) else throw e
    }
    override suspend fun getClient(datastoreInformation: IDatastoreInformation, retry: Int): Client = attemptConnection(datastoreInformation, retry)

    private data class CouchDbConnectorReference(val datastoreInformation: IDatastoreInformation)

}
