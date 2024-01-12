package org.taktik.icure.asynclogic.bridge

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.security.jwt.JwtUtils
import org.taktik.icure.services.external.rest.v2.mapper.HealthcarePartyV2MapperImpl
import org.taktik.icure.test.*

@SpringBootTest(
    classes = [KmehrTestApplication::class],
    properties = [
        "spring.main.allow-bean-definition-overriding=true"
    ],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles(profiles = ["kmehr"])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HealthcarePartyLogicBridgeTest(
    @Autowired val bridgeConfig: BridgeConfig,
    @Autowired val hcpMapper: HealthcarePartyV2MapperImpl,
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

    "Retrieving a non existent hcp will return null" {
        withAuthenticatedReactorContext(credentials) {
            hcpBridge.getHealthcareParty(uuid()) shouldBe null
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
                )
            }

            val result = hcpBridge.listHealthcarePartiesBySsin(hcpList.first()!!.ssin!!).toList()
            result.size shouldBe 1
            result.first().id shouldBe hcpList.first()!!.id
            result.first().ssin shouldBe hcpList.first()!!.ssin
        }
    }

    "Can retrieve HCPs by SSIN when exceeding pagination" {
        withAuthenticatedReactorContext(credentials) {
            val ssin = uuid()

            val hcpList = List(1500) {
                hcpBridge.createHealthcareParty(
                    HealthcareParty(
                        id = uuid(),
                        firstName = uuid(),
                        lastName = uuid(),
                        parentId = credentials.dataOwnerId!!,
                        ssin = ssin,
                        nihii = uuid()
                    )
                )
            }

            hcpList.size shouldBeGreaterThan 0

            List(1000) {
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

            hcpBridge.listHealthcarePartiesBySsin(ssin)
                .onEach {
                    it.ssin shouldBe ssin
                }.count() shouldBe hcpList.size
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
            val nihii = uuid()

            val hcpList = List(1500) {
                hcpBridge.createHealthcareParty(
                    HealthcareParty(
                        id = uuid(),
                        firstName = uuid(),
                        lastName = uuid(),
                        parentId = credentials.dataOwnerId!!,
                        ssin = uuid(),
                        nihii = nihii
                    )
                )
            }

            hcpList.size shouldBeGreaterThan 0

            List(1000) {
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

            hcpBridge.listHealthcarePartiesByNihii(nihii)
                .onEach {
                    it.nihii shouldBe nihii
                }.count() shouldBe hcpList.size
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

    "Can retrieve multiple HCPs by id, if none exist a empty flow is returned" {
        withAuthenticatedReactorContext(credentials) {
            hcpBridge.getHealthcareParties(List(3) { uuid() }).count() shouldBe 0
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
