package org.taktik.icure.asynclogic.bridge

import com.icure.sdk.api.raw.impl.RawUserApiImpl
import com.icure.sdk.api.raw.successBodyOrNull404
import com.icure.sdk.utils.InternalIcureApi
import com.icure.sdk.utils.Serialization
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Service
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.asynclogic.UserLogic
import org.taktik.icure.asynclogic.bridge.auth.KmehrAuthProvider
import org.taktik.icure.asynclogic.bridge.mappers.UserMapper
import org.taktik.icure.asynclogic.impl.BridgeAsyncSessionLogic
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.User
import org.taktik.icure.entities.base.PropertyStub
import org.taktik.icure.errors.UnauthorizedException
import org.taktik.icure.exceptions.BridgeException
import org.taktik.icure.pagination.PaginationElement

@Service
class UserLogicBridge(
    private val asyncSessionLogic: BridgeAsyncSessionLogic,
    private val bridgeConfig: BridgeConfig,
    private val userMapper: UserMapper
) : GenericLogicBridge<User>(), UserLogic {

    @OptIn(InternalIcureApi::class)
    private suspend fun getApi() = asyncSessionLogic.getCurrentJWT()?.let { token ->
        RawUserApiImpl(
            apiUrl = bridgeConfig.iCureUrl,
            authProvider = KmehrAuthProvider(token),
            httpClient = bridgeHttpClient,
            json = Serialization.json
        )
    } ?: throw UnauthorizedException("You must be logged in to perform this operation")

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

    @OptIn(InternalIcureApi::class)
    override suspend fun getUser(id: String): User? =
        getApi().getUser(id).successBodyOrNull404()?.let(userMapper::map)

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

    @OptIn(InternalIcureApi::class)
    override fun listUserIdsByHcpartyId(hcpartyId: String): Flow<String> = flow {
        emitAll(getApi().findByHcpartyId(hcpartyId).successBody().asFlow())
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
