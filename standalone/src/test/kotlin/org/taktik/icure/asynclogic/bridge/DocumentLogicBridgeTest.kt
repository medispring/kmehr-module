package org.taktik.icure.asynclogic.bridge

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestInstance
import org.taktik.icure.asynclogic.objectstorage.DataAttachmentChange
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.entities.Document
import org.taktik.icure.security.jwt.JwtUtils
import org.taktik.icure.services.external.rest.v2.mapper.DocumentV2MapperImpl
import org.taktik.icure.test.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DocumentLogicBridgeTest(
    val bridgeConfig: BridgeConfig,
    val documentMapper: DocumentV2MapperImpl,
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
                    delegations = mapOf(credentials.dataOwnerId!! to setOf())
                )
            )
            documentBridge.getDocument(document!!.id).let {
                it shouldNotBe null
                it?.id shouldBe document.id
                it?.name shouldBe document.name
            }
        }
    }

    "Trying to get a Document that does not exists will result in a null result" {
        withAuthenticatedReactorContext(credentials) {
            documentBridge.getDocument(uuid()) shouldBe null
        }
    }

    "Can create a Document" {
        withAuthenticatedReactorContext(credentials) {
            val createdDocument = documentBridge.createDocument(
                Document(
                    id = uuid(),
                    medicalLocationId = uuid(),
                    delegations = mapOf(credentials.dataOwnerId!! to setOf())
                ),
                true
            )
            createdDocument shouldNotBe null
            documentBridge.getDocument(createdDocument!!.id) shouldNotBe null
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
