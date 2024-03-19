package org.taktik.icure.be.format.logic

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestInstance
import org.taktik.icure.asynclogic.bridge.ContactLogicBridge
import org.taktik.icure.asynclogic.bridge.DocumentLogicBridge
import org.taktik.icure.asynclogic.objectstorage.DataAttachmentChange
import org.taktik.icure.be.format.logic.impl.MedidocLogicImpl
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
class MedidocLogicTest(
	private val medidocLogic: MedidocLogicImpl,
	private val bridgeConfig: BridgeConfig,
	private val documentLogic: DocumentLogicBridge,
	private val contactLogic: ContactLogicBridge,
	private val jwtUtils: JwtUtils
) : BaseKmehrTest() {

	companion object {
		data class MediDocDocument(
			val rawDocument: String,
			val clinicName: String,
			val protocolId: String,
			val language: String,
			val formId: String,
			val planOfActionId: String
		)

		fun loadHealthOneDocumentFromFile(language: String, formId: String, planOfActionId: String) : MediDocDocument {
			val protocolId = Random.nextInt(1_000_000, 9_999_999).toString()
			val clinicName = uuid()
			val content = File("src/test/resources/org/taktik/icure/be/format/logic/mediDocAttachment.dat").readText()
			return MediDocDocument(content, clinicName, protocolId, language, formId, planOfActionId)
		}
	}

	val hcpCredentials: UserCredentials = runBlocking { createHealthcarePartyUser(
		bridgeConfig.iCureUrl,
		KmehrTestApplication.masterHcp.login,
		KmehrTestApplication.masterHcp.password,
		jwtUtils
	) }

	init {
		medidocLogicTest()
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
				listOf("public.plain-text"),
				false
			)
		).shouldNotBeNull()
	}

	private fun StringSpec.medidocLogicTest() {

		val transactionCodeStub = CodeStub(
			id = "CD-TRANSACTION|labresult|1",
			type = "CD-TRANSACTION",
			code = "labresult",
			version = "1"
		)

		"MedidocLogic can handle a file if the document main attachment starts with a line in the correct format" {
			val content = File("src/test/resources/org/taktik/icure/be/format/logic/mediDocAttachment.dat").readText()
			withAuthenticatedReactorContext(hcpCredentials) {
				val documentWithAttachment = createDocumentWithAttachmentFor(hcpCredentials, content)
				medidocLogic.canHandle(documentWithAttachment, emptyList(), null) shouldBe true
			}
		}

		"MedidocLogic cannot handle a document with no main attachment" {
			withAuthenticatedReactorContext(hcpCredentials) {
				val document = documentLogic.createDocument(
					Document(
						id = uuid(),
						name = uuid(),
						delegations = mapOf(hcpCredentials.dataOwnerId.shouldNotBeNull() to setOf())
					)
				).shouldNotBeNull()
				medidocLogic.canHandle(document, emptyList(), null) shouldBe false
			}
		}

		"MedidocLogic cannot handle a file if the document main attachment does not match the correct format" {
			withAuthenticatedReactorContext(hcpCredentials) {
				val documentWithAttachment = createDocumentWithAttachmentFor(hcpCredentials, "${uuid()}\n${uuid()}")
				medidocLogic.canHandle(documentWithAttachment, emptyList(), null) shouldBe false
			}
		}

		"HealthOneLogic can import a file where the contact exists, updating it" {
			val healthDocument = loadHealthOneDocumentFromFile("fr", "0", uuid())
			withAuthenticatedReactorContext(hcpCredentials) {
				val document = createDocumentWithAttachmentFor(hcpCredentials, healthDocument.rawDocument)
				val newContact = contactLogic.createContact(
					Contact(
					id = uuid(),
					tags = setOf(transactionCodeStub),
					delegations = mapOf(hcpCredentials.dataOwnerId.shouldNotBeNull() to setOf())
					)
				).shouldNotBeNull()
				medidocLogic.doImport(
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

		"MedidocLogic cannot import a file where the contact exists but a wrong revision is passed" {
			val healthDocument = loadHealthOneDocumentFromFile("fr", "0", uuid())
			withAuthenticatedReactorContext(hcpCredentials) {
				val document = createDocumentWithAttachmentFor(hcpCredentials, healthDocument.rawDocument)
				val newContact = contactLogic.createContact(
					Contact(
					id = uuid(),
					tags = setOf(transactionCodeStub),
					delegations = mapOf(hcpCredentials.dataOwnerId.shouldNotBeNull() to setOf())
					)
				).shouldNotBeNull()
				contactLogic.modifyEntities(
					listOf(
					newContact.copy(groupId = uuid())
				)
				).firstOrNull().shouldNotBeNull()
				shouldThrow<IllegalArgumentException> {
					medidocLogic.doImport(
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

		"MedidocLogic can import a file where the contact does not exist, creating it" {
			val healthDocument = loadHealthOneDocumentFromFile("fr", "0", uuid())
			withAuthenticatedReactorContext(hcpCredentials) {
				val document = createDocumentWithAttachmentFor(hcpCredentials, healthDocument.rawDocument)
				val newContact = Contact(
					id = uuid(),
					tags = setOf(transactionCodeStub),
					delegations = mapOf(hcpCredentials.dataOwnerId.shouldNotBeNull() to setOf())
				)
				medidocLogic.doImport(
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

		"HealthOneLogic cannot import a report creating the contact if the contact does not have any delegation" {
			val healthDocument = loadHealthOneDocumentFromFile("fr", "0", uuid())
			withAuthenticatedReactorContext(hcpCredentials) {
				val document = createDocumentWithAttachmentFor(hcpCredentials, healthDocument.rawDocument)
				val newContact = Contact(
					id = uuid(),
					tags = setOf(transactionCodeStub)
				)
				shouldThrow<IllegalArgumentException> {
					medidocLogic.doImport(
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