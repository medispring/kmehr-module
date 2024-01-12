/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.be.ehealth.logic.kmehr.note.impl.v20161201

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.withContext
import org.apache.commons.logging.LogFactory
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.stereotype.Service
import org.taktik.icure.asynclogic.CodeLogic
import org.taktik.icure.asynclogic.DocumentLogic
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.Utils
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.*
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.id.v1.IDKMEHR
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.id.v1.IDKMEHRschemes
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.schema.v1.*
import org.taktik.icure.be.ehealth.logic.kmehr.Config
import org.taktik.icure.be.ehealth.logic.kmehr.note.KmehrNoteLogic
import org.taktik.icure.be.ehealth.logic.kmehr.v20161201.KmehrExport
import org.taktik.icure.config.KmehrConfiguration
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Patient
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.time.Instant
import java.util.*
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import kotlin.text.Charsets.UTF_8

@Profile("kmehr")
@Service
class KmehrNoteLogicImpl(
    codeLogic: CodeLogic,
    documentLogic: DocumentLogic,
    kmehrConfiguration: KmehrConfiguration
) : KmehrNoteLogic, KmehrExport(codeLogic, documentLogic, kmehrConfiguration) {

    override val log = LogFactory.getLog(KmehrNoteLogicImpl::class.java)

    internal val config = Config(
        _kmehrId = System.currentTimeMillis().toString(),
        date = Utils.makeXGC(Instant.now().toEpochMilli())!!,
        time = Utils.makeXGC(Instant.now().toEpochMilli(), true)!!,
        soft = Config.Software(name = "iCure", version = kmehrConfiguration.kmehrVersion),
        clinicalSummaryType = "",
        defaultLanguage = "en",
    )

    override suspend fun createNote(
        id: String,
        author: HealthcareParty,
        date: Long,
        recipientNihii: String,
        recipientSsin: String,
        recipientFirstName: String,
        recipientLastName: String,
        patient: Patient,
        lang: String,
        transactionType: String,
        mimeType: String,
        document: ByteArray,
    ) = flow {
        val message = Kmehrmessage().apply {
            header = HeaderType().apply {
                standard = StandardType().apply { cd = CDSTANDARD().apply { s = "CD-STANDARD"; value =
                    this@KmehrNoteLogicImpl.standard
                } }
                ids.add(
                    IDKMEHR().apply {
                        s = IDKMEHRschemes.ID_KMEHR; value = "$recipientNihii.${config._kmehrId ?: System.currentTimeMillis()}"
                    }
                )
                ids.add(localIdKmehr(transactionType, id, config))
                this.date = Utils.makeXGC(Instant.now().toEpochMilli())
                this.time = Utils.makeXGC(Instant.now().toEpochMilli())
                this.sender = SenderType().apply {
                    hcparties.add(createParty(author, emptyList()))
                    hcparties.add(createSpecialistParty(author, emptyList()))
                    hcparties.add(HcpartyType().apply { this.cds.addAll(listOf(CDHCPARTY().apply { s(CDHCPARTYschemes.CD_HCPARTY); value = "application" })); this.name = "${config.soft?.name} ${config.soft?.version}" })
                }
                val recipient = HealthcareParty(
                    id = UUID.randomUUID().toString(),
                    lastName = recipientLastName,
                    firstName = recipientFirstName,
                    nihii = recipientNihii,
                    ssin = recipientSsin,
                )
                this.recipients.add(
                    RecipientType().apply {
                        hcparties.add(createParty(recipient, emptyList()))
                    },
                )
            }
            folders.add(
                FolderType().apply {
                    this.ids.add(IDKMEHR().apply { s = IDKMEHRschemes.ID_KMEHR; value = 1.toString() })
                    this.patient = makePerson(patient, config)

                    this.transactions.add(
                        TransactionType().apply {
                            this.ids.add(IDKMEHR().apply { s = IDKMEHRschemes.ID_KMEHR; value = 1.toString() })
                            this.ids.add(localIdKmehr(transactionType, id, config))
                            this.cds.add(CDTRANSACTION().apply { s(CDTRANSACTIONschemes.CD_TRANSACTION); value = transactionType })
                            this.date = Utils.makeXGC(date)
                            this.time = Utils.makeXGC(date)
                            this.author = AuthorType().apply {
                                hcparties.add(createParty(author, emptyList()))
                                hcparties.add(createSpecialistParty(author, emptyList()))
                            }
                            this.isIscomplete = true
                            this.isIsvalidated = true

                            this.headingsAndItemsAndTexts.add(
                                LnkType().apply {
                                    this.type = CDLNKvalues.MULTIMEDIA
                                    this.mediatype = CDMEDIATYPEvalues.fromValue(mimeType)
                                    this.value = document
                                },
                            )
                        },
                    )
                },
            )
        }

        val jaxbMarshaller = JAXBContext.newInstance("org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.schema.v1", com.sun.xml.bind.v2.ContextFactory::class.java.classLoader).createMarshaller()

        val os = ByteArrayOutputStream(10000)
        // output pretty printed
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
        jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, UTF_8.toString())

        withContext(Dispatchers.IO) {
            jaxbMarshaller.marshal(message, OutputStreamWriter(os, "UTF-8"))
        }
        emitAll(DataBufferUtils.read(ByteArrayResource(os.toByteArray()), DefaultDataBufferFactory(), 10000).asFlow())
    }
}
