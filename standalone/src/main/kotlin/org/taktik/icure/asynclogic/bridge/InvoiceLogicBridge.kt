package org.taktik.icure.asynclogic.bridge

import io.icure.kraken.client.apis.InvoiceApi
import io.icure.kraken.client.security.ExternalJWTProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Service
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asynclogic.InvoiceLogic
import org.taktik.icure.asynclogic.impl.BridgeAsyncSessionLogic
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Invoice
import org.taktik.icure.entities.data.LabelledOccurence
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.InvoiceType
import org.taktik.icure.entities.embed.InvoicingCode
import org.taktik.icure.entities.embed.MediumType
import org.taktik.icure.entities.requests.BulkShareOrUpdateMetadataParams
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.exceptions.BridgeException
import org.taktik.icure.pagination.PaginationElement
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.mapper.InvoiceV2Mapper

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
@Service
class InvoiceLogicBridge(
    private val asyncSessionLogic: BridgeAsyncSessionLogic,
    private val bridgeConfig: BridgeConfig,
    private val invoiceMapper: InvoiceV2Mapper
) : GenericLogicBridge<Invoice>(), InvoiceLogic {

    private suspend fun getApi() = asyncSessionLogic.getCurrentJWT()?.let {
        InvoiceApi(basePath = bridgeConfig.iCureUrl, authProvider = ExternalJWTProvider(it))
    }

    override fun appendCodes(
        hcPartyId: String,
        userId: String,
        insuranceId: String?,
        secretPatientKeys: Set<String>,
        type: InvoiceType,
        sentMediumType: MediumType,
        invoicingCodes: List<InvoicingCode>,
        invoiceId: String?,
        invoiceGraceTime: Int?,
    ): Flow<Invoice> {
        throw BridgeException()
    }

    override fun bulkShareOrUpdateMetadata(requests: BulkShareOrUpdateMetadataParams): Flow<EntityBulkShareResult<Invoice>> {
        throw BridgeException()
    }

    override suspend fun createInvoice(invoice: Invoice): Invoice? {
        throw BridgeException()
    }

    override suspend fun deleteInvoice(invoiceId: String): DocIdentifier? {
        throw BridgeException()
    }

    override suspend fun addDelegation(invoiceId: String, delegation: Delegation): Invoice? {
        throw BridgeException()
    }

    override fun findInvoicesByAuthor(
        hcPartyId: String,
        fromDate: Long?,
        toDate: Long?,
        paginationOffset: PaginationOffset<ComplexKey>
    ): Flow<PaginationElement> {
        throw BridgeException()
    }

    override suspend fun addDelegations(invoiceId: String, delegations: List<Delegation>): Invoice? {
        throw BridgeException()
    }

    override suspend fun getInvoice(invoiceId: String): Invoice? {
        throw BridgeException()
    }

    override fun getInvoices(ids: List<String>): Flow<Invoice> = flow {
        emitAll(
            getApi()?.getInvoices(ListOfIdsDto(ids = ids))
                ?.map { invoiceMapper.map(it) }
                ?.asFlow() ?: emptyFlow()
        )
    }

    override fun getInvoicesForUsersAndInsuranceIds(userIds: List<String>?): Flow<Invoice> {
        throw BridgeException()
    }

    override suspend fun getTarificationsCodesOccurrences(
        hcPartyId: String,
        minOccurrences: Long
    ): List<LabelledOccurence> {
        throw BridgeException()
    }

    override fun getUnsentInvoicesForUsersAndInsuranceIds(userIds: List<String>?): Flow<Invoice> {
        throw BridgeException()
    }

    override fun listInvoiceIdsByTarificationsByCode(
        hcPartyId: String,
        codeCode: String?,
        startValueDate: Long?,
        endValueDate: Long?,
    ): Flow<String> {
        throw BridgeException()
    }

    override fun listInvoicesByHcPartyAndGroupId(hcParty: String, inputGroupId: String): Flow<Invoice> {
        throw BridgeException()
    }

    override fun listInvoicesByHcPartyAndPatientSksUnsent(
        hcParty: String,
        secretPatientKeys: Set<String>,
    ): Flow<Invoice> {
        throw BridgeException()
    }

    override fun listInvoicesByHcPartyAndRecipientIds(hcParty: String, recipientIds: Set<String?>): Flow<Invoice> {
        throw BridgeException()
    }

    override fun listInvoicesByHcPartyAndPatientSfks(hcParty: String, secretPatientKeys: Set<String>): Flow<Invoice> {
        throw BridgeException()
    }

    override fun listInvoiceIdsByDataOwnerPatientInvoiceDate(
        dataOwnerId: String,
        secretForeignKeys: Set<String>,
        startDate: Long?,
        endDate: Long?,
        descending: Boolean
    ): Flow<String> {
        throw BridgeException()
    }

    override fun listInvoicesByHcPartyAndRecipientIdsUnsent(
        hcParty: String,
        recipientIds: Set<String?>,
    ): Flow<Invoice> {
        throw BridgeException()
    }

    override fun listInvoicesByHcPartyContacts(hcParty: String, contactIds: Set<String>): Flow<Invoice> {
        throw BridgeException()
    }

    override fun listInvoicesByHcPartySendingModeStatus(
        hcParty: String,
        sendingMode: String?,
        status: String?,
        fromDate: Long?,
        toDate: Long?,
    ): Flow<Invoice> {
        throw BridgeException()
    }

    override fun listInvoicesByHcPartySentMediumTypeInvoiceTypeSentDate(
        hcParty: String,
        sentMediumType: MediumType,
        invoiceType: InvoiceType,
        sent: Boolean,
        fromDate: Long?,
        toDate: Long?,
    ): Flow<Invoice> {
        throw BridgeException()
    }

    override fun listInvoicesByServiceIds(serviceIds: Set<String>): Flow<Invoice> {
        throw BridgeException()
    }

    override fun listInvoicesHcpsByStatus(status: String, from: Long?, to: Long?, hcpIds: List<String>): Flow<Invoice> {
        throw BridgeException()
    }

    override fun solveConflicts(limit: Int?): Flow<IdAndRev> {
        throw BridgeException()
    }

    override fun listInvoicesIdsByTarificationsByCode(
        hcPartyId: String,
        codeCode: String,
        startValueDate: Long,
        endValueDate: Long,
    ): Flow<String> {
        throw BridgeException()
    }

    override suspend fun mergeInvoices(hcParty: String, invoices: List<Invoice>, destination: Invoice?): Invoice? {
        throw BridgeException()
    }

    override suspend fun modifyInvoice(invoice: Invoice): Invoice? {
        throw BridgeException()
    }

    override fun removeCodes(
        userId: String,
        secretPatientKeys: Set<String>,
        serviceId: String,
        inputTarificationIds: List<String>,
    ): Flow<Invoice> {
        throw BridgeException()
    }

    override suspend fun validateInvoice(
        hcParty: String,
        invoice: Invoice?,
        refScheme: String,
        forcedValue: String?,
    ): Invoice? {
        throw BridgeException()
    }
}
