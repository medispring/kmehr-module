package org.taktik.icure.asynclogic.bridge

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestInstance
import org.taktik.icure.asynclogic.bridge.mappers.ContactFilterMapper
import org.taktik.icure.asynclogic.bridge.mappers.ContactMapper
import org.taktik.icure.asynclogic.bridge.mappers.ServiceFilterMapper
import org.taktik.icure.asynclogic.bridge.mappers.ServiceMapper
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.domain.filter.impl.service.ServiceByHcPartyTagCodeDateFilter
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.security.jwt.JwtUtils
import org.taktik.icure.test.BaseKmehrTest
import org.taktik.icure.test.KmehrTestApplication
import org.taktik.icure.test.UserCredentials
import org.taktik.icure.test.createHealthcarePartyUser
import org.taktik.icure.test.uuid
import org.taktik.icure.test.withAuthenticatedReactorContext
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContactLogicBridgeTest(
    val bridgeConfig: BridgeConfig,
    val serviceMapper: ServiceMapper,
    val contactMapper: ContactMapper,
    val serviceFilterMapper: ServiceFilterMapper,
    val contactFilterMapper: ContactFilterMapper,
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

            val contactBridge = ContactLogicBridge(
                KmehrTestApplication.fakeSessionLogic,
                bridgeConfig,
                contactMapper,
                serviceMapper,
                contactFilterMapper,
                serviceFilterMapper
            )

            contactLogicBridgeTest(hcp, contactBridge)
        }
    }
}

private fun StringSpec.contactLogicBridgeTest(
    hcp: UserCredentials,
    contactBridge: ContactLogicBridge
) {

    "Can create a Contact" {
        withAuthenticatedReactorContext(hcp) {
            val contactToCreate = Contact(
                id = uuid(),
                medicalLocationId = uuid(),
                delegations = mapOf(hcp.dataOwnerId!! to emptySet())
            )
            contactBridge.createContact(contactToCreate).let {
                it shouldNotBe null
                it?.id shouldBe contactToCreate.id
                it?.medicalLocationId shouldBe contactToCreate.medicalLocationId
            }
        }
    }

    "Can retrieve the id of one Service" {
        withAuthenticatedReactorContext(hcp) {
            val tagType = uuid().substring(0, 6)
            val tagCode = uuid().substring(0, 6)
            val sfk = uuid()

            val contact1 = contactBridge.createContact(
                Contact(
                    id = uuid(),
                    secretForeignKeys = setOf(sfk),
                    services = setOf(
                        Service(
                            id = uuid(),
                            tags = setOf(
                                CodeStub(
                                    id = "$tagType|$tagCode|1",
                                    type = tagType,
                                    code = tagCode,
                                    version = "1"
                                )
                            )
                        )
                    ),
                    delegations = mapOf(
                        hcp.dataOwnerId.shouldNotBeNull() to setOf()
                    )
                )
            )

            contactBridge.createContact(
                Contact(
                    id = uuid(),
                    secretForeignKeys = setOf(uuid()),
                    services = setOf(
                        Service(
                            id = uuid(),
                            tags = setOf(
                                CodeStub(
                                    id = "AAAA|BBBB|1",
                                    type = "AAAA",
                                    code = "BBBB",
                                    version = "1"
                                )
                            )
                        )
                    ),
                    delegations = mapOf(
                        hcp.dataOwnerId to setOf()
                    )
                )
            )

            contactBridge.matchEntitiesBy(
                ServiceByHcPartyTagCodeDateFilter(
                    healthcarePartyId = hcp.dataOwnerId,
                    patientSecretForeignKeys = listOf(sfk),
                    tagType = tagType,
                    tagCode = tagCode,
                )
            ).onEach {
                it shouldBe contact1!!.services.first().id
            }.count() shouldBe 1
        }
    }

    "If no Service is matched by HCP, SFKs, and tag, an empty flow is returned" {
        withAuthenticatedReactorContext(hcp) {
            contactBridge.matchEntitiesBy(
                ServiceByHcPartyTagCodeDateFilter(
                    healthcarePartyId = uuid(),
                    patientSecretForeignKeys = listOf(uuid()),
                    tagType = uuid(),
                    tagCode = uuid()
                )
            ).count() shouldBe 0
        }
    }

    "Can retrieve Contacts by HCP id and Secret Foreign Keys" {
        withAuthenticatedReactorContext(hcp) {
            val sfk = uuid()

            val correctContacts = List(10) {
                contactBridge.createContact(
                    Contact(
                        id = uuid(),
                        secretForeignKeys = setOf(sfk),
                        services = setOf(Service(id = uuid())),
                        delegations = mapOf(hcp.dataOwnerId!! to setOf())
                    )
                )
            }

            val correctIds = correctContacts.map { it!!.id }
            correctIds.size shouldBe 10

            List(10) {
                contactBridge.createContact(
                    Contact(
                        id = uuid(),
                        secretForeignKeys = setOf(uuid()),
                        services = setOf(Service(id = uuid())),
                        delegations = mapOf(uuid() to setOf())
                    )
                )
            }

            contactBridge.listContactsByHCPartyAndPatient(
                hcp.dataOwnerId!!,
                listOf(sfk)
            ).onEach {
                correctIds shouldContain it.id
            }.count() shouldBe correctIds.size
        }
    }

    "If no Contact matches HCP and SFKs, an empty flow is returned" {
        withAuthenticatedReactorContext(hcp) {
            contactBridge.listContactsByHCPartyAndPatient(uuid(), listOf(uuid())).count() shouldBe 0
        }
    }

    "Can modify a Contact" {
        withAuthenticatedReactorContext(hcp) {
            val contactToCreate = Contact(
                id = uuid(),
                medicalLocationId = uuid(),
                delegations = mapOf(hcp.dataOwnerId!! to emptySet())
            )
            val createdContact = contactBridge.createContact(contactToCreate).also {
                it shouldNotBe null
                it?.id shouldBe contactToCreate.id
                it?.medicalLocationId shouldBe contactToCreate.medicalLocationId
            }.shouldNotBeNull()

            val newLocation = uuid()
            contactBridge.modifyEntities(
                listOf(createdContact.copy(medicalLocationId = newLocation))
            ).firstOrNull().shouldNotBeNull().let {
                it.id shouldBe createdContact.id
                it.medicalLocationId shouldBe newLocation
            }
        }
    }
}
