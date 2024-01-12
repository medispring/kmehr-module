/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.be.ehealth

import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Insurance
import org.taktik.icure.entities.Invoice
import org.taktik.icure.services.external.rest.v1.dto.be.efact.MessageWithBatch

/**
 * The classes that implement this interface will manage the communication with the eFact online invoice system.
 * https://efact.be/en
 */
interface EfactLogic {

    /**
     * Creates a new invoices batch from a set of invoices all related to the same insurance and to the same HCP.
     * The invoice batch creates will be compatible with the eFact online invoice system.
     * The invoices can be related to different patients.
     * @param messageId the UUID of the message to create, as String
     * @param hcp the HCP creating the invoice
     * @param insurance the Insurance recipient of the invoice
     * @param invoices a map where each key is the ID of a Patient and each value is a list of invoices
     * @return a message with a batch of invoices
     */
    suspend fun prepareBatch(messageId: String, hcp: HealthcareParty, insurance: Insurance, invoices: Map<String, List<Invoice>>): MessageWithBatch?
}
