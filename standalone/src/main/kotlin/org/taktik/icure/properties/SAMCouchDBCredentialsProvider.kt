package org.taktik.icure.properties

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.taktik.icure.db.CouchDbConfigurationClient
import org.taktik.icure.db.impl.CouchDbConfigurationClientImpl
import org.taktik.icure.security.UsernamePassword
import org.taktik.icure.security.credentialsrotation.CouchDbCredentialsManager
import org.taktik.icure.security.credentialsrotation.RotationNotification
import org.taktik.icure.security.credentialsrotation.RotationResponse

@Component
@Profile("sam")
class SAMCouchDBCredentialsProvider(
    val properties: SAMCouchDbProperties
) : CouchDbCredentialsManager {

    companion object {
        private val log = LoggerFactory.getLogger(SAMCouchDbProperties::class.java)
    }

    private var currentCredentials = UsernamePassword(
        checkNotNull(properties.username) { "CouchDB username must not be null" },
        checkNotNull(properties.password) { "CouchDB password must not be null" }
    ).also {
        runBlocking {
            val initialCredentialsOk = checkCredentialsSameOnAllClusters(it)?.also {
                log.error("Invalid initial credentials: $it")
            } == null
            if(!initialCredentialsOk) {
                throw IllegalStateException("Credentials stored in kubernetes are not valid")
            }
        }
    }

    override fun getCredentials(): UsernamePassword = currentCredentials
    override suspend fun notifyNewCredentials(
        notification: RotationNotification
    ): RotationResponse = properties.knownServerUrls().let { knownServers ->
        if (!notification.affectedServices.containsAll(knownServers)) {
            log.warn("Received credentials update notification which does not include all known couchdb servers (${notification.affectedServices}")
            RotationResponse(false, "Rotation does not affect all known couchdb server")
        } else {
            checkCredentialsSameOnAllClusters(notification.credentials)
                ?.let { RotationResponse(false, it) }
                ?: RotationResponse(true).also {
                    log.info("Updating credentials from notification: ${notification.credentials.toHashedString()}")
                    currentCredentials = notification.credentials
                    log.info("Credentials after update: ${currentCredentials.toHashedString()}")
                }
        }
    }

    /**
     * Returns null if the credentials are ok, an error message if not.
     */
    private suspend fun checkCredentialsSameOnAllClusters(credentials: UsernamePassword): String? =
        properties.knownServerUrls().asFlow().fold(null as String?) { err, currUrl ->
            err ?: try {
                if (checkCredentialsSameOnAllNodes(credentials, currUrl)) {
                    null
                } else "Credentials for new user are not the same on all nodes of cluster $currUrl"
            } catch (e: CouchDbConfigurationClient.UnauthorizedException) {
                // This is the only exception we should consider, as it can happen if the notifier provided invalid credentials
                // If we get any other exception we will leave it to the global error handler
                e.message
            }
        }

    /**
     * If there are problem with the configuration it will either throw a [CouchDbConfigurationClient.UnauthorizedException]
     * or return false.
     */
    private suspend fun checkCredentialsSameOnAllNodes(credentials: UsernamePassword, clusterUrl: String): Boolean {
        val configurationClient = CouchDbConfigurationClientImpl(
            clusterUrl,
            checkNotNull(properties.username) { "CouchDB username must not be null" },
            checkNotNull(properties.password) { "CouchDB password must not be null" }
        )
        val members = configurationClient.getNodes()
        val adminsOfNodes = members.asFlow().map { configurationClient.getAdmins(it) }.toList()
        return adminsOfNodes.map { it[credentials.username] }.toSet().let {
            it.size == 1 && it.first() != null
        }
    }

}
