package org.taktik.icure.asynclogic.bridge

import com.icure.sdk.api.raw.impl.RawInvoiceApiImpl
import com.icure.sdk.crypto.impl.NoAccessControlKeysHeadersProvider
import com.icure.sdk.model.ListOfIds
import com.icure.sdk.utils.InternalIcureApi
import com.icure.sdk.utils.Serialization
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Service
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asynclogic.InvoiceLogic
import org.taktik.icure.asynclogic.bridge.auth.KmehrAuthProvider
import org.taktik.icure.asynclogic.bridge.mappers.InvoiceMapper
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
import org.taktik.icure.errors.UnauthorizedException
import org.taktik.icure.exceptions.BridgeException
import org.taktik.icure.pagination.PaginationElement

@Service
class InvoiceLogicBridge(
    private val asyncSessionLogic: BridgeAsyncSessionLogic,
    private val bridgeConfig: BridgeConfig,
    private val invoiceMapper: InvoiceMapper
) : GenericLogicBridge<Invoice>(), InvoiceLogic {

    @OptIn(InternalIcureApi::class)
    private suspend fun getApi() = asyncSessionLogic.getCurrentJWT()?.let { token ->
        RawInvoiceApiImpl(
            apiUrl = bridgeConfig.iCureUrl,
            authProvider = KmehrAuthProvider(token),
            httpClient = bridgeHttpClient,
            json = Serialization.json,
            accessControlKeysHeadersProvider = NoAccessControlKeysHeadersProvider
        )
    } ?: throw UnauthorizedException("You must be logged in to perform this operation")

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

    @OptIn(InternalIcureApi::class)
    override fun getInvoices(ids: List<String>): Flow<Invoice> = flow {
        emitAll(getApi()
            .getInvoices(ListOfIds(ids = ids))
            .successBody()
            .map(invoiceMapper::map)
            .asFlow()
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
