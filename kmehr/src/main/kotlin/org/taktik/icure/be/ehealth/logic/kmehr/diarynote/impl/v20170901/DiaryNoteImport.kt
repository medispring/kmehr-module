/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.be.ehealth.logic.kmehr.diarynote.impl.v20170901

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asynclogic.ContactLogic
import org.taktik.icure.domain.mapping.ImportMapping
import org.taktik.icure.domain.result.ImportResult
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.User
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.schema.v1.Kmehrmessage
import java.io.InputStream
import java.util.LinkedList
import javax.xml.bind.JAXBContext

@Profile("kmehr")
@Service("diaryNoteImport")
class DiaryNoteImport(val contactLogic: ContactLogic) {

    /**
     * Imports a Diary Note XML into iCure.
     * @param inputStream the InputStream containing the XML.
     * @param author the User responsible for the import.
     * @param language the language of the Content.
     * @param mappings
     * @param dest
     * @return a List of ImportResult.
     */
    fun importDiaryNote(
        inputStream: InputStream,
        author: User,
        language: String,
        mappings: Map<String, List<ImportMapping>>,
        dest: Patient? = null,
    ): List<ImportResult> {
        val jc = JAXBContext.newInstance(Kmehrmessage::class.java)

        val unmarshaller = jc.createUnmarshaller()
        val kmehrMessage = unmarshaller.unmarshal(inputStream) as Kmehrmessage

        val allRes = LinkedList<ImportResult>()

        val standard = kmehrMessage.header.standard.cd.value

        // TODO Might want to have several implementations babsed on standards
//        kmehrMessage.header.sender.hcparties?.forEach { createOrProcessHcp(it) }
//        kmehrMessage.folders.forEach { folder ->
//            val res = ImportResult().apply { allRes.add(this) }
//            createOrProcessPatient(folder.patient, author, res, dest)?.let { patient ->
//                res.patient = patient
//                folder.transactions.forEach { trn ->
//                    val ctc: Contact = when (trn.cds.find { it.s == CDTRANSACTIONschemes.CD_TRANSACTION }?.value) {
//                        "sumehr" -> parseSumehr(trn, author, res, language, mappings)
//                        else -> parseGenericTransaction(trn, author, res, language, mappings)
//                    }
//                    contactLogic.createContact(ctc)
//                    res.ctcs.add(ctc)
//                }
//            }
//        }
        return allRes
    }
}
