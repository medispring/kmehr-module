package org.taktik.icure.asynclogic.bridge

import com.icure.sdk.utils.RequestStatusException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestInstance
import org.taktik.icure.asynclogic.bridge.mappers.HealthcarePartyMapper
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.security.jwt.JwtUtils
import org.taktik.icure.test.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HealthcarePartyLogicBridgeTest(
    val bridgeConfig: BridgeConfig,
    val hcpMapper: HealthcarePartyMapper,
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

            val hcpBridge = HealthcarePartyLogicBridge(
                KmehrTestApplication.fakeSessionLogic,
                bridgeConfig,
                hcpMapper
            )

            healthcarePartyLogicBridgeTest(hcp, hcpBridge)
        }
    }
}

private fun StringSpec.healthcarePartyLogicBridgeTest(
    credentials: UserCredentials,
    hcpBridge: HealthcarePartyLogicBridge
) {

    "Can retrieve a hcp" {
        withAuthenticatedReactorContext(credentials) {
            val hcp = hcpBridge.getHealthcareParty(credentials.dataOwnerId!!)
            hcp shouldNotBe null
            hcp!!.id shouldBe credentials.dataOwnerId
        }
    }

    "Can create a hcp" {
        withAuthenticatedReactorContext(credentials) {
            val hcp = HealthcareParty(
                id = uuid(),
                firstName = uuid(),
                lastName = uuid(),
            )

            val createdHcp = hcpBridge.createHealthcareParty(hcp)

            createdHcp shouldNotBe null
            createdHcp!!.id shouldBe hcp.id
            createdHcp.firstName shouldBe hcp.firstName
            createdHcp.lastName shouldBe hcp.lastName
        }
    }

    "Retrieving a non existent hcp will return a 404 Client exception" {
        withAuthenticatedReactorContext(credentials) {
            shouldThrow<RequestStatusException> { hcpBridge.getHealthcareParty(uuid()) }.also {
                it.statusCode shouldBe 404
            }
        }
    }

    "The HCP hierarchy of a single HCP is the HCP itself" {
        withAuthenticatedReactorContext(credentials) {
            val hcp = hcpBridge.getHealthcareParty(credentials.dataOwnerId!!)
            hcp shouldNotBe null
            val hierarchy = hcpBridge.getHcpHierarchyIds(hcp!!)
            hierarchy.size shouldBe 1
            hierarchy.first() shouldBe hcp.id
        }
    }

    "Can retrieve the hierarchy of a HCP" {
        withAuthenticatedReactorContext(credentials) {
            val hcp1 = hcpBridge.createHealthcareParty(
                HealthcareParty(
                    id = uuid(),
                    firstName = uuid(),
                    lastName = uuid(),
                    parentId = credentials.dataOwnerId!!
                )
            )
            val hcp2 = hcpBridge.createHealthcareParty(
                HealthcareParty(
                    id = uuid(),
                    firstName = uuid(),
                    lastName = uuid(),
                    parentId = hcp1!!.id
                )
            ).let { hcpBridge.getHealthcareParty(it!!.id)!! }

            val hierarchy = hcpBridge.getHcpHierarchyIds(hcp2)
            hierarchy.size shouldBe 3
            hierarchy shouldContain credentials.dataOwnerId
            hierarchy shouldContain hcp1.id
            hierarchy shouldContain hcp2.id
        }
    }

    "A non-existent HCP is automatically excluded from the hierarchy" {
        withAuthenticatedReactorContext(credentials) {
            val hcp = hcpBridge.createHealthcareParty(
                HealthcareParty(
                    id = uuid(),
                    firstName = uuid(),
                    lastName = uuid(),
                    parentId = uuid()
                )
            ).let { hcpBridge.getHealthcareParty(it!!.id)!! }

            val hierarchy = hcpBridge.getHcpHierarchyIds(hcp)
            hierarchy.size shouldBe 1
            hierarchy.first() shouldBe hcp.id
        }
    }

    "Can retrieve HCPs by SSIN" {
        withAuthenticatedReactorContext(credentials) {
            val hcpList = List(3) {
                hcpBridge.createHealthcareParty(
                    HealthcareParty(
                        id = uuid(),
                        firstName = uuid(),
                        lastName = uuid(),
                        parentId = credentials.dataOwnerId!!,
                        ssin = uuid(),
                        nihii = uuid()
                    )
                ).shouldNotBeNull()
            }

            val result = hcpBridge.listHealthcarePartiesBySsin(hcpList.first().ssin!!).toList()
            result.size shouldBe 1
            result.first().id shouldBe hcpList.first().id
            result.first().ssin shouldBe hcpList.first().ssin
        }
    }

    "Can retrieve HCPs by SSIN when exceeding pagination" {
        withAuthenticatedReactorContext(credentials) {
            val ssin = ssin()

            val hcpList = List(50) {
                hcpBridge.createHealthcareParty(
                    HealthcareParty(
                        id = uuid(),
                        firstName = uuid(),
                        lastName = uuid(),
                        parentId = credentials.dataOwnerId!!,
                        ssin = ssin(),
                        nihii = ssin
                    )
                ).shouldNotBeNull()
            } + hcpBridge.createHealthcareParty(
                HealthcareParty(
                    id = uuid(),
                    firstName = uuid(),
                    lastName = uuid(),
                    parentId = credentials.dataOwnerId!!,
                    ssin = ssin,
                    nihii = ssin()
                )
            ).shouldNotBeNull()

            List(100) {
                hcpBridge.createHealthcareParty(
                    HealthcareParty(
                        id = uuid(),
                        firstName = uuid(),
                        lastName = uuid(),
                        parentId = credentials.dataOwnerId,
                        ssin = ssin(),
                        nihii = ssin()
                    )
                )
            }

            val result = hcpBridge.listHealthcarePartiesBySsin(ssin).first()
            result shouldBe hcpList.last()
        }
    }

    "If no HCP with that SSIN exists, an empty result is returned" {
        withAuthenticatedReactorContext(credentials) {
            hcpBridge.listHealthcarePartiesBySsin(uuid()).count() shouldBe 0
        }
    }

    "Can retrieve HCPs by Nihii" {
        withAuthenticatedReactorContext(credentials) {
            val hcpList = List(3) {
                hcpBridge.createHealthcareParty(
                    HealthcareParty(
                        id = uuid(),
                        firstName = uuid(),
                        lastName = uuid(),
                        parentId = credentials.dataOwnerId!!,
                        ssin = uuid(),
                        nihii = uuid()
                    )
                )
            }

            val result = hcpBridge.listHealthcarePartiesByNihii(hcpList.first()!!.nihii!!).toList()
            result.size shouldBe 1
            result.first().id shouldBe hcpList.first()!!.id
            result.first().nihii shouldBe hcpList.first()!!.nihii
        }
    }

    "Can retrieve HCPs by Nihii when exceeding pagination" {
        withAuthenticatedReactorContext(credentials) {
            val nihii = ssin()

            val hcpList = List(50) {
                hcpBridge.createHealthcareParty(
                    HealthcareParty(
                        id = uuid(),
                        firstName = uuid(),
                        lastName = uuid(),
                        parentId = credentials.dataOwnerId!!,
                        ssin = nihii,
                        nihii = ssin()
                    )
                ).shouldNotBeNull()
            } + hcpBridge.createHealthcareParty(
                HealthcareParty(
                    id = uuid(),
                    firstName = uuid(),
                    lastName = uuid(),
                    parentId = credentials.dataOwnerId!!,
                    ssin = ssin(),
                    nihii = nihii
                )
            ).shouldNotBeNull()

            List(100) {
                hcpBridge.createHealthcareParty(
                    HealthcareParty(
                        id = uuid(),
                        firstName = uuid(),
                        lastName = uuid(),
                        parentId = credentials.dataOwnerId,
                        ssin = ssin(),
                        nihii = ssin()
                    )
                )
            }

            val result = hcpBridge.listHealthcarePartiesByNihii(nihii).first()
            result shouldBe hcpList.last()
        }
    }

    "If no HCP with that Nihii exists, an empty result is returned" {
        withAuthenticatedReactorContext(credentials) {
            hcpBridge.listHealthcarePartiesByNihii(uuid()).count() shouldBe 0
        }
    }

    "Can retrieve multiple HCPs by id" {
        withAuthenticatedReactorContext(credentials) {
            val hcpList = List(3) {
                hcpBridge.createHealthcareParty(
                    HealthcareParty(
                        id = uuid(),
                        firstName = uuid(),
                        lastName = uuid(),
                        parentId = credentials.dataOwnerId!!,
                        ssin = uuid(),
                        nihii = uuid()
                    )
                )
            }

            hcpBridge.getHealthcareParties(hcpList.map { it!!.id }).count() shouldBe 3
        }
    }

    "Can retrieve multiple HCPs by id, if some ids do not exist they are ignored" {
        withAuthenticatedReactorContext(credentials) {
            val hcpList = List(3) {
                hcpBridge.createHealthcareParty(
                    HealthcareParty(
                        id = uuid(),
                        firstName = uuid(),
                        lastName = uuid(),
                        parentId = credentials.dataOwnerId!!,
                        ssin = uuid(),
                        nihii = uuid()
                    )
                )
            }

            hcpBridge.getHealthcareParties(hcpList.map { it!!.id } + uuid()).count() shouldBe 3
        }
    }

    "Can retrieve multiple HCPs by id, if none exist an empty flow is returned" {
        withAuthenticatedReactorContext(credentials) {
            hcpBridge.getHealthcareParties(List(3) { uuid() }).count() shouldBe 0
        }
    }

    "Can retrieve multiple HCPs by id, if an empty list is passed then an empty flow is returned" {
        withAuthenticatedReactorContext(credentials) {
            hcpBridge.getHealthcareParties(emptyList()).count() shouldBe 0
        }
    }

    "Can retrieve HCPs by name" {
        withAuthenticatedReactorContext(credentials) {
            val firstName = uuid().substring(0, 6).replace(Regex("[0-6]"), "")
            val lastName = uuid().substring(0, 6).replace(Regex("[0-6]"), "")
            val createdHcp = hcpBridge.createHealthcareParty(
                HealthcareParty(
                    id = uuid(),
                    firstName = firstName,
                    lastName = lastName,
                    parentId = credentials.dataOwnerId!!,
                    ssin = uuid(),
                    nihii = uuid()
                )
            )

            hcpBridge.listHealthcarePartiesByName("$firstName$lastName").onEach {
                it.id shouldBe createdHcp!!.id
            }
        }
    }

}
