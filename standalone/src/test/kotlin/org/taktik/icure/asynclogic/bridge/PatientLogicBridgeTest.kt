package org.taktik.icure.asynclogic.bridge

import com.icure.sdk.utils.RequestStatusException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestInstance
import org.taktik.icure.asynclogic.bridge.mappers.PatientFilterMapper
import org.taktik.icure.asynclogic.bridge.mappers.PatientMapper
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.domain.filter.impl.patient.PatientByHcPartyAndSsinFilter
import org.taktik.icure.domain.filter.impl.patient.PatientByHcPartyDateOfBirthFilter
import org.taktik.icure.domain.filter.impl.patient.PatientByHcPartyNameFilter
import org.taktik.icure.entities.Patient
import org.taktik.icure.security.jwt.JwtUtils
import org.taktik.icure.test.*
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PatientLogicBridgeTest(
    val bridgeConfig: BridgeConfig,
    val patientMapper: PatientMapper,
    val patientFilterMapper: PatientFilterMapper,
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

            val patientBridge = PatientLogicBridge(
                KmehrTestApplication.fakeSessionLogic,
                bridgeConfig,
                patientMapper,
                patientFilterMapper
            )

            patientLogicBridgeTest(hcp, patientBridge)
        }
    }

}

private suspend fun StringSpec.patientLogicBridgeTest(
    credentials: UserCredentials,
    patientBridge: PatientLogicBridge
) {

    "Can get a Patient" {
        withAuthenticatedReactorContext(credentials) {
            val newPatient = patientBridge.createPatient(
                Patient(
                    id = uuid(),
                    firstName = "patient",
                    lastName = uuid(),
                    delegations = mapOf(credentials.dataOwnerId!! to setOf())
                )
            )

            val retrievedPatient = patientBridge.getPatient(newPatient!!.id)
            retrievedPatient shouldNotBe null
            retrievedPatient!!.id shouldBe newPatient.id
        }
    }

    "Can create a Patient" {
        withAuthenticatedReactorContext(credentials) {
            val patientToCreate = Patient(
                id = uuid(),
                firstName = "patient",
                lastName = uuid(),
                delegations = mapOf(credentials.dataOwnerId!! to setOf())
            )
            patientBridge.createPatient(patientToCreate).let {
                it shouldNotBe null
                it?.id shouldBe patientToCreate.id
                it?.firstName shouldBe patientToCreate.firstName
                it?.lastName shouldBe patientToCreate.lastName
            }
        }
    }

    "Trying to retrieve a non-existing Patient will result in a 404 Client exception" {
        withAuthenticatedReactorContext(credentials) {
            shouldThrow<RequestStatusException> { patientBridge.getPatient(uuid()) }.also {
                it.statusCode shouldBe 404
            }
        }
    }

    "Can retrieve Patients by HCP id and SSIN" {
        withAuthenticatedReactorContext(credentials) {
            val patients = List(5) {
                Patient(
                    id = uuid(),
                    firstName = "patient",
                    lastName = uuid(),
                    ssin = uuid(),
                    delegations = mapOf(credentials.dataOwnerId!! to setOf())
                )
            }
            patientBridge.createPatients(patients).count() shouldBe patients.size

            val result = patientBridge.matchEntitiesBy(
                PatientByHcPartyAndSsinFilter(
                    ssin = patients.first().ssin!!,
                    healthcarePartyId = patients.first().delegations.keys.first()
                )
            ).toList()
            result.size shouldBe 1
            result.first() shouldBe patients.first().id
        }
    }

    "Can retrieve Patients by HCP id and SSIN even when it exceeds internal pagination limit" {
        withAuthenticatedReactorContext(credentials) {
            val ssin = uuid()
            val hcpId = credentials.dataOwnerId!!
            val correctPatients = List(1500) {
                Patient(
                    id = uuid(),
                    firstName = "patient",
                    lastName = uuid(),
                    ssin = ssin,
                    delegations = mapOf(hcpId to setOf())
                )
            }
            patientBridge.createPatients(correctPatients).count() shouldBe correctPatients.size
            val correctPatientsId = correctPatients.map { it.id }
            patientBridge.createPatients(
                List(1000) {
                    Patient(
                        id = uuid(),
                        firstName = "patient",
                        lastName = uuid(),
                        ssin = uuid(),
                        delegations = mapOf(hcpId to setOf())
                    )
                }
            ).count() shouldBe 1000

            patientBridge.matchEntitiesBy(
                PatientByHcPartyAndSsinFilter(
                    ssin = ssin,
                    healthcarePartyId = hcpId
                )
            ).onEach {
                correctPatientsId shouldContain it
            }.count() shouldBe correctPatients.size
        }
    }

    "When no Patient matches the HCP and SSIN filter, an empty result is returned" {
        withAuthenticatedReactorContext(credentials) {
            patientBridge.matchEntitiesBy(
                PatientByHcPartyAndSsinFilter(
                    ssin = uuid(),
                    healthcarePartyId = credentials.dataOwnerId.shouldNotBeNull()
                )
            ).count() shouldBe 0
        }
    }

    "Can retrieve Patients by HCP id and date of birth" {
        withAuthenticatedReactorContext(credentials) {
            val patients = List(5) {
                Patient(
                    id = uuid(),
                    firstName = "patient",
                    lastName = uuid(),
                    ssin = uuid(),
                    dateOfBirth = Random.nextInt(19000000, 20220000),
                    delegations = mapOf(credentials.dataOwnerId!! to setOf())
                )
            }
            patientBridge.createPatients(patients).count() shouldBe 5

            val result = patientBridge.matchEntitiesBy(
                PatientByHcPartyDateOfBirthFilter(
                    dateOfBirth = patients.first().dateOfBirth.shouldNotBeNull(),
                    healthcarePartyId = patients.first().delegations.keys.first()
                )
            ).toList()
            result.size shouldBe 1
            result.first() shouldBe patients.first().id
        }
    }

    "Can retrieve Patients by HCP id and date of birth even when it exceeds internal pagination limit" {
        withAuthenticatedReactorContext(credentials) {
            val dateOfBirth = Random.nextInt(19000000, 20220000)
            val hcpId = credentials.dataOwnerId!!
            val correctPatients = List(1500) {
                Patient(
                    id = uuid(),
                    firstName = "patient",
                    lastName = uuid(),
                    ssin = uuid(),
                    dateOfBirth = dateOfBirth,
                    delegations = mapOf(hcpId to setOf())
                )
            }
            patientBridge.createPatients(correctPatients).count() shouldBe correctPatients.size
            val correctPatientsId = correctPatients.map { it.id }
            patientBridge.createPatients(
                List(1000) {
                    Patient(
                        id = uuid(),
                        firstName = "patient",
                        lastName = uuid(),
                        ssin = uuid(),
                        dateOfBirth = Random.nextInt(19000000, 20220000),
                        delegations = mapOf(hcpId to setOf())
                    )
                }
            ).count() shouldBe 1000

            patientBridge.matchEntitiesBy(
                PatientByHcPartyDateOfBirthFilter(
                    dateOfBirth = dateOfBirth,
                    healthcarePartyId = hcpId
                )
            ).onEach {
                correctPatientsId shouldContain it
            }.count() shouldBe correctPatients.size
        }
    }

    "When no Patient matches the HCP and date of birth filter, an empty result is returned" {
        withAuthenticatedReactorContext(credentials) {
            patientBridge.matchEntitiesBy(
                PatientByHcPartyDateOfBirthFilter(
                    dateOfBirth = Random.nextInt(19000000, 20220000),
                    healthcarePartyId = credentials.dataOwnerId.shouldNotBeNull()
                )
            ).count() shouldBe 0
        }
    }

    "Can retrieve Patients by HCP id and fuzzy name" {
        withAuthenticatedReactorContext(credentials) {
            val name = uuid().replace("-", "")
            val patients = List(5) {
                Patient(
                    id = uuid(),
                    firstName = "patient",
                    lastName = uuid(),
                    ssin = uuid(),
                    dateOfBirth = Random.nextInt(19000000, 20220000),
                    delegations = mapOf(credentials.dataOwnerId!! to setOf())
                )
            }
            val correctPatients = listOf(
                Patient(
                    id = uuid(),
                    firstName = name,
                    lastName = "patient",
                    ssin = uuid(),
                    dateOfBirth = Random.nextInt(19000000, 20220000),
                    delegations = mapOf(credentials.dataOwnerId!! to setOf())
                ),
                Patient(
                    id = uuid(),
                    firstName = "patient",
                    lastName = "patient",
                    spouseName = name,
                    ssin = uuid(),
                    dateOfBirth = Random.nextInt(19000000, 20220000),
                    delegations = mapOf(credentials.dataOwnerId to setOf())
                ), Patient(
                    id = uuid(),
                    firstName = "patient",
                    lastName = "patient",
                    maidenName = name,
                    ssin = uuid(),
                    dateOfBirth = Random.nextInt(19000000, 20220000),
                    delegations = mapOf(credentials.dataOwnerId to setOf())
                )
            )
            patientBridge.createPatients(patients + correctPatients)
                .count() shouldBe (patients.size + correctPatients.size)

            patientBridge.matchEntitiesBy(
                PatientByHcPartyNameFilter(
                    name = name,
                    healthcarePartyId = credentials.dataOwnerId
                )
            ).onEach { patientId ->
                correctPatients.map { it.id } shouldContain patientId
            }.count() shouldBe correctPatients.size
        }
    }

    "When no Patient matches the HCP and fuzzy name filter, an empty result is returned" {
        withAuthenticatedReactorContext(credentials) {
            patientBridge.matchEntitiesBy(
                PatientByHcPartyNameFilter(
                    name = uuid().replace("-", ""),
                    healthcarePartyId = credentials.dataOwnerId
                )
            ).count() shouldBe 0
        }
    }

    "Can retrieve multiple patients by ID" {
        withAuthenticatedReactorContext(credentials) {
            val patients = List(5) {
                Patient(
                    id = uuid(),
                    firstName = "patient",
                    lastName = uuid(),
                    ssin = uuid(),
                    delegations = mapOf(credentials.dataOwnerId!! to setOf())
                )
            }
            patientBridge.createPatients(patients).count() shouldBe patients.size

            patientBridge.getPatients(patients.map { it.id }).count() shouldBe 5
        }
    }

    "Can retrieve multiple patients by ID, skipping the ids that do not exist" {
        withAuthenticatedReactorContext(credentials) {
            val patients = List(5) {
                Patient(
                    id = uuid(),
                    firstName = "patient",
                    lastName = uuid(),
                    ssin = uuid(),
                    delegations = mapOf(credentials.dataOwnerId!! to setOf())
                )
            }
            patientBridge.createPatients(patients).count() shouldBe patients.size

            patientBridge.getPatients(patients.map { it.id } + uuid()).count() shouldBe 5
        }
    }

    "Retrieving multiple patients with non-existent ids will result in an empty flow" {
        withAuthenticatedReactorContext(credentials) {
            patientBridge.getPatients(List(5) { uuid() }).count() shouldBe 0
        }
    }

}
