/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.be.format.logic

import kotlinx.coroutines.flow.Flow
import org.springframework.core.io.buffer.DataBuffer
import org.taktik.icure.domain.result.ResultInfo
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.Document
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Patient
import java.time.LocalDateTime

/**
 * This logic defines the methods to import or export data from documents stored as attachments in iCure.
 */
interface ResultFormatLogic {

    /**
     * This method checks if the logic that implements this interface can handle the type of document that is stored
     * in the main attachment of the iCure Document passed as parameter.
     * Each logic class will have its own criteria.
     * @param doc the iCure Document where to extract the attachment.
     * @param encKeys the keys used to decrypt the document.
     * @param cachedAttachment if not null, this is used instead of retrieving the main attachment.
     * @return true if the logic can parse the document, false otherwise.
     */
    suspend fun canHandle(doc: Document, encKeys: List<String>, cachedAttachment: ByteArray? = null): Boolean

    /**
     * Retrieves the main attachment of an iCure Document, tries to decrypt it and parses all the lines. It creates a
     * ResultInfo for each line and creates services when needed.
     * @param doc the iCure Document where to extract the attachment.
     * @param full
     * @param language the language of the Content of the Services that will be created in the ResultInfo.
     * @param encKeys the keys used to decrypt the attachment.
     * @param cachedAttachment if not null, this is used instead of retrieving the main attachment.
     * @return a List of ResultInfo.
     */
    suspend fun getInfos(doc: Document, full: Boolean, language: String, encKeys: List<String>, cachedAttachment: ByteArray? = null): List<ResultInfo>

    /**
     * Imports a series of protocols from a document, it parses them into Services and SubContacts and adds them to an
     * existing Contact.
     * @param language the language of the Content of the Services.
     * @param doc the Document where to extract the attachment.
     * @param hcpId the id of the HCP responsible for the SubContacts.
     * @param protocolIds
     * @param formIds
     * @param planOfActionId
     * @param ctc the existing Contact.
     * @param encKeys the keys used to decrypt the attachment.
     * @param cachedAttachment if not null, this is used instead of retrieving the main attachment.
     * @return the updated Contact.
     */
    suspend fun doImport(language: String, doc: Document, hcpId: String?, protocolIds: List<String>, formIds: List<String>, planOfActionId: String?, ctc: Contact, encKeys: List<String>, cachedAttachment: ByteArray? = null): Contact?

    /**
     * Creates an XML message with the provided date.
     * @param sender the HCP responsible for the creation.
     * @param recipient the HCP recipient of the message.
     * @param patient the Patient object of the message.
     * @param date the date of the message.
     * @param ref
     * @param text
     * @return the XML as a Flow of DataBuffer.
     */
    fun doExport(sender: HealthcareParty?, recipient: HealthcareParty?, patient: Patient?, date: LocalDateTime?, ref: String?, text: String?): Flow<DataBuffer>

    /**
     * Creates an XML message with the provided date.
     * @param sender the HCP responsible for the creation.
     * @param recipient the HCP recipient of the message.
     * @param patient the Patient object of the message.
     * @param date the date of the message.
     * @param mimeType the MIME type of the content to include in the message.
     * @param content the data to include in the message.
     * @return the XML as a Flow of DataBuffer.
     */
    fun doExport(sender: HealthcareParty?, recipient: HealthcareParty?, patient: Patient?, date: LocalDateTime?, ref: String?, mimeType: String?, content: ByteArray?): Flow<DataBuffer>
}
