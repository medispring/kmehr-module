package org.taktik.icure.asynclogic.bridge

import io.icure.kraken.client.apis.UserApi
import io.icure.kraken.client.security.ExternalJWTProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Service
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.asynclogic.UserLogic
import org.taktik.icure.asynclogic.impl.BridgeAsyncSessionLogic
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.User
import org.taktik.icure.entities.base.PropertyStub
import org.taktik.icure.exceptions.BridgeException
import org.taktik.icure.pagination.PaginationElement
import org.taktik.icure.services.external.rest.v2.dto.UserDto
import org.taktik.icure.services.external.rest.v2.mapper.UnsecureUserV2Mapper

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
@Service
class UserLogicBridge(
    private val asyncSessionLogic: BridgeAsyncSessionLogic,
    private val bridgeConfig: BridgeConfig,
    private val userMapper: UnsecureUserV2Mapper
) : GenericLogicBridge<User>(), UserLogic {

    private val userDtoToUser: (UserDto) -> User = {
        userMapper.map(it.copy(
            authenticationTokens = it.authenticationTokens.mapValues { (_, token) -> token.copy(token = "*") }
        ))
    }

    private suspend fun getApi() = asyncSessionLogic.getCurrentJWT()?.let { token ->
        UserApi(basePath = bridgeConfig.iCureUrl, authProvider = ExternalJWTProvider(token))
    }

    override suspend fun createOrUpdateToken(
        userIdentifier: String,
        key: String,
        tokenValidity: Long,
        token: String?,
        useShortToken: Boolean
    ): String {
        throw BridgeException()
    }

    override suspend fun createUser(user: User): User? {
        throw UnsupportedOperationException("The Kmehr module cannot create users")
    }

    override suspend fun deleteUser(userId: String): DocIdentifier? {
        throw UnsupportedOperationException("The Kmehr module cannot delete users")
    }

    override suspend fun disableUser(userId: String): User? {
        throw UnsupportedOperationException("The Kmehr module cannot disable users")
    }

    override suspend fun enableUser(userId: String): User? {
        throw UnsupportedOperationException("The Kmehr module cannot enable users")
    }

    override fun filterUsers(
        paginationOffset: PaginationOffset<Nothing>,
        filter: FilterChain<User>
    ): Flow<ViewQueryResultEvent> {
        throw BridgeException()
    }

    override fun findByNameEmailPhone(
        searchString: String,
        pagination: PaginationOffset<String>
    ): Flow<ViewQueryResultEvent> {
        throw BridgeException()
    }

    override fun findByPatientId(patientId: String): Flow<String> {
        throw BridgeException()
    }

    override suspend fun getUser(id: String): User? =
        getApi()?.getUser(id)?.let(userDtoToUser)

    override suspend fun getUserByEmail(email: String): User? {
        throw BridgeException()
    }

    override suspend fun getUserByGenericIdentifier(genericIdentifier: String): User? {
        throw BridgeException()
    }

    override suspend fun getUserByLogin(login: String): User? {
        throw BridgeException()
    }

    override suspend fun getUserByPhone(phone: String): User? {
        throw BridgeException()
    }

    override fun getUsers(ids: List<String>): Flow<User> {
        throw BridgeException()
    }

    override fun getUsersByLogin(login: String): Flow<User> {
        throw BridgeException()
    }

    override fun listUserIdsByHcpartyId(hcpartyId: String): Flow<String> = flow {
        emitAll(getApi()?.findByHcpartyId(hcpartyId)?.asFlow() ?: emptyFlow())
    }

    override fun listUserIdsByNameEmailPhone(searchString: String): Flow<String> {
        throw BridgeException()
    }

    override fun listUsers(
        paginationOffset: PaginationOffset<String>,
        skipPatients: Boolean
    ): Flow<PaginationElement> {
        throw BridgeException()
    }

    override suspend fun modifyUser(modifiedUser: User): User? {
        throw UnsupportedOperationException("The Kmehr module cannot modify users")
    }

    override suspend fun setProperties(userId: String, properties: List<PropertyStub>): User? {
        throw UnsupportedOperationException("The Kmehr module cannot modify users")
    }

    override suspend fun undeleteUser(userId: String) {
        throw UnsupportedOperationException("The Kmehr module cannot undelete users")
    }
}
