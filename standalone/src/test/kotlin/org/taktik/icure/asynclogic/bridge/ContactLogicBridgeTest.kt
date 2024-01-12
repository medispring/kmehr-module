package org.taktik.icure.asynclogic.bridge

import io.icure.kraken.client.apis.*
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.security.jwt.JwtUtils
import org.taktik.icure.services.external.rest.v2.mapper.ContactV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.ServiceV2Mapper
import org.taktik.icure.test.*
import kotlin.random.Random

@SpringBootTest(
    classes = [KmehrTestApplication::class],
    properties = [
        "spring.main.allow-bean-definition-overriding=true"
    ],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles(profiles = ["kmehr"])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContactLogicBridgeTest(
    @Autowired val bridgeConfig: BridgeConfig,
    @Autowired val serviceMapper: ServiceV2Mapper,
    @Autowired val contactMapper: ContactV2Mapper,
    @Autowired val jwtUtils: JwtUtils
) : StringSpec() {

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
                serviceMapper
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
                        hcp.dataOwnerId!! to setOf()
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

            contactBridge.listServiceIdsByTag(
                hcp.dataOwnerId,
                listOf(sfk),
                tagType,
                tagCode,
                null,
                null
            ).onEach {
                it shouldBe contact1!!.services.first().id
            }.count() shouldBe 1
        }
    }

    "Can retrieve the ids of more Services than the page size" {
        withAuthenticatedReactorContext(hcp) {
            val tagType = uuid().substring(0, 6)
            val tagCode = uuid().substring(0, 6)
            val version = Random.nextInt(1, 9).toString()
            val sfk = uuid()

            val correctContacts = List(1500) {
                contactBridge.createContact(
                    Contact(
                        id = uuid(),
                        secretForeignKeys = setOf(sfk),
                        services = setOf(
                            Service(
                                id = uuid(),
                                tags = setOf(
                                    CodeStub(
                                        id = "$tagType|$tagCode|$version",
                                        type = tagType,
                                        code = tagCode,
                                        version = version
                                    )
                                )
                            )
                        ),
                        delegations = mapOf(
                            hcp.dataOwnerId!! to setOf()
                        )
                    )
                )
            }

            val correctIds = correctContacts.map { it!!.services.first().id }
            correctIds.size shouldBe 1500

            List(1500) {
                val wrongType = uuid().substring(0, 6)
                val wrongCode = uuid().substring(0, 6)
                contactBridge.createContact(
                    Contact(
                        id = uuid(),
                        secretForeignKeys = setOf(uuid()),
                        services = setOf(
                            Service(
                                id = uuid(),
                                tags = setOf(
                                    CodeStub(
                                        id = "$wrongType|$wrongCode|${it % 10}",
                                        type = wrongType,
                                        code = wrongCode,
                                        version = "${it % 10}"
                                    )
                                )
                            )
                        ),
                        delegations = mapOf(
                            uuid() to setOf()
                        )
                    )
                )
            }

            contactBridge.listServiceIdsByTag(
                hcp.dataOwnerId!!,
                listOf(sfk),
                tagType,
                tagCode,
                null,
                null
            ).onEach {
                correctIds shouldContain it
            }.count() shouldBe correctIds.size
        }
    }

    "If no Service is matched by HCP, SFKs, and tag, an empty flow is returned" {
        withAuthenticatedReactorContext(hcp) {
            contactBridge.listServiceIdsByTag(uuid(), listOf(uuid()), uuid(), uuid(), null, null)
                .count() shouldBe 0
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
            }!!

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
