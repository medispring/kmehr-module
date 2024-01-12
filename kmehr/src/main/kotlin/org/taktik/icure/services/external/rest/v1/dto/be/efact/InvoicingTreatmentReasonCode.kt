/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.be.efact

/**
 * Created with IntelliJ IDEA.
 * User: aduchate
 * Date: 19/08/15
 * Time: 11:57
 * To change this template use File | Settings | File Templates.
 */
enum class InvoicingTreatmentReasonCode(val code: Int) {
    Chimiotherapy(50),
    ProfessionalDisease(60),
    WorkAccident(70),
    Accident(80),
    Other(0),
}
