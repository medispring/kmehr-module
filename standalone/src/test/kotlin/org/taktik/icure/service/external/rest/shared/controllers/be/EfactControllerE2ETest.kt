@file:OptIn(InternalIcureApi::class)

package org.taktik.icure.service.external.rest.shared.controllers.be

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.icure.sdk.api.raw.impl.RawEntityReferenceApiImpl
import com.icure.sdk.api.raw.impl.RawHealthcarePartyApiImpl
import com.icure.sdk.api.raw.impl.RawInsuranceApiImpl
import com.icure.sdk.api.raw.impl.RawInvoiceApiImpl
import com.icure.sdk.api.raw.impl.RawPatientApiImpl
import com.icure.sdk.api.raw.impl.RawPermissionApiImpl
import com.icure.sdk.crypto.impl.NoAccessControlKeysHeadersProvider
import com.icure.sdk.model.EncryptedInvoice
import com.icure.sdk.model.EncryptedPatient
import com.icure.sdk.model.EntityReference
import com.icure.sdk.model.Insurance
import com.icure.sdk.model.embed.DecryptedAddress
import com.icure.sdk.model.embed.DecryptedFinancialInstitutionInformation
import com.icure.sdk.model.embed.EncryptedInvoicingCode
import com.icure.sdk.model.security.AlwaysPermissionItem
import com.icure.sdk.model.security.Permission
import com.icure.sdk.model.security.PermissionType
import com.icure.sdk.utils.InternalIcureApi
import com.icure.sdk.utils.Serialization
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.web.server.LocalServerPort
import org.taktik.icure.asynclogic.bridge.auth.KmehrAuthProvider
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.security.jwt.JwtUtils
import org.taktik.icure.service.external.rest.shared.controllers.be.EfactControllerE2ETest.Companion.createHcpWithBankInfo
import org.taktik.icure.service.external.rest.shared.controllers.be.EfactControllerE2ETest.Companion.createInsurance
import org.taktik.icure.service.external.rest.shared.controllers.be.EfactControllerE2ETest.Companion.generateInvoices
import org.taktik.icure.service.external.rest.shared.controllers.be.EfactControllerE2ETest.Companion.objectMapper
import org.taktik.icure.services.external.rest.v1.dto.be.efact.MessageWithBatch
import org.taktik.icure.test.BaseKmehrTest
import org.taktik.icure.test.KmehrTestApplication
import org.taktik.icure.test.TestHttpClient
import org.taktik.icure.test.UserCredentials
import org.taktik.icure.test.createHealthcarePartyUser
import org.taktik.icure.test.createPatientUser
import org.taktik.icure.test.getAuthProvider
import org.taktik.icure.test.ssin
import org.taktik.icure.test.testHttpClient
import org.taktik.icure.test.uuid
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EfactControllerE2ETest(
    @LocalServerPort val port: Int,
    val httpClient: TestHttpClient,
    val bridgeConfig: BridgeConfig,
    val jwtUtils: JwtUtils
) : BaseKmehrTest() {

    companion object {

        val objectMapper: ObjectMapper = ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())

        suspend fun generateInvoices(iCureUrl: String, jwt: String) =
            RawInvoiceApiImpl(
                apiUrl = iCureUrl,
                authProvider = KmehrAuthProvider(jwt),
                httpClient = testHttpClient,
                json = Serialization.json,
                accessControlKeysHeadersProvider = NoAccessControlKeysHeadersProvider
            ).createInvoices(
                (0 .. 2).fold(emptyList()) { acc, it ->
                    acc + EncryptedInvoice(
                        id = uuid(),
                        gnotionNihii = uuid(),
                        invoicingCodes = listOf(
                            EncryptedInvoicingCode(
                                id = uuid(),
                                reimbursement = Random.nextInt(1,10) / 10.0,
                                code = Random.nextLong(1000L, 9999L).toString(),
                                contract = uuid(),
                                units = it
                            )
                        )
                    )
                }
            ).successBody()

        suspend fun createInsurance(iCureUrl: String) = RawInsuranceApiImpl(
            apiUrl = iCureUrl,
            authProvider = getAuthProvider(iCureUrl, KmehrTestApplication.masterHcp.login, KmehrTestApplication.masterHcp.password),
            httpClient = testHttpClient,
            json = Serialization.json
        ).createInsurance(
            Insurance(
                id = uuid(),
                code = Random.nextLong(1000L, 9999L).toString(),
                privateInsurance = true,
                address = DecryptedAddress()
            )
        ).successBody()

        suspend fun createHcpWithBankInfo(
            iCureUrl: String,
            jwtUtils: JwtUtils,
            hasCbe: Boolean = false,
            hasNihii: Boolean = false,
            insuranceCode: String? = null
        ): UserCredentials {
            val hcp = createHealthcarePartyUser(
                iCureUrl,
                KmehrTestApplication.masterHcp.login,
                KmehrTestApplication.masterHcp.password,
                jwtUtils
            )
            RawHealthcarePartyApiImpl(
                apiUrl = iCureUrl,
                authProvider = getAuthProvider(iCureUrl, KmehrTestApplication.masterHcp.login, KmehrTestApplication.masterHcp.password),
                httpClient = testHttpClient,
                json = Serialization.json
            ).let { api ->
                val oldHcp = api.getHealthcareParty(hcp.dataOwnerId!!).successBody()
                api.modifyHealthcareParty(
                    oldHcp.copy(
                        cbe = ssin().takeIf { hasCbe },
                        nihii = ssin().takeIf { hasNihii },
                        ssin = ssin().takeIf { hasNihii },
                        financialInstitutionInformation =
                        insuranceCode?.let {
                            listOf(
                                DecryptedFinancialInstitutionInformation(
                                    key = insuranceCode,
                                    bic = uuid(),
                                    bankAccount = uuid()
                                )
                            )
                        } ?: emptyList()
                    )
                ).successBody()
            }
            RawPermissionApiImpl(
                apiUrl = iCureUrl,
                authProvider = getAuthProvider(iCureUrl, KmehrTestApplication.masterHcp.login, KmehrTestApplication.masterHcp.password),
                httpClient = testHttpClient,
                json = Serialization.json
            ).modifyUserPermissions(
                "${KmehrTestApplication.groupId}:${hcp.userId}",
                Permission(
                    grants = setOf(
                        AlwaysPermissionItem(
                            PermissionType.LegacyDataView
                        )
                    )
                )
            ).successBody()
            return hcp
        }
    }

    init {
        runBlocking {
            eFactControllerTest(httpClient, bridgeConfig.iCureUrl,"http://127.0.0.1:$port", jwtUtils)
        }
    }

}

private fun StringSpec.eFactControllerTest(
    httpClient: TestHttpClient,
    iCureUrl: String,
    khmerUrl: String,
    jwtUtils: JwtUtils
) {

    fun createMapOfIdsPayload(map: Map<String, List<String>>, apiVersion: String) =
        when(apiVersion) {
            "v1" -> objectMapper.writeValueAsString(
                org.taktik.icure.services.external.rest.v1.dto.MapOfIdsDto(mapOfIds = map)
            )
            "v2" -> objectMapper.writeValueAsString(
                org.taktik.icure.services.external.rest.v2.dto.MapOfIdsDto(mapOfIds = map)
            )
            else -> throw IllegalStateException("Wrong api version")
        }

    "Should be able to create a message with batch from a set of invoices" {
        val insurance = createInsurance(iCureUrl)
        val hcpCredentials = createHcpWithBankInfo(iCureUrl, jwtUtils, hasCbe = true, hasNihii = true, insuranceCode = insurance.code)

        val hcpApi = RawHealthcarePartyApiImpl(
            apiUrl = iCureUrl,
            authProvider = KmehrAuthProvider(hcpCredentials.authJWT.shouldNotBeNull()),
            httpClient = testHttpClient,
            json = Serialization.json
        )
        val entityRefApi = RawEntityReferenceApiImpl(
            apiUrl = iCureUrl,
            authProvider = KmehrAuthProvider(hcpCredentials.authJWT.shouldNotBeNull()),
            httpClient = testHttpClient,
            json = Serialization.json
        )
        val patientApi = RawPatientApiImpl(
            apiUrl = iCureUrl,
            authProvider = KmehrAuthProvider(hcpCredentials.authJWT.shouldNotBeNull()),
            httpClient = testHttpClient,
            json = Serialization.json,
            accessControlKeysHeadersProvider = NoAccessControlKeysHeadersProvider
        )

        val hcp = hcpApi.getHealthcareParty(hcpCredentials.dataOwnerId.shouldNotBeNull()).successBody()

        val lastReference = Random.nextInt(424200, 848400)
        entityRefApi.createEntityReference(
            EntityReference(
                id = "efact:${hcp.id}:${insurance.code}:${lastReference.toString().padStart(9 , '0')}",
                docId = uuid()
            )
        ).successBody()
        val patient1 = patientApi.createPatient(
            EncryptedPatient(
                id = uuid(),
                firstName = uuid(),
                lastName = uuid()
            )
        ).successBody()
        val patient2 = patientApi.createPatient(
            EncryptedPatient(
                id = uuid(),
                firstName = uuid(),
                lastName = uuid()
            )
        ).successBody()

        val invoices1 = generateInvoices(iCureUrl, hcpCredentials.authJWT)
        invoices1.size shouldBe 3
        val invoices2 = generateInvoices(iCureUrl, hcpCredentials.authJWT)
        invoices2.size shouldBe 3

        listOf(1, 2).forEach { version ->
            val messageId = uuid()
            val responseString = httpClient.makePostRequest(
                "$khmerUrl/rest/v$version/be_efact/${insurance.id}/$messageId/${Random.nextLong()}",
                createMapOfIdsPayload(mapOf(
                    patient1.id to invoices1.map { it.id },
                    patient2.id to invoices2.map { it.id }
                ), "v$version"),
                mapOf("Authorization" to "Bearer ${hcpCredentials.authJWT}")
            )
            val response = objectMapper.readValue<MessageWithBatch>(responseString)

           entityRefApi.getLatest("efact:${hcp.id}:${insurance.code}:").successBody()
                .let {
                    it.docId shouldBe messageId
                    it.id.split(":").last().toLong() shouldBe (lastReference + version)
                }

            response.message.id shouldBe messageId
            (invoices1 + invoices2).forEach {
                response.message.invoiceIds shouldContain it.id
            }
            response.message.delegations.size shouldBe 1
            response.message.delegations.keys shouldContain hcp.id

            response.invoicesBatch.ioFederationCode shouldBe insurance.code
            response.invoicesBatch.sender?.ssin shouldBe hcp.ssin!!.replace("[^0-9]".toRegex(), "")
            response.invoicesBatch.invoices.filter { it.patient!!.id == patient1.id }.size shouldBe 3
            response.invoicesBatch.invoices.filter { it.patient!!.id == patient2.id }.size shouldBe 3
        }
    }

    "A non-HCP that tries to create a message with batch will receive a 403" {
        val patientCredentials = createPatientUser(iCureUrl, KmehrTestApplication.masterHcp.login, KmehrTestApplication.masterHcp.password)
        listOf("v1", "v2").forEach { apiVersion ->
            httpClient.makePostRequest(
                "$khmerUrl/rest/$apiVersion/be_efact/${uuid()}/${uuid()}/${Random.nextLong()}",
                createMapOfIdsPayload(mapOf(uuid() to listOf(uuid())), apiVersion),
                mapOf("Authorization" to "Bearer ${patientCredentials.authJWT}"),
                403
            )
        }
    }

    "Creating a message with batch without a valid Insurance will receive in a 404" {
        val hcpCredentials = createHcpWithBankInfo(iCureUrl, jwtUtils, hasCbe = true, hasNihii = true)
        listOf("v1", "v2").forEach { apiVersion ->
            httpClient.makePostRequest(
                "$khmerUrl/rest/$apiVersion/be_efact/${uuid()}/${uuid()}/${Random.nextLong()}",
                createMapOfIdsPayload(mapOf(uuid() to listOf(uuid())), apiVersion),
                mapOf("Authorization" to "Bearer ${hcpCredentials.authJWT}"),
                404
            )
        }
    }

    "Creating a message with batch without a valid invoices will result in a 404" {
        val insurance = createInsurance(iCureUrl)
        val hcpCredentials = createHcpWithBankInfo(iCureUrl, jwtUtils, hasCbe = true, hasNihii = true)
        listOf("v1", "v2").forEach { apiVersion ->
            httpClient.makePostRequest(
                "$khmerUrl/rest/$apiVersion/be_efact/${insurance.id}/${uuid()}/${Random.nextLong()}",
                createMapOfIdsPayload(mapOf(uuid() to listOf(uuid())), apiVersion),
                mapOf("Authorization" to "Bearer ${hcpCredentials.authJWT}"),
                404
            )
        }
    }

    "Creating a message with batch with a HCP without a cbe will result in a 400" {
        val insurance = createInsurance(iCureUrl)
        val hcpCredentials = createHcpWithBankInfo(iCureUrl, jwtUtils, hasCbe = false, hasNihii = true, insuranceCode = insurance.code)

        val hcpApi = RawHealthcarePartyApiImpl(
            apiUrl = iCureUrl,
            authProvider = KmehrAuthProvider(hcpCredentials.authJWT.shouldNotBeNull()),
            httpClient = testHttpClient,
            json = Serialization.json
        )
        val entityRefApi = RawEntityReferenceApiImpl(
            apiUrl = iCureUrl,
            authProvider = KmehrAuthProvider(hcpCredentials.authJWT.shouldNotBeNull()),
            httpClient = testHttpClient,
            json = Serialization.json
        )
        val patientApi = RawPatientApiImpl(
            apiUrl = iCureUrl,
            authProvider = KmehrAuthProvider(hcpCredentials.authJWT.shouldNotBeNull()),
            httpClient = testHttpClient,
            json = Serialization.json,
            accessControlKeysHeadersProvider = NoAccessControlKeysHeadersProvider
        )

        val hcp = hcpApi.getHealthcareParty(hcpCredentials.dataOwnerId.shouldNotBeNull()).successBody()
        val lastReference = Random.nextInt(424200, 848400)
        entityRefApi.createEntityReference(
            EntityReference(
                id = "efact:${hcp.id}:${insurance.code}:${lastReference.toString().padStart(9 , '0')}",
                docId = uuid()
            )
        ).successBody()
        val patient = patientApi.createPatient(
            EncryptedPatient(
                id = uuid(),
                firstName = uuid(),
                lastName = uuid()
            )
        ).successBody()
        val invoices = generateInvoices(iCureUrl, hcpCredentials.authJWT)
        invoices.size shouldBe 3
        listOf("v1", "v2").forEach { apiVersion ->
            httpClient.makePostRequest(
                "$khmerUrl/rest/$apiVersion/be_efact/${insurance.id}/${uuid()}/${Random.nextLong()}",
                createMapOfIdsPayload(mapOf(patient.id to invoices.map { it.id }), apiVersion),
                mapOf("Authorization" to "Bearer ${hcpCredentials.authJWT}"),
                400
            )
        }
    }

    "Creating a message with batch with a HCP without a nihii will result in a 400" {
        val insurance = createInsurance(iCureUrl)
        val hcpCredentials = createHcpWithBankInfo(iCureUrl, jwtUtils, hasCbe = true, hasNihii = false, insuranceCode = insurance.code)

        val hcpApi = RawHealthcarePartyApiImpl(
            apiUrl = iCureUrl,
            authProvider = KmehrAuthProvider(hcpCredentials.authJWT.shouldNotBeNull()),
            httpClient = testHttpClient,
            json = Serialization.json
        )
        val entityRefApi = RawEntityReferenceApiImpl(
            apiUrl = iCureUrl,
            authProvider = KmehrAuthProvider(hcpCredentials.authJWT.shouldNotBeNull()),
            httpClient = testHttpClient,
            json = Serialization.json
        )
        val patientApi = RawPatientApiImpl(
            apiUrl = iCureUrl,
            authProvider = KmehrAuthProvider(hcpCredentials.authJWT.shouldNotBeNull()),
            httpClient = testHttpClient,
            json = Serialization.json,
            accessControlKeysHeadersProvider = NoAccessControlKeysHeadersProvider
        )

        val hcp = hcpApi.getHealthcareParty(hcpCredentials.dataOwnerId.shouldNotBeNull()).successBody()
        val lastReference = Random.nextInt(424200, 848400)
        entityRefApi.createEntityReference(
            EntityReference(
                id = "efact:${hcp.id}:${insurance.code}:${lastReference.toString().padStart(9 , '0')}",
                docId = uuid()
            )
        ).successBody()
        val patient = patientApi.createPatient(
            EncryptedPatient(
                id = uuid(),
                firstName = uuid(),
                lastName = uuid()
            )
        ).successBody()
        val invoices = generateInvoices(iCureUrl, hcpCredentials.authJWT)
        invoices.size shouldBe 3
        listOf("v1", "v2").forEach { apiVersion ->
            httpClient.makePostRequest(
                "$khmerUrl/rest/$apiVersion/be_efact/${insurance.id}/${uuid()}/${Random.nextLong()}",
                createMapOfIdsPayload(mapOf(patient.id to invoices.map { it.id }), apiVersion),
                mapOf("Authorization" to "Bearer ${hcpCredentials.authJWT}"),
                400
            )
        }
    }

    "Creating a message with batch with a HCP without banking information will result in a 400" {
        val insurance = createInsurance(iCureUrl)
        val hcpCredentials = createHcpWithBankInfo(iCureUrl, jwtUtils, hasCbe = true, hasNihii = true)

        val hcpApi = RawHealthcarePartyApiImpl(
            apiUrl = iCureUrl,
            authProvider = KmehrAuthProvider(hcpCredentials.authJWT.shouldNotBeNull()),
            httpClient = testHttpClient,
            json = Serialization.json
        )
        val entityRefApi = RawEntityReferenceApiImpl(
            apiUrl = iCureUrl,
            authProvider = KmehrAuthProvider(hcpCredentials.authJWT.shouldNotBeNull()),
            httpClient = testHttpClient,
            json = Serialization.json
        )
        val patientApi = RawPatientApiImpl(
            apiUrl = iCureUrl,
            authProvider = KmehrAuthProvider(hcpCredentials.authJWT.shouldNotBeNull()),
            httpClient = testHttpClient,
            json = Serialization.json,
            accessControlKeysHeadersProvider = NoAccessControlKeysHeadersProvider
        )

        val hcp = hcpApi.getHealthcareParty(hcpCredentials.dataOwnerId.shouldNotBeNull()).successBody()
        val lastReference = Random.nextInt(424200, 848400)
        entityRefApi.createEntityReference(
            EntityReference(
                id = "efact:${hcp.id}:${insurance.code}:${lastReference.toString().padStart(9 , '0')}",
                docId = uuid()
            )
        ).successBody()
        val patient = patientApi.createPatient(
            EncryptedPatient(
                id = uuid(),
                firstName = uuid(),
                lastName = uuid()
            )
        ).successBody()
        val invoices = generateInvoices(iCureUrl, hcpCredentials.authJWT)
        invoices.size shouldBe 3
        listOf("v1", "v2").forEach { apiVersion ->
            httpClient.makePostRequest(
                "$khmerUrl/rest/$apiVersion/be_efact/${insurance.id}/${uuid()}/${Random.nextLong()}",
                createMapOfIdsPayload(mapOf(patient.id to invoices.map { it.id }), apiVersion),
                mapOf("Authorization" to "Bearer ${hcpCredentials.authJWT}"),
                400
            )
        }
    }

}
