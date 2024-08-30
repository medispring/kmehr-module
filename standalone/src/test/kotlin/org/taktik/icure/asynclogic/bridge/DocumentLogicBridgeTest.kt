package org.taktik.icure.asynclogic.bridge

import com.icure.sdk.utils.RequestStatusException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestInstance
import org.taktik.icure.asynclogic.bridge.mappers.DocumentMapper
import org.taktik.icure.asynclogic.objectstorage.DataAttachmentChange
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.entities.Document
import org.taktik.icure.security.jwt.JwtUtils
import org.taktik.icure.test.BaseKmehrTest
import org.taktik.icure.test.KmehrTestApplication
import org.taktik.icure.test.UserCredentials
import org.taktik.icure.test.createHealthcarePartyUser
import org.taktik.icure.test.uuid
import org.taktik.icure.test.withAuthenticatedReactorContext
import org.taktik.icure.utils.toDataBuffer
import java.nio.ByteBuffer
import java.nio.charset.Charset

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DocumentLogicBridgeTest(
    val bridgeConfig: BridgeConfig,
    val documentMapper: DocumentMapper,
    val jwtUtils: JwtUtils
) : BaseKmehrTest() {

    init {
        runBlocking {
            val hcp = createHealthcarePartyUser(
                bridgeConfig.iCureUrl,
                KmehrTestApplication.masterHcp.login,
                KmehrTestApplication.masterHcp.password,
                jwtUtils
            )

            val documentBridge = DocumentLogicBridge(
                KmehrTestApplication.fakeSessionLogic,
                bridgeConfig,
                documentMapper
            )

            documentLogicBridgeTest(hcp, documentBridge)
        }
    }
}

private fun ByteBuffer.decodeString(charset: Charset): String {
    val bytes = if(hasArray()) {
        array()
    } else {
        val tmpBytes = ByteArray(remaining())
        get(tmpBytes)
        tmpBytes
    }
    return String(bytes, charset)
}

private fun StringSpec.documentLogicBridgeTest(
    credentials: UserCredentials,
    documentBridge: DocumentLogicBridge
) {

    "Can get a Document" {
        withAuthenticatedReactorContext(credentials) {
            val document = documentBridge.createDocument(
                Document(
                    id = uuid(),
                    name = uuid(),
                    delegations = mapOf(credentials.dataOwnerId.shouldNotBeNull() to setOf())
                )
            ).shouldNotBeNull()
            documentBridge.getDocument(document.id).let {
                it shouldNotBe null
                it?.id shouldBe document.id
                it?.name shouldBe document.name
            }
        }
    }

    "Trying to get a Document that does not exist will result in a 404" {
        withAuthenticatedReactorContext(credentials) {
            shouldThrow<RequestStatusException> { documentBridge.getDocument(uuid()) }.also {
                it.statusCode shouldBe 404
            }
        }
    }

    "Can create a Document" {
        withAuthenticatedReactorContext(credentials) {
            val createdDocument = documentBridge.createDocument(
                Document(
                    id = uuid(),
                    medicalLocationId = uuid(),
                    delegations = mapOf(credentials.dataOwnerId.shouldNotBeNull() to setOf())
                ),
                true
            ).shouldNotBeNull()
            documentBridge.getDocument(createdDocument.id) shouldNotBe null
        }
    }

    "Can create a Document with a main attachment" {
        withAuthenticatedReactorContext(credentials) {
            val document = documentBridge.createDocument(
                Document(
                    id = uuid(),
                    name = uuid(),
                    delegations = mapOf(credentials.dataOwnerId.shouldNotBeNull() to setOf())
                )
            ).shouldNotBeNull()
            val content = "A1/${uuid()}\n${uuid()}"
            val fakeAttachment = content.toByteArray(Charsets.UTF_8)
            val documentWithAttachment = documentBridge.updateAttachments(
                document,
                DataAttachmentChange.CreateOrUpdate(
                    flowOf(ByteBuffer.wrap(fakeAttachment).toDataBuffer()),
                    fakeAttachment.size.toLong(),
                    listOf("public.plain-text"),
                    false
                )
            ).shouldNotBeNull()
            documentBridge.getMainAttachment(documentWithAttachment.id).map {
                it.asByteBuffer().decodeString(Charsets.UTF_8)
            }.toList().joinToString("") shouldBe content
        }
    }

    "Retrieving the main attachment from a document that does not have it will result in an empty flow" {
        withAuthenticatedReactorContext(credentials) {
            val document = documentBridge.createDocument(
                Document(
                    id = uuid(),
                    name = uuid(),
                    delegations = mapOf(credentials.dataOwnerId.shouldNotBeNull() to setOf())
                )
            ).shouldNotBeNull()
            documentBridge.getMainAttachment(document.id).toList().shouldBeEmpty()
        }
    }

    "Trying to update the attachments of a Document without a rev will result in an error" {
        withAuthenticatedReactorContext(credentials) {
            val doc = Document(id = uuid())
            shouldThrow<IllegalStateException> {
                documentBridge.updateAttachments(doc, DataAttachmentChange.Delete)
            }
        }
    }


}
