package org.taktik.icure.service.external.rest.shared.controllers.be

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.icure.kraken.client.apis.EntityrefApi
import io.icure.kraken.client.apis.HealthcarePartyApi
import io.icure.kraken.client.apis.InsuranceApi
import io.icure.kraken.client.apis.InvoiceApi
import io.icure.kraken.client.apis.PatientApi
import io.icure.kraken.client.apis.PermissionApi
import io.icure.kraken.client.security.BasicAuthProvider
import io.icure.kraken.client.security.ExternalJWTProvider
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.security.jwt.JwtUtils
import org.taktik.icure.service.external.rest.shared.controllers.be.EfactControllerE2ETest.Companion.createHcpWithBankInfo
import org.taktik.icure.service.external.rest.shared.controllers.be.EfactControllerE2ETest.Companion.createInsurance
import org.taktik.icure.service.external.rest.shared.controllers.be.EfactControllerE2ETest.Companion.generateInvoices
import org.taktik.icure.service.external.rest.shared.controllers.be.EfactControllerE2ETest.Companion.objectMapper
import org.taktik.icure.services.external.rest.v1.dto.be.efact.MessageWithBatch
import org.taktik.icure.services.external.rest.v2.dto.EntityReferenceDto
import org.taktik.icure.services.external.rest.v2.dto.InsuranceDto
import org.taktik.icure.services.external.rest.v2.dto.InvoiceDto
import org.taktik.icure.services.external.rest.v2.dto.PatientDto
import org.taktik.icure.services.external.rest.v2.dto.embed.FinancialInstitutionInformationDto
import org.taktik.icure.services.external.rest.v2.dto.embed.InvoicingCodeDto
import org.taktik.icure.services.external.rest.v2.dto.security.AlwaysPermissionItemDto
import org.taktik.icure.services.external.rest.v2.dto.security.PermissionDto
import org.taktik.icure.services.external.rest.v2.dto.security.PermissionTypeDto
import org.taktik.icure.test.KmehrTestApplication
import org.taktik.icure.test.TestHttpClient
import org.taktik.icure.test.UserCredentials
import org.taktik.icure.test.createHealthcarePartyUser
import org.taktik.icure.test.createPatientUser
import org.taktik.icure.test.ssin
import org.taktik.icure.test.uuid
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
@SpringBootTest(
    classes = [KmehrTestApplication::class],
    properties = [
        "spring.main.allow-bean-definition-overriding=true"
    ],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles(profiles = ["kmehr"])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EfactControllerE2ETest(
    @LocalServerPort val port: Int,
    val httpClient: TestHttpClient,
    val bridgeConfig: BridgeConfig,
    val jwtUtils: JwtUtils
) : StringSpec() {

    companion object {

        val objectMapper: ObjectMapper = ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())!!

        suspend fun generateInvoices(iCureUrl: String, jwt: String) =
            InvoiceApi(basePath = iCureUrl, authProvider = ExternalJWTProvider(jwt))
            .createInvoices(
                (0 .. 2).fold(emptyList()) { acc, it ->
                    acc + InvoiceDto(
                        id = uuid(),
                        gnotionNihii = uuid(),
                        invoicingCodes = listOf(
                            InvoicingCodeDto(
                                id = uuid(),
                                reimbursement = Random.nextInt(1,10) / 10.0,
                                code = Random.nextLong(1000L, 9999L).toString(),
                                contract = uuid(),
                                units = it
                            )
                        )
                    )
                }
            )

        suspend fun createInsurance(iCureUrl: String) = InsuranceApi(
            basePath = iCureUrl,
            authProvider = BasicAuthProvider(
                KmehrTestApplication.masterHcp.login,
                KmehrTestApplication.masterHcp.password
            )
        ).createInsurance(
            InsuranceDto(
                id = uuid(),
                code = Random.nextLong(1000L, 9999L).toString(),
                privateInsurance = true
            )
        )

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
            HealthcarePartyApi(
                basePath = iCureUrl,
                authProvider = BasicAuthProvider(
                    KmehrTestApplication.masterHcp.login,
                    KmehrTestApplication.masterHcp.password
                )
            ).let { api ->
                val oldHcp = api.getHealthcareParty(hcp.dataOwnerId!!)
                api.modifyHealthcareParty(
                    oldHcp.copy(
                        cbe = ssin().takeIf { hasCbe },
                        nihii = ssin().takeIf { hasNihii },
                        ssin = ssin().takeIf { hasNihii },
                        financialInstitutionInformation =
                        insuranceCode?.let {
                            listOf(
                                FinancialInstitutionInformationDto(
                                    key = insuranceCode,
                                    bic = uuid(),
                                    bankAccount = uuid()
                                )
                            )
                        } ?: emptyList()
                    )
                )
            }
            PermissionApi(
                basePath = iCureUrl,
                authProvider = BasicAuthProvider(
                    KmehrTestApplication.masterHcp.login,
                    KmehrTestApplication.masterHcp.password)
            ).modifyUserPermissions(
                    "${KmehrTestApplication.groupId}:${hcp.userId}",
                    PermissionDto(
                        grants = setOf(
                            AlwaysPermissionItemDto(
                                PermissionTypeDto.LEGACY_DATA_VIEW
                            )
                        )
                    )
                )
            return hcp
        }
    }

    init {
        runBlocking {
            eFactControllerTest(httpClient, bridgeConfig.iCureUrl,"http://127.0.0.1:$port", jwtUtils)
        }
    }

}

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
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

        val hcp = HealthcarePartyApi(basePath = iCureUrl, authProvider = ExternalJWTProvider(hcpCredentials.authJWT!!))
            .getHealthcareParty(hcpCredentials.dataOwnerId!!)

        val lastReference = Random.nextInt(424200, 848400)
        EntityrefApi(basePath = iCureUrl, authProvider = ExternalJWTProvider(hcpCredentials.authJWT))
            .createEntityReference(
                EntityReferenceDto(
                    id = "efact:${hcp.id}:${insurance.code}:${lastReference.toString().padStart(9 , '0')}",
                    docId = uuid()
                )
            )
        val patient1 = PatientApi(basePath = iCureUrl, authProvider = ExternalJWTProvider(hcpCredentials.authJWT))
            .createPatient(
                PatientDto(
                    id = uuid(),
                    firstName = uuid(),
                    lastName = uuid()
                )
            )
        val patient2 = PatientApi(basePath = iCureUrl, authProvider = ExternalJWTProvider(hcpCredentials.authJWT))
            .createPatient(
                PatientDto(
                    id = uuid(),
                    firstName = uuid(),
                    lastName = uuid()
                )
            )

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

            EntityrefApi(basePath = iCureUrl, authProvider = ExternalJWTProvider(hcpCredentials.authJWT))
                .getLatest("efact:${hcp.id}:${insurance.code}:")
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
        val hcp = HealthcarePartyApi(basePath = iCureUrl, authProvider = ExternalJWTProvider(hcpCredentials.authJWT!!))
            .getHealthcareParty(hcpCredentials.dataOwnerId!!)
        val lastReference = Random.nextInt(424200, 848400)
        EntityrefApi(basePath = iCureUrl, authProvider = ExternalJWTProvider(hcpCredentials.authJWT))
            .createEntityReference(
                EntityReferenceDto(
                    id = "efact:${hcp.id}:${insurance.code}:${lastReference.toString().padStart(9 , '0')}",
                    docId = uuid()
                )
            )
        val patient = PatientApi(basePath = iCureUrl, authProvider = ExternalJWTProvider(hcpCredentials.authJWT))
            .createPatient(
                PatientDto(
                    id = uuid(),
                    firstName = uuid(),
                    lastName = uuid()
                )
            )
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
        val hcp = HealthcarePartyApi(basePath = iCureUrl, authProvider = ExternalJWTProvider(hcpCredentials.authJWT!!))
            .getHealthcareParty(hcpCredentials.dataOwnerId!!)
        val lastReference = Random.nextInt(424200, 848400)
        EntityrefApi(basePath = iCureUrl, authProvider = ExternalJWTProvider(hcpCredentials.authJWT))
            .createEntityReference(
                EntityReferenceDto(
                    id = "efact:${hcp.id}:${insurance.code}:${lastReference.toString().padStart(9 , '0')}",
                    docId = uuid()
                )
            )
        val patient = PatientApi(basePath = iCureUrl, authProvider = ExternalJWTProvider(hcpCredentials.authJWT))
            .createPatient(
                PatientDto(
                    id = uuid(),
                    firstName = uuid(),
                    lastName = uuid()
                )
            )
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
        val hcp = HealthcarePartyApi(basePath = iCureUrl, authProvider = ExternalJWTProvider(hcpCredentials.authJWT!!))
            .getHealthcareParty(hcpCredentials.dataOwnerId!!)
        val lastReference = Random.nextInt(424200, 848400)
        EntityrefApi(basePath = iCureUrl, authProvider = ExternalJWTProvider(hcpCredentials.authJWT))
            .createEntityReference(
                EntityReferenceDto(
                    id = "efact:${hcp.id}:${insurance.code}:${lastReference.toString().padStart(9 , '0')}",
                    docId = uuid()
                )
            )
        val patient = PatientApi(basePath = iCureUrl, authProvider = ExternalJWTProvider(hcpCredentials.authJWT))
            .createPatient(
                PatientDto(
                    id = uuid(),
                    firstName = uuid(),
                    lastName = uuid()
                )
            )
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
