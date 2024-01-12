/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.be.ehealth.logic.kmehr.diarynote.impl.v20170901

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.be.ehealth.logic.kmehr.diarynote.DiaryNoteLogic
import org.taktik.icure.domain.mapping.ImportMapping
import org.taktik.icure.domain.result.ImportResult
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.User
import java.io.InputStream

@Profile("kmehr")
@Service("diaryNoteLogic")
class DiaryNoteLogicImpl(
    @Qualifier("dairyNoteExport") val diaryNoteExport: DiaryNoteExport,
    @Qualifier("diaryNoteImport") val diaryNoteImport: DiaryNoteImport
) : DiaryNoteLogic {
    override fun createDiaryNote(
        pat: Patient,
        sfks: List<String>,
        sender: HealthcareParty,
        recipient: HealthcareParty,
        note: String?,
        tags: List<String>,
        contexts: List<String>,
        isPsy: Boolean,
        documentId: String?,
        attachmentId: String?) = flow {
            emitAll(
                diaryNoteExport.createDiaryNote(pat, sfks, sender, recipient, note, tags, contexts, isPsy, documentId, attachmentId)
            )
        }

    override fun importDiaryNote(
        inputStream: InputStream,
        author: User,
        language: String,
        dest: Patient?,
        mappings: Map<String, List<ImportMapping>>): List<ImportResult> =
            diaryNoteImport.importDiaryNote(inputStream, author, language, mappings, dest)
}
