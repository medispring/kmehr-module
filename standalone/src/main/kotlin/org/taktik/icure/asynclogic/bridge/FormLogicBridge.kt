package org.taktik.icure.asynclogic.bridge

import com.icure.sdk.api.raw.impl.RawFormApiImpl
import com.icure.sdk.api.raw.successBodyOrNull404
import com.icure.sdk.crypto.impl.NoAccessControlKeysHeadersProvider
import com.icure.sdk.utils.InternalIcureApi
import com.icure.sdk.utils.Serialization
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service
import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.asynclogic.FormLogic
import org.taktik.icure.asynclogic.bridge.auth.KmehrAuthProvider
import org.taktik.icure.asynclogic.bridge.mappers.FormMapper
import org.taktik.icure.asynclogic.impl.BridgeAsyncSessionLogic
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.entities.Form
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.requests.BulkShareOrUpdateMetadataParams
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.errors.UnauthorizedException
import org.taktik.icure.exceptions.BridgeException

@Service
class FormLogicBridge(
    private val asyncSessionLogic: BridgeAsyncSessionLogic,
    private val bridgeConfig: BridgeConfig,
    private val formMapper: FormMapper
) : GenericLogicBridge<Form>(), FormLogic {

    @OptIn(InternalIcureApi::class)
    private suspend fun getApi() = asyncSessionLogic.getCurrentJWT()?.let { token ->
        RawFormApiImpl(
            apiUrl = bridgeConfig.iCureUrl,
            authProvider = KmehrAuthProvider(token),
            httpClient = bridgeHttpClient,
            json = Serialization.json,
            accessControlKeysHeadersProvider = NoAccessControlKeysHeadersProvider
        )
    } ?: throw UnauthorizedException("You must be logged in to perform this operation")

    @OptIn(InternalIcureApi::class)
    override suspend fun createForm(form: Form): Form? =
        getApi().createForm(form.let(formMapper::map)).successBodyOrNull404()?.let(formMapper::map)

    override fun deleteForms(ids: Set<String>): Flow<DocIdentifier> {
        throw BridgeException()
    }

    override suspend fun addDelegation(formId: String, delegation: Delegation): Form? {
        throw BridgeException()
    }

    override suspend fun addDelegations(formId: String, delegations: List<Delegation>): Form? {
        throw BridgeException()
    }

    override fun listFormsByLogicalUuid(formUuid: String, descending: Boolean): Flow<Form> {
        throw BridgeException()
    }

    override fun listFormsByUniqueId(lid: String, descending: Boolean): Flow<Form> {
        throw BridgeException()
    }

    override fun bulkShareOrUpdateMetadata(requests: BulkShareOrUpdateMetadataParams): Flow<EntityBulkShareResult<Form>> {
        throw BridgeException()
    }

    override suspend fun getForm(id: String): Form? {
        throw BridgeException()
    }

    override fun getForms(selectedIds: Collection<String>): Flow<Form> {
        throw BridgeException()
    }

    override fun listByHcPartyAndParentId(hcPartyId: String, formId: String): Flow<Form> {
        throw BridgeException()
    }

    override fun listFormsByHCPartyAndPatient(
        hcPartyId: String,
        secretPatientKeys: List<String>,
        healthElementId: String?,
        planOfActionId: String?,
        formTemplateId: String?
    ): Flow<Form> {
        throw BridgeException()
    }

    override fun listFormIdsByDataOwnerPatientOpeningDate(
        dataOwnerId: String,
        secretForeignKeys: Set<String>,
        startDate: Long?,
        endDate: Long?,
        descending: Boolean
    ): Flow<String> {
        throw BridgeException()
    }

    override suspend fun modifyForm(form: Form): Form? {
        throw BridgeException()
    }
}
