package org.taktik.icure.asynclogic.bridge

import io.icure.kraken.client.apis.PatientApi
import io.icure.kraken.client.infrastructure.ClientException
import io.icure.kraken.client.models.filter.AbstractFilterDto
import io.icure.kraken.client.security.ExternalJWTProvider
import io.jsonwebtoken.JwtException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Service
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asynclogic.PatientLogic
import org.taktik.icure.asynclogic.impl.BridgeAsyncSessionLogic
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.db.Sorting
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.Gender
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.entities.requests.BulkShareOrUpdateMetadataParams
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.exceptions.BridgeException
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.PatientDto
import org.taktik.icure.services.external.rest.v2.mapper.PatientV2Mapper
import java.time.Instant

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
@Service
class PatientLogicBridge(
    private val asyncSessionLogic: BridgeAsyncSessionLogic,
    private val bridgeConfig: BridgeConfig,
    private val patientMapper: PatientV2Mapper
) : GenericLogicBridge<Patient>(), PatientLogic {

    private suspend fun getApi() = asyncSessionLogic.getCurrentJWT()?.let { token ->
        PatientApi(basePath = bridgeConfig.iCureUrl, authProvider = ExternalJWTProvider(token))
    }

    override suspend fun addDelegations(patientId: String, delegations: Collection<Delegation>): Patient? {
        throw BridgeException()
    }

    override fun bulkShareOrUpdateMetadata(requests: BulkShareOrUpdateMetadataParams): Flow<EntityBulkShareResult<Patient>> {
        throw BridgeException()
    }

    override suspend fun countByHcParty(healthcarePartyId: String): Int {
        throw BridgeException()
    }

    override suspend fun createPatient(patient: Patient): Patient? =
        getApi()?.createPatient(patientMapper.map(patient))?.let { patientMapper.map(it) }

    override fun createPatients(patients: List<Patient>): Flow<Patient> = flow {
        emitAll(
            getApi()?.let { api ->
                api.createPatients(patients.map(patientMapper::map))
                .let { result ->
                    api.getPatients(ListOfIdsDto(ids = result.map { it.id!! }))
                        .map(patientMapper::map)
                }.asFlow()
            } ?: emptyFlow()
        )
    }

    override suspend fun addDelegation(patientId: String, delegation: Delegation): Patient? {
        throw BridgeException()
    }

    override fun deletePatients(ids: Set<String>): Flow<DocIdentifier> {
        throw BridgeException()
    }

    override fun findByHcPartyAndIdentifier(healthcarePartyId: String, system: String, id: String): Flow<Patient> {
        throw BridgeException()
    }

    override fun findByHcPartyAndSsin(
        ssin: String?,
        healthcarePartyId: String,
        paginationOffset: PaginationOffset<List<String>>,
    ): Flow<ViewQueryResultEvent> {
        throw BridgeException()
    }

    override fun findByHcPartyAndSsinOrDateOfBirthOrNameContainsFuzzy(
        healthcarePartyId: String,
        offset: PaginationOffset<List<String>>,
        searchString: String?,
        sorting: Sorting,
    ): Flow<ViewQueryResultEvent> {
        throw BridgeException()
    }

    override fun findByHcPartyDateOfBirth(
        date: Int?,
        healthcarePartyId: String,
        paginationOffset: PaginationOffset<List<String>>,
    ): Flow<ViewQueryResultEvent> {
        throw BridgeException()
    }

    override fun findByHcPartyIdsOnly(
        healthcarePartyId: String,
        offset: PaginationOffset<List<String>>,
    ): Flow<ViewQueryResultEvent> {
        throw BridgeException()
    }

    override fun findByHcPartyModificationDate(
        start: Long?,
        end: Long?,
        healthcarePartyId: String,
        descending: Boolean,
        paginationOffset: PaginationOffset<List<String>>,
    ): Flow<ViewQueryResultEvent> {
        throw BridgeException()
    }

    override fun findByHcPartyNameContainsFuzzy(
        searchString: String?,
        healthcarePartyId: String,
        offset: PaginationOffset<*>,
        descending: Boolean,
    ): Flow<ViewQueryResultEvent> {
        throw BridgeException()
    }

    override suspend fun findByUserId(id: String): Patient? {
        throw BridgeException()
    }

    override fun findDeletedPatientsByDeleteDate(
        start: Long,
        end: Long?,
        descending: Boolean,
        paginationOffset: PaginationOffset<Long>,
    ): Flow<ViewQueryResultEvent> {
        throw BridgeException()
    }

    override fun findOfHcPartyNameContainsFuzzy(
        searchString: String?,
        healthcarePartyId: String,
        offset: PaginationOffset<*>,
        descending: Boolean,
    ): Flow<ViewQueryResultEvent> {
        throw BridgeException()
    }

    override fun findOfHcPartyAndSsinOrDateOfBirthOrNameContainsFuzzy(
        healthcarePartyId: String,
        offset: PaginationOffset<ComplexKey>,
        searchString: String?,
        sorting: Sorting
    ): Flow<ViewQueryResultEvent> {
        throw BridgeException()
    }

    override fun fuzzySearchPatients(
        firstName: String?,
        lastName: String?,
        dateOfBirth: Int?,
        healthcarePartyId: String?,
    ): Flow<Patient> {
        throw BridgeException()
    }

    override suspend fun getAesExchangeKeysForDelegate(healthcarePartyId: String): Map<String, Map<String, Map<String, String>>> {
        throw BridgeException()
    }

    override suspend fun getByExternalId(externalId: String): Patient? {
        throw BridgeException()
    }

    override fun solveConflicts(limit: Int?): Flow<IdAndRev> {
        throw BridgeException()
    }

    override fun getDuplicatePatientsByName(
        healthcarePartyId: String,
        paginationOffset: PaginationOffset<List<String>>,
    ): Flow<ViewQueryResultEvent> {
        throw BridgeException()
    }

    override fun getDuplicatePatientsBySsin(
        healthcarePartyId: String,
        paginationOffset: PaginationOffset<List<String>>,
    ): Flow<ViewQueryResultEvent> {
        throw BridgeException()
    }

    @Deprecated("A DataOwner may now have multiple AES Keys. Use getAesExchangeKeysForDelegate instead")
    override suspend fun getHcPartyKeysForDelegate(healthcarePartyId: String): Map<String, String> {
        throw BridgeException()
    }

    override suspend fun getPatient(patientId: String): Patient? =
        try {
            getApi()?.getPatient(patientId)?.let { patientMapper.map(it) }
        } catch (e: ClientException) {
            null
        }

    override fun getPatients(patientIds: Collection<String>): Flow<Patient> = flow {
        emitAll(
            getApi()
                ?.getPatients(ListOfIdsDto(ids = patientIds.toList()))
                ?.map(patientMapper::map)
                ?.asFlow() ?: emptyFlow()
        )
    }

    override fun listByHcPartyAndActiveIdsOnly(active: Boolean, healthcarePartyId: String): Flow<String> {
        throw BridgeException()
    }

    override fun listByHcPartyAndExternalIdsOnly(externalId: String?, healthcarePartyId: String): Flow<String> {
        throw BridgeException()
    }

    private fun filterToFlowOfString(
        filter: AbstractFilterDto<PatientDto>,
        startKey: String? = null,
        startDocumentId: String? = null
    ): Flow<String> = flow {
        val result = getApi()?.filterPatientsBy(
            io.icure.kraken.client.models.filter.chain.FilterChain(filter),
            startKey = startKey,
            startDocumentId = startDocumentId,
            limit = 1000,
            skip = null,
            sort = null,
            desc = false
        ) ?: throw JwtException("Not authorized")
        emitAll(result.rows.map { it.id }.asFlow())
        if(result.nextKeyPair != null) emitAll(
            filterToFlowOfString(
                filter,
                result.nextKeyPair?.startKey as? String,
                result.nextKeyPair?.startKeyDocId
            )
        )
    }

    override fun listByHcPartyAndSsinIdsOnly(ssin: String, healthcarePartyId: String): Flow<String> = flow {
        emitAll(
            filterToFlowOfString(
                io.icure.kraken.client.models.filter.patient.PatientByHcPartyAndSsinFilter(
                    healthcarePartyId = healthcarePartyId,
                    ssin = ssin
                )
            )
        )
    }

    override fun listByHcPartyAndSsinsIdsOnly(ssins: Collection<String>, healthcarePartyId: String): Flow<String> {
        throw BridgeException()
    }

    override fun listByHcPartyDateOfBirthIdsOnly(date: Int, healthcarePartyId: String): Flow<String> = flow {
        emitAll(
            filterToFlowOfString(
                io.icure.kraken.client.models.filter.patient.PatientByHcPartyDateOfBirthFilter(
                    healthcarePartyId = healthcarePartyId,
                    dateOfBirth = date
                )
            )
        )
    }

    override fun listByHcPartyDateOfBirthIdsOnly(
        startDate: Int?,
        endDate: Int?,
        healthcarePartyId: String,
    ): Flow<String> {
        throw BridgeException()
    }

    override fun listByHcPartyGenderEducationProfessionIdsOnly(
        healthcarePartyId: String,
        gender: Gender?,
        education: String?,
        profession: String?,
    ): Flow<String> {
        throw BridgeException()
    }

    override fun listByHcPartyIdsOnly(healthcarePartyId: String): Flow<String> {
        throw BridgeException()
    }

    override fun listByHcPartyName(searchString: String?, healthcarePartyId: String): Flow<String> {
        throw BridgeException()
    }

    override fun listByHcPartyNameContainsFuzzyIdsOnly(searchString: String?, healthcarePartyId: String): Flow<String> = flow {
        emitAll(
            filterToFlowOfString(
                io.icure.kraken.client.models.filter.patient.PatientByHcPartyNameContainsFuzzyFilter(
                    searchString = searchString,
                    healthcarePartyId = healthcarePartyId
                )
            )
        )
    }

    override fun listDeletedPatientsByNames(firstName: String?, lastName: String?): Flow<Patient> {
        throw BridgeException()
    }

    override fun listOfMergesAfter(date: Long?): Flow<Patient> {
        throw BridgeException()
    }

    override fun listOfPatientsModifiedAfter(
        date: Long,
        startKey: Long?,
        startDocumentId: String?,
        limit: Int?,
    ): Flow<ViewQueryResultEvent> {
        throw BridgeException()
    }

    override fun listPatientIdsByHcPartyAndAddressOnly(searchString: String?, healthcarePartyId: String): Flow<String> {
        throw BridgeException()
    }

    override fun listPatientIdsByHcPartyAndAddressOnly(
        streetAndCity: String?,
        postalCode: String?,
        houseNumber: String?,
        healthcarePartyId: String
    ): Flow<String> {
        throw BridgeException()
    }

    override fun listPatientIdsByHcPartyAndTelecomOnly(searchString: String?, healthcarePartyId: String): Flow<String> {
        throw BridgeException()
    }

    override fun listPatientIdsByHcpartyAndIdentifiers(
        healthcarePartyId: String,
        identifiers: List<Identifier>,
    ): Flow<String> {
        throw BridgeException()
    }

    override fun listPatients(
        paginationOffset: PaginationOffset<*>,
        filterChain: FilterChain<Patient>,
        sort: String?,
        desc: Boolean?,
    ): Flow<ViewQueryResultEvent> {
        throw BridgeException()
    }

    override suspend fun mergePatients(fromId: String, expectedFromRev: String, updatedInto: Patient): Patient {
        throw BridgeException()
    }

    override suspend fun modifyPatient(patient: Patient): Patient? {
        throw BridgeException()
    }

    override suspend fun modifyPatientReferral(
        patient: Patient,
        referralId: String?,
        start: Instant?,
        end: Instant?,
    ): Patient? {
        throw BridgeException()
    }

    override fun modifyPatients(patients: Collection<Patient>): Flow<Patient> {
        throw BridgeException()
    }

    override fun undeletePatients(ids: Set<String>): Flow<DocIdentifier> {
        throw BridgeException()
    }
}
