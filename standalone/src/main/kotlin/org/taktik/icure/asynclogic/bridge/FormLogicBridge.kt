package org.taktik.icure.asynclogic.bridge

import io.icure.kraken.client.apis.FormApi
import io.icure.kraken.client.security.ExternalJWTProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asynclogic.FormLogic
import org.taktik.icure.asynclogic.impl.BridgeAsyncSessionLogic
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.entities.Form
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.requests.BulkShareOrUpdateMetadataParams
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.exceptions.BridgeException
import org.taktik.icure.services.external.rest.v2.mapper.FormV2Mapper

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
@Service
class FormLogicBridge(
    private val asyncSessionLogic: BridgeAsyncSessionLogic,
    private val bridgeConfig: BridgeConfig,
    private val formMapper: FormV2Mapper
) : GenericLogicBridge<Form>(), FormLogic {

    private suspend fun getApi() = asyncSessionLogic.getCurrentJWT()?.let {
        FormApi(basePath = bridgeConfig.iCureUrl, authProvider = ExternalJWTProvider(it))
    }

    override suspend fun createForm(form: Form): Form? =
        getApi()?.createForm(form.let(formMapper::map))?.let(formMapper::map)

    override fun deleteForms(ids: Set<String>): Flow<DocIdentifier> {
        throw BridgeException()
    }

    override suspend fun getAllByLogicalUuid(formUuid: String): List<Form> {
        throw BridgeException()
    }

    override suspend fun addDelegation(formId: String, delegation: Delegation): Form? {
        throw BridgeException()
    }

    override suspend fun addDelegations(formId: String, delegations: List<Delegation>): Form? {
        throw BridgeException()
    }

    override fun solveConflicts(limit: Int?): Flow<IdAndRev> {
        throw BridgeException()
    }

    override fun bulkShareOrUpdateMetadata(requests: BulkShareOrUpdateMetadataParams): Flow<EntityBulkShareResult<Form>> {
        throw BridgeException()
    }

    override suspend fun getAllByUniqueId(lid: String): List<Form> {
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
