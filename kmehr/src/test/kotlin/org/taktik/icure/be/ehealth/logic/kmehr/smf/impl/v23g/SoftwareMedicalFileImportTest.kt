package org.taktik.icure.be.ehealth.logic.kmehr.smf.impl.v23g

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.taktik.couchdb.id.UUIDGenerator
import org.taktik.icure.asynclogic.ContactLogic
import org.taktik.icure.asynclogic.DocumentLogic
import org.taktik.icure.asynclogic.FormLogic
import org.taktik.icure.asynclogic.FormTemplateLogic
import org.taktik.icure.asynclogic.HealthElementLogic
import org.taktik.icure.asynclogic.HealthcarePartyLogic
import org.taktik.icure.asynclogic.InsuranceLogic
import org.taktik.icure.asynclogic.PatientLogic
import org.taktik.icure.asynclogic.UserLogic
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.User

class SoftwareMedicalFileImportTest : StringSpec({

	"shallowNormalizeName lowercases and drops spaces and punctuation" {
		SoftwareMedicalFileImport.shallowNormalizeName("Other ") shouldBe "other"
		SoftwareMedicalFileImport.shallowNormalizeName("O.T.H.E.R") shouldBe "other"
		SoftwareMedicalFileImport.shallowNormalizeName("De Vries-Janssen") shouldBe "devriesjanssen"
		SoftwareMedicalFileImport.shallowNormalizeName("HEALTH one 1") shouldBe "healthone1"
	}

	"shallowNormalizeName is accent-sensitive: accents are case-folded but never stripped" {
		// Case folding still applies to accented letters.
		SoftwareMedicalFileImport.shallowNormalizeName("Beneét") shouldBe "beneét"
		SoftwareMedicalFileImport.shallowNormalizeName("BENEÉT") shouldBe "beneét"

		// But the accented form is NOT equal to the unaccented form.
		SoftwareMedicalFileImport.shallowNormalizeName("Beneét") shouldNotBe
			SoftwareMedicalFileImport.shallowNormalizeName("Beneet")
		SoftwareMedicalFileImport.shallowNormalizeName("Müller") shouldNotBe
			SoftwareMedicalFileImport.shallowNormalizeName("Muller")
	}

	"Importing a PMF with a sender hcparty whose <name> has a trailing space stores the HCP with the trimmed name" {
		val patientLogic = mockk<PatientLogic>(relaxed = true)
		val userLogic = mockk<UserLogic>(relaxed = true)
		val healthcarePartyLogic = mockk<HealthcarePartyLogic>(relaxed = true)
		val healthElementLogic = mockk<HealthElementLogic>(relaxed = true)
		val contactLogic = mockk<ContactLogic>(relaxed = true)
		val documentLogic = mockk<DocumentLogic>(relaxed = true)
		val formLogic = mockk<FormLogic>(relaxed = true)
		val formTemplateLogic = mockk<FormTemplateLogic>(relaxed = true)
		val insuranceLogic = mockk<InsuranceLogic>(relaxed = true)

		val byNameCalls = mutableListOf<String>()
		every { healthcarePartyLogic.listHealthcarePartiesByName(any()) } answers {
			byNameCalls += firstArg<String>()
			emptyFlow()
		}
		every { healthcarePartyLogic.listHealthcarePartiesByNihii(any()) } returns emptyFlow()
		every { healthcarePartyLogic.listHealthcarePartiesBySsin(any()) } returns emptyFlow()
		coEvery { healthcarePartyLogic.getHealthcareParty(any()) } returns null

		val importer = SoftwareMedicalFileImport(
			patientLogic = patientLogic,
			userLogic = userLogic,
			healthcarePartyLogic = healthcarePartyLogic,
			healthElementLogic = healthElementLogic,
			contactLogic = contactLogic,
			documentLogic = documentLogic,
			formLogic = formLogic,
			formTemplateLogic = formTemplateLogic,
			insuranceLogic = insuranceLogic,
			idGenerator = UUIDGenerator(),
		)

		val xml = checkNotNull(SoftwareMedicalFileImportTest::class.java.getResourceAsStream("pmf-other-sender.xml")) {
			"Test resource pmf-other-sender.xml is missing"
		}.readBytes()

		val author = User(id = "test-user", healthcarePartyId = "test-hcp-id")

		val results = importer.importSMF(
			inputData = xml,
			author = author,
			language = "en",
			saveToDatabase = false,
			mappings = emptyMap(),
			dest = null,
		)

		results shouldHaveSize 1
		val res = results.single()

		println("== HCPs in ImportResult ==")
		res.hcps.forEach {
			println("id=${it.id} name=${it.name} lastName=${it.lastName} firstName=${it.firstName} nihii=${it.nihii} ssin=${it.ssin} speciality=${it.speciality}")
		}
		println("== listHealthcarePartiesByName called with ==")
		byNameCalls.forEach { println("'$it'") }

		// The lookup key must be trimmed: "Other", not "Other ".
		byNameCalls shouldContainExactly listOf("Other")

		// The newly-created application HCP must be stored with the trimmed name.
		val applicationHcp: HealthcareParty = res.hcps.single { it.speciality == "application" }
		applicationHcp.name shouldBe "Other"
		applicationHcp.nihii shouldBe null
		applicationHcp.ssin shouldBe null

		// Sanity: the persphysician with NIHII 14798636004 is also imported (sender + transaction author dedup).
		val persphysician = res.hcps.single { it.nihii == "14798636004" }
		persphysician.speciality shouldBe "persphysician"

		// saveToDatabase = false, so we never persist anything.
		coVerify(exactly = 0) { healthcarePartyLogic.createHealthcareParty(any()) }
	}

	"An application hcparty whose <name> matches an existing HCP up to case/spaces/punctuation reuses that HCP instead of creating a new one" {
		val patientLogic = mockk<PatientLogic>(relaxed = true)
		val userLogic = mockk<UserLogic>(relaxed = true)
		val healthcarePartyLogic = mockk<HealthcarePartyLogic>(relaxed = true)
		val healthElementLogic = mockk<HealthElementLogic>(relaxed = true)
		val contactLogic = mockk<ContactLogic>(relaxed = true)
		val documentLogic = mockk<DocumentLogic>(relaxed = true)
		val formLogic = mockk<FormLogic>(relaxed = true)
		val formTemplateLogic = mockk<FormTemplateLogic>(relaxed = true)
		val insuranceLogic = mockk<InsuranceLogic>(relaxed = true)

		// A decoy that does NOT match (proves we no longer blindly take the first result),
		// and a match expressed as firstName + lastName with different case/spacing/punctuation.
		val decoy = HealthcareParty(id = "decoy", name = "Something Else", speciality = "application")
		val matchByFullName = HealthcareParty(
			id = "existing-other",
			firstName = "Ot",
			lastName = " HER.",
			speciality = "application",
		)
		every { healthcarePartyLogic.listHealthcarePartiesByName("Other") } returns flowOf(decoy, matchByFullName)
		every { healthcarePartyLogic.listHealthcarePartiesByNihii(any()) } returns emptyFlow()
		every { healthcarePartyLogic.listHealthcarePartiesBySsin(any()) } returns emptyFlow()
		coEvery { healthcarePartyLogic.getHealthcareParty(any()) } returns null

		val importer = SoftwareMedicalFileImport(
			patientLogic = patientLogic,
			userLogic = userLogic,
			healthcarePartyLogic = healthcarePartyLogic,
			healthElementLogic = healthElementLogic,
			contactLogic = contactLogic,
			documentLogic = documentLogic,
			formLogic = formLogic,
			formTemplateLogic = formTemplateLogic,
			insuranceLogic = insuranceLogic,
			idGenerator = UUIDGenerator(),
		)

		val xml = checkNotNull(SoftwareMedicalFileImportTest::class.java.getResourceAsStream("pmf-other-sender.xml")) {
			"Test resource pmf-other-sender.xml is missing"
		}.readBytes()

		val author = User(id = "test-user", healthcarePartyId = "test-hcp-id")

		val results = importer.importSMF(
			inputData = xml,
			author = author,
			language = "en",
			saveToDatabase = false,
			mappings = emptyMap(),
			dest = null,
		)

		val res = results.single()

		// The existing HCP that matched on normalized firstName+lastName is reused, not the decoy.
		val applicationHcp: HealthcareParty = res.hcps.single { it.speciality == "application" }
		applicationHcp.id shouldBe "existing-other"
		res.hcps.none { it.id == "decoy" } shouldBe true

		// saveToDatabase = false, so we never persist anything.
		coVerify(exactly = 0) { healthcarePartyLogic.createHealthcareParty(any()) }
	}
})