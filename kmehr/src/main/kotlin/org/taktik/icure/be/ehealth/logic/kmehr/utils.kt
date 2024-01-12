/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.be.ehealth.logic.kmehr

import com.sun.xml.bind.v2.ContextFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.taktik.icure.be.ehealth.dto.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.schema.v1.Kmehrmessage
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.embed.PatientHealthCareParty
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller

fun validSsinOrNull(ssin: String?): String? =
    ssin
        ?.replace(" ", "")
        ?.replace("-", "")
        ?.replace(".", "")
        ?.replace("/", "")
        ?.takeIf { it.length == 11 }

fun validNihiiOrNull(nihii: String?): String? = validSsinOrNull(nihii)

fun emitMessage(message: Kmehrmessage): Flow<DataBuffer> {
    val os = ByteArrayOutputStream(10000)

    val jaxbMarshaller = createMarshaller("org.taktik.icure.be.ehealth.dto.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.schema.v1")
    jaxbMarshaller.marshal(message, OutputStreamWriter(os, "UTF-8"))

    return DataBufferUtils.read(ByteArrayResource(os.toByteArray()), DefaultDataBufferFactory(), 10000).asFlow()
}

fun emitMessage(message: org.taktik.icure.be.ehealth.dto.kmehr.v20110701.be.fgov.ehealth.standards.kmehr.schema.v1.Kmehrmessage): Flow<DataBuffer> {
    val os = ByteArrayOutputStream(10000)

    val jaxbMarshaller = createMarshaller("org.taktik.icure.be.ehealth.dto.kmehr.v20110701.be.fgov.ehealth.standards.kmehr.schema.v1")
    jaxbMarshaller.marshal(message, OutputStreamWriter(os, "UTF-8"))
    return DataBufferUtils.read(ByteArrayResource(os.toByteArray()), DefaultDataBufferFactory(), 10000).asFlow()
}

fun emitMessage(message: org.taktik.icure.be.ehealth.dto.kmehr.v20131001.be.fgov.ehealth.standards.kmehr.schema.v1.Kmehrmessage): Flow<DataBuffer> {
    val os = ByteArrayOutputStream(10000)

    val jaxbMarshaller = createMarshaller("org.taktik.icure.be.ehealth.dto.kmehr.v20131001.be.fgov.ehealth.standards.kmehr.schema.v1")
    jaxbMarshaller.marshal(message, OutputStreamWriter(os, "UTF-8"))
    return DataBufferUtils.read(ByteArrayResource(os.toByteArray()), DefaultDataBufferFactory(), 10000).asFlow()
}

fun emitMessage(message: org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.schema.v1.Kmehrmessage): Flow<DataBuffer> {
    val os = ByteArrayOutputStream(10000)

    val jaxbMarshaller = createMarshaller("org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.schema.v1")
    jaxbMarshaller.marshal(message, OutputStreamWriter(os, "UTF-8"))
    return DataBufferUtils.read(ByteArrayResource(os.toByteArray()), DefaultDataBufferFactory(), 10000).asFlow()
}

fun emitMessage(message: org.taktik.icure.be.ehealth.dto.kmehr.v20170601.be.fgov.ehealth.standards.kmehr.schema.v1.Kmehrmessage): Flow<DataBuffer> {
    val os = ByteArrayOutputStream(10000)

    val jaxbMarshaller = createMarshaller("org.taktik.icure.be.ehealth.dto.kmehr.v20170601.be.fgov.ehealth.standards.kmehr.schema.v1")
    jaxbMarshaller.marshal(message, OutputStreamWriter(os, "UTF-8"))
    return DataBufferUtils.read(ByteArrayResource(os.toByteArray()), DefaultDataBufferFactory(), 10000).asFlow()
}

private fun createMarshaller(pkg: String): Marshaller {
    val jaxbMarshaller = JAXBContext.newInstance(pkg, ContextFactory::class.java.classLoader).createMarshaller()
    // output pretty printed
    jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
    jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8")

    return jaxbMarshaller
}

/**
 * Calculates the MD5 signature of a patient
 */
fun Patient.getSignature(): String = DigestUtils.md5Hex(
    "${this.firstName}:${this.lastName}:${this.patientHealthCareParties.find(PatientHealthCareParty::referral)?.let { "" + it.healthcarePartyId + it.referralPeriods.last().startDate + it.referralPeriods.last().endDate } ?: ""}:${this.dateOfBirth}:${this.dateOfDeath}:${this.ssin}",
)
