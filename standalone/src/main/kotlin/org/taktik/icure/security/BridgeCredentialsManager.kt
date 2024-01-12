package org.taktik.icure.security

interface BridgeCredentialsManager {
    suspend fun getModuleJwt(): String
}
