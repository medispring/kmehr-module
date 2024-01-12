package org.taktik.icure.db

/**
 * Client to access configuration information of couchdb.
 */
interface CouchDbConfigurationClient {
    /**
     * @return the nodes part of this couchdb cluster.
     * @throws UnauthorizedException if this client is not authorised to access couchdb
     */
    suspend fun getNodes(): Set<String>

    /**
     * Get the admins for this couchdb cluster at a specific node.
     * @param node a node of this cluster.
     * @return the admin map as username -> passwordHash.
     * @throws UnauthorizedException if this client is not authorised to access couchdb
     */
    suspend fun getAdmins(node: String): Map<String, String>

    class UnauthorizedException(message: String, cause: Throwable? = null) : Exception(message, cause)
}

/**
 * Factory to build configuration clients.
 */
interface CouchDbConfigurationClientFactory {
    /**
     * Build a new configuration client for the couchdb cluster at the provided url,
     * using the provided username and password for authentication.
     */
    fun build(
        url: String,
        username: String,
        password: String
    ): CouchDbConfigurationClient
}
