package org.taktik.icure.asynclogic.bridge

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestInstance
import org.taktik.icure.asynclogic.bridge.mappers.HealthElementMapper
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.security.jwt.JwtUtils
import org.taktik.icure.test.*
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HealthElementLogicBridgeTest(
    val bridgeConfig: BridgeConfig,
    val heMapper: HealthElementMapper,
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

            val heBridge = HealthElementLogicBridge(
                KmehrTestApplication.fakeSessionLogic,
                bridgeConfig,
                heMapper
            )

            healthcarePartyLogicBridgeTest(hcp, heBridge)
        }
    }
}

private fun StringSpec.healthcarePartyLogicBridgeTest(
    credentials: UserCredentials,
    heBridge: HealthElementLogicBridge
) {

    "Can create a HE" {
        withAuthenticatedReactorContext(credentials) {
            val healthElement = HealthElement(
                id = uuid(),
                note = uuid()
            )
            heBridge.createEntities(listOf(healthElement))
                .firstOrNull()
                .shouldNotBeNull()
                .let {
                    it.id shouldBe healthElement.id
                    it.note shouldBe healthElement.note
                }
        }
    }

    "Can retrieve the latest HE by hcParty and patient SFK" {
        withAuthenticatedReactorContext(credentials) {
            val patientSFKs = List(3) { uuid() }

            val expectedHE = List(3) { uuid() }.associateWith { heID ->
                val sfk = patientSFKs.random()
                (0..4).forEach { _ ->
                    heBridge.createEntities(listOf(
                        HealthElement(
                            id = uuid(),
                            healthElementId = heID,
                            created = System.currentTimeMillis() - Random.nextLong(10000L, 999999L),
                            secretForeignKeys = setOf(sfk),
                            delegations = mapOf(
                                credentials.dataOwnerId!! to setOf(
                                    Delegation(
                                        uuid(),
                                        credentials.dataOwnerId,
                                        uuid(),
                                        emptyList()
                                    )
                                )
                            )
                        )
                    )).first()
                }

                heBridge.createEntities(listOf(
                    HealthElement(
                        id = uuid(),
                        healthElementId = heID,
                        created = System.currentTimeMillis(),
                        secretForeignKeys = setOf(sfk),
                        delegations = mapOf(
                            credentials.dataOwnerId!! to setOf(
                                Delegation(
                                    uuid(),
                                    credentials.dataOwnerId,
                                    uuid(),
                                    emptyList()
                                )
                            )
                        )
                    )
                )).first()
            }

            List(10) {
                heBridge.createEntities(listOf(
                    HealthElement(
                        id = uuid(),
                        healthElementId = uuid(),
                        created = System.currentTimeMillis(),
                        secretForeignKeys = setOf(uuid()),
                        delegations = mapOf(uuid() to setOf(Delegation(uuid(), uuid(), uuid(), emptyList())))
                    )
                )).first()

            }

            val result =
                heBridge.listLatestHealthElementsByHcPartyAndSecretPatientKeys(credentials.dataOwnerId!!, patientSFKs)
                    .associateBy { it.healthElementId!! }

            result.size shouldBe 3
            expectedHE.forEach {
                result[it.key] shouldNotBe null
                result[it.key]!!.healthElementId!! shouldBe it.value.healthElementId
                result[it.key]!!.secretForeignKeys shouldContain it.value.secretForeignKeys.first()
                result[it.key]!!.delegations.keys shouldContain credentials.dataOwnerId
                result[it.key]!!.created!! shouldBe it.value.created
            }
        }
    }

    "If there is no HE matching the filter, an empty list is returned" {
        withAuthenticatedReactorContext(credentials) {
            List(10) {
                heBridge.createEntities(listOf(
                    HealthElement(
                        id = uuid(),
                        healthElementId = uuid(),
                        created = System.currentTimeMillis(),
                        secretForeignKeys = setOf(uuid()),
                        delegations = mapOf(uuid() to setOf(Delegation(uuid(), uuid(), uuid(), emptyList())))
                    )
                )).first()
            }
            heBridge.listLatestHealthElementsByHcPartyAndSecretPatientKeys(uuid(), listOf(uuid())).size shouldBe 0
        }
    }

    "Can retrieve HealthElements by hcParty and patient SFK" {
        withAuthenticatedReactorContext(credentials) {
            val patientSFKs = List(3) { uuid() }

            val expectedHE = List(3) { uuid() }.flatMap { heID ->
                val sfk = patientSFKs.random()
                List(4) {
                    heBridge.createEntities(listOf(
                        HealthElement(
                            id = uuid(),
                            healthElementId = heID,
                            created = System.currentTimeMillis() - Random.nextLong(10000L, 999999L),
                            secretForeignKeys = setOf(sfk),
                            delegations = mapOf(
                                credentials.dataOwnerId!! to setOf(
                                    Delegation(
                                        uuid(),
                                        credentials.dataOwnerId,
                                        uuid(),
                                        emptyList()
                                    )
                                )
                            )
                        )
                    )).first()
                }
            }

            expectedHE.size shouldBe 12

            List(10) {
                heBridge.createEntities(listOf(
                    HealthElement(
                        id = uuid(),
                        healthElementId = uuid(),
                        created = System.currentTimeMillis(),
                        secretForeignKeys = setOf(uuid()),
                        delegations = mapOf(uuid() to setOf(Delegation(uuid(), uuid(), uuid(), emptyList())))
                    )
                )).first()
            }

            heBridge.listHealthElementsByHcPartyAndSecretPatientKeys(credentials.dataOwnerId!!, patientSFKs)
                .onEach {
                    it.delegations.keys shouldContain credentials.dataOwnerId
                    patientSFKs shouldContain it.secretForeignKeys.first()
                }.count() shouldBe expectedHE.size
        }
    }
}
