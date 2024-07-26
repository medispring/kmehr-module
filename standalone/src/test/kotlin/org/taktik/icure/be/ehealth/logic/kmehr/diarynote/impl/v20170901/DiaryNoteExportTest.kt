package org.taktik.icure.be.ehealth.logic.kmehr.diarynote.impl.v20170901

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.toList
import org.junit.jupiter.api.TestInstance
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Patient
import org.taktik.icure.test.BaseKmehrTest
import org.taktik.icure.test.combineToString
import org.taktik.icure.test.uuid

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DiaryNoteExportTest(
	private val diaryNoteExport: DiaryNoteExport,
) : BaseKmehrTest() {

	init {
		diaryNoteExportTest()
	}

	private fun StringSpec.diaryNoteExportTest() {

		"CSM-385 - Folders should appear only once in the export" {
			val xml = diaryNoteExport.createDiaryNote(
				pat = Patient(id = uuid()),
				sfks = listOf(uuid()),
				sender = HealthcareParty(id = uuid()),
				recipient = HealthcareParty(id = uuid()),
				note = "Some note",
				tags = listOf("Tag 1", "Test"),
				contexts = listOf("Context?"),
				isPsy = false,
				documentId = null,
				attachmentId = null
			).toList().combineToString()
			val folderMatches = Regex("<folder>").findAll(xml).toList()
			folderMatches.size shouldBe 1
		}

	}

}