package org.taktik.icure.be.ehealth.logic.kmehr.medicationscheme.impl.v20161201

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.TestInstance
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.Utils
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.schema.v1.FolderType
import org.taktik.icure.be.ehealth.logic.kmehr.Config
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.embed.Content
import org.taktik.icure.entities.embed.Medication
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.entities.embed.Substanceproduct
import org.taktik.icure.test.BaseKmehrTest
import org.taktik.icure.test.uuid
import java.time.Instant
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.isAccessible

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MedicationSchemeExportTest(
	private val medicationSchemeExport: MedicationSchemeExport
) : BaseKmehrTest() {

	private val defaultConfig = Config(
		_kmehrId = System.currentTimeMillis().toString(),
		date = Utils.makeXGC(Instant.now().toEpochMilli())!!,
		time = Utils.makeXGC(Instant.now().toEpochMilli(), true)!!,
		soft = Config.Software(name = "iCure", version = "1.2.3"),
		clinicalSummaryType = "",
		defaultLanguage = "en",
	)

	init {
		medicationSchemeExportTest()
	}

	private fun StringSpec.medicationSchemeExportTest() {

		"CSM-344 - makePatientFolder will not increase the version if a version is passed" {
			val version = Random.nextInt(10, 100)
			val folder = medicationSchemeExport.callMakePatientFolder(
				42,
				Patient(id = uuid()),
				version,
				HealthcareParty(id = uuid()),
				defaultConfig,
				language = "en",
				emptyList(),
				null,
				null
			)
			folder.transactions.size shouldBe 1
			folder.transactions.first().version shouldBe "$version"
		}

		"CSM-344 - makePatientFolder will have a version = 1 if no version is passed and no version can be deduced from the services" {
			val folder = medicationSchemeExport.callMakePatientFolder(
				42,
				Patient(id = uuid()),
				null,
				HealthcareParty(id = uuid()),
				defaultConfig,
				language = "en",
				emptyList(),
				null,
				null
			)
			folder.transactions.size shouldBe 1
			folder.transactions.first().version shouldBe "1"
		}

		"CSM-344 - if the version can be inferred from the services, then the folder version will be version + 1" {
			val version = Random.nextInt(10, 100)
			val folder = medicationSchemeExport.callMakePatientFolder(
				42,
				Patient(id = uuid()),
				null,
				HealthcareParty(id = uuid()),
				defaultConfig,
				language = "en",
				listOf(
					Service(
						id = uuid(),
						content = mapOf(
							"en" to Content(
								medicationValue = Medication(
									idOnSafes = uuid(),
									medicationSchemeSafeVersion = version,
									substanceProduct = Substanceproduct(
										intendedname = uuid()
									)
								)
							)
						)
					)
				),
				null,
				null
			)
			folder.transactions.first().version shouldBe "${version + 1}"
		}

	}

	private suspend fun MedicationSchemeExport.callMakePatientFolder(
		patientIndex: Int,
		patient: Patient,
		version: Int?,
		healthcareParty: HealthcareParty,
		config: Config,
		language: String,
		medicationServices: List<Service>,
		recipientSafe: String?,
		serviceAuthors: List<HealthcareParty>?,
	): FolderType = suspendCoroutine { continuation ->
		val folderType = this::class.memberFunctions.first { it.name == "makePatientFolder" }.apply {
			this.isAccessible = true
		}.call(
			this,
			patientIndex,
			patient,
			version,
			healthcareParty,
			config,
			language,
			medicationServices,
			recipientSafe,
			serviceAuthors,
			continuation
		) as FolderType
		continuation.resume(folderType)
	}




}