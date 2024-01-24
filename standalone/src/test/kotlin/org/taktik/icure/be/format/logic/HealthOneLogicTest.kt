package org.taktik.icure.be.format.logic

import io.icure.kraken.client.infrastructure.ClientException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestInstance
import org.taktik.icure.asynclogic.bridge.ContactLogicBridge
import org.taktik.icure.asynclogic.bridge.DocumentLogicBridge
import org.taktik.icure.asynclogic.objectstorage.DataAttachmentChange
import org.taktik.icure.be.format.logic.impl.HealthOneLogicImpl
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.Document
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.security.jwt.JwtUtils
import org.taktik.icure.test.BaseKmehrTest
import org.taktik.icure.test.KmehrTestApplication
import org.taktik.icure.test.UserCredentials
import org.taktik.icure.test.createHealthcarePartyUser
import org.taktik.icure.test.uuid
import org.taktik.icure.test.withAuthenticatedReactorContext
import org.taktik.icure.utils.toDataBuffer
import java.io.File
import java.nio.ByteBuffer
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HealthOneLogicTest(
    private val healthOneLogic: HealthOneLogicImpl,
    private val bridgeConfig: BridgeConfig,
    private val documentLogic: DocumentLogicBridge,
    private val contactLogic: ContactLogicBridge,
    private val jwtUtils: JwtUtils
) : BaseKmehrTest() {

    companion object {

        val controValues = mapOf(
            "Globules rouges" to 4.5,
            "Hémoglobine" to 14.8,
            "Hématocrite" to 42.0,
            "Vol.globulaire moyen" to 90.0,
            "Teneur globulaire en HB" to 30.4,
            "Conc.glob.moyen.en HB" to 35.1,
            "Indice distribution GR" to 15.0,
            "Globules blancs" to 8.6
        )

        data class HealthOneDocument(
            val rawDocument: String,
            val clinicName: String,
            val protocolId: String,
            val language: String,
            val formId: String,
            val planOfActionId: String
        )

        fun loadHealthOneDocumentFromFile(language: String, formId: String, planOfActionId: String) : HealthOneDocument {
            val protocolId = Random.nextInt(1_000_000, 9_999_999).toString()
            val clinicName = uuid()
            val content = File("src/test/resources/org/taktik/icure/be/format/logic/healthOneAttachmentLab.txt")
                .readText()
                .replace("{PROTOCOL_ID}", protocolId)
                .replace("{CLININC_NAME}", clinicName)
            return HealthOneDocument(content, clinicName, protocolId, language, formId, planOfActionId)
        }
    }

    val hcpCredentials: UserCredentials = runBlocking { createHealthcarePartyUser(
        bridgeConfig.iCureUrl,
        KmehrTestApplication.masterHcp.login,
        KmehrTestApplication.masterHcp.password,
        jwtUtils
    ) }

    init {
        healthOneLogicTest()
    }

    private suspend fun createDocumentWithAttachmentFor(credentials: UserCredentials, content: String): Document {
        val document = documentLogic.createDocument(
            Document(
                id = uuid(),
                name = uuid(),
                delegations = mapOf(credentials.dataOwnerId.shouldNotBeNull() to setOf())
            )
        ).shouldNotBeNull()
        val fakeAttachment = content.toByteArray(Charsets.UTF_8)
        return documentLogic.updateAttachments(
            document,
            DataAttachmentChange.CreateOrUpdate(
                flowOf(ByteBuffer.wrap(fakeAttachment).toDataBuffer()),
                fakeAttachment.size.toLong(),
                listOf("public.plain-text")
            )
        ).shouldNotBeNull()
    }

    private fun StringSpec.healthOneLogicTest() {

        val transactionCodeStub = CodeStub(
            id = "CD-TRANSACTION|labresult|1",
            type = "CD-TRANSACTION",
            code = "labresult",
            version = "1"
        )

        suspend fun testImportedContact(importedContact: Contact, existingContact: Contact, document: HealthOneDocument) {
            importedContact.id shouldBe existingContact.id
            importedContact.rev shouldNotBe existingContact.rev
            importedContact.services.size shouldBe controValues.size
            importedContact.services.forEach { service ->
                service.label shouldNotBe null
                val controlValue = controValues[service.label.shouldNotBeNull()].shouldNotBeNull()
                service.content[document.language].shouldNotBeNull().also {
                    it.measureValue.shouldNotBeNull().value shouldBe controlValue
                }
            }
            importedContact.subContacts.size shouldBe 1
            importedContact.subContacts.first().also { ctc ->
                ctc.services.map { it.serviceId } shouldContainExactlyInAnyOrder importedContact.services.map { it.id }
                ctc.descr shouldBe document.clinicName
                ctc.protocol shouldBe document.protocolId
                ctc.formId shouldBe document.formId
                ctc.planOfActionId shouldBe document.planOfActionId
            }

            contactLogic.getContact(importedContact.id) shouldBe importedContact
        }

        listOf("A1/${uuid()}\n${uuid()}", "123 S1 example text\n${uuid()}").mapIndexed { index, s ->
            "HealthOneLogic can handle a module if the document main attachment starts with a line in the correct format - $index" {
                withAuthenticatedReactorContext(hcpCredentials) {
                    val documentWithAttachment = createDocumentWithAttachmentFor(hcpCredentials, s)
                    healthOneLogic.canHandle(documentWithAttachment, emptyList(), null) shouldBe true
                }
            }
        }

        "HealthOneLogic cannot handle a document with no main attachment" {
            withAuthenticatedReactorContext(hcpCredentials) {
                val document = documentLogic.createDocument(
                    Document(
                        id = uuid(),
                        name = uuid(),
                        delegations = mapOf(hcpCredentials.dataOwnerId.shouldNotBeNull() to setOf())
                    )
                ).shouldNotBeNull()
                healthOneLogic.canHandle(document, emptyList(), null) shouldBe false
            }
        }

        "HealthOneLogic cannot handle a module if the document main attachment starts with a line in the wrong format" {
            withAuthenticatedReactorContext(hcpCredentials) {
                val documentWithAttachment = createDocumentWithAttachmentFor(hcpCredentials, "${uuid()}\n${uuid()}")
                healthOneLogic.canHandle(documentWithAttachment, emptyList(), null) shouldBe false
            }
        }

        "HealthOneLogic can import a report where the contact exists, updating it" {
            val healthDocument = loadHealthOneDocumentFromFile("fr", "0", uuid())
            withAuthenticatedReactorContext(hcpCredentials) {
                val document = createDocumentWithAttachmentFor(hcpCredentials, healthDocument.rawDocument)
                val newContact = contactLogic.createContact(Contact(
                    id = uuid(),
                    tags = setOf(transactionCodeStub),
                    delegations = mapOf(hcpCredentials.dataOwnerId.shouldNotBeNull() to setOf()))).shouldNotBeNull()
                val importedContact = healthOneLogic.doImport(
                    "fr",
                    document,
                    hcpCredentials.dataOwnerId,
                    protocolIds = listOf(healthDocument.protocolId),
                    formIds = listOf(healthDocument.formId),
                    planOfActionId = healthDocument.planOfActionId,
                    ctc = newContact,
                    encKeys = emptyList()
                ).shouldNotBeNull()
                testImportedContact(importedContact, newContact, healthDocument)
            }
        }

        "HealthOneLogic cannot import a report where the contact exists but a wrong revision is passed" {
            val healthDocument = loadHealthOneDocumentFromFile("fr", "0", uuid())
            withAuthenticatedReactorContext(hcpCredentials) {
                val document = createDocumentWithAttachmentFor(hcpCredentials, healthDocument.rawDocument)
                val newContact = contactLogic.createContact(Contact(
                    id = uuid(),
                    tags = setOf(transactionCodeStub),
                    delegations = mapOf(hcpCredentials.dataOwnerId.shouldNotBeNull() to setOf()))).shouldNotBeNull()
                contactLogic.modifyEntities(listOf(
                    newContact.copy(groupId = uuid())
                )).firstOrNull().shouldNotBeNull()
                shouldThrow<IllegalArgumentException> {
                    healthOneLogic.doImport(
                        "fr",
                        document,
                        hcpCredentials.dataOwnerId,
                        protocolIds = listOf(healthDocument.protocolId),
                        formIds = listOf(healthDocument.formId),
                        planOfActionId = healthDocument.planOfActionId,
                        ctc = newContact,
                        encKeys = emptyList()
                    ).shouldNotBeNull()
                }
            }
        }

        "HealthOneLogic can import a report where the contact does not exist, creating it" {
            val healthDocument = loadHealthOneDocumentFromFile("fr", "0", uuid())
            withAuthenticatedReactorContext(hcpCredentials) {
                val document = createDocumentWithAttachmentFor(hcpCredentials, healthDocument.rawDocument)
                val newContact = Contact(
                    id = uuid(),
                    tags = setOf(transactionCodeStub),
                    delegations = mapOf(hcpCredentials.dataOwnerId.shouldNotBeNull() to setOf()))
                val importedContact = healthOneLogic.doImport(
                    "fr",
                    document,
                    hcpCredentials.dataOwnerId,
                    protocolIds = listOf(healthDocument.protocolId),
                    formIds = listOf(healthDocument.formId),
                    planOfActionId = healthDocument.planOfActionId,
                    ctc = newContact,
                    encKeys = emptyList()
                ).shouldNotBeNull()
                testImportedContact(importedContact, newContact, healthDocument)
            }
        }

        "HealthOneLogic cannot import a report creating the contact if the contact does not have any delegation" {
            val healthDocument = loadHealthOneDocumentFromFile("fr", "0", uuid())
            withAuthenticatedReactorContext(hcpCredentials) {
                val document = createDocumentWithAttachmentFor(hcpCredentials, healthDocument.rawDocument)
                val newContact = Contact(
                    id = uuid(),
                    tags = setOf(transactionCodeStub)
                )
                shouldThrow<IllegalArgumentException> {
                    healthOneLogic.doImport(
                        "fr",
                        document,
                        hcpCredentials.dataOwnerId,
                        protocolIds = listOf(healthDocument.protocolId),
                        formIds = listOf(healthDocument.formId),
                        planOfActionId = healthDocument.planOfActionId,
                        ctc = newContact,
                        encKeys = emptyList()
                    )
                }
            }
        }
    }

}