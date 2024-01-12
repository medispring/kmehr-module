/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.be.efact

/**
 * Created with IntelliJ IDEA.
 * User: gpiroux
 * Date: 22/11/18
 * Time: 11:13
 * To change this template use File | Settings | File Templates.
 */
enum class InvoicingDerogationMaxNumberCode(val code: Int) {
	/*
	 01 Dérogation au nombre maximal.
	 02 Autre séance/prescription.
	 03 2e prestation identique de la journée.
	 04 3e ou suivante prestation identique de la journée.
	 00 Dans les autres cas.
	*/

    Other(0),
    DerogationMaxNumber(1),
    OtherPrescription(2),
    SecondPrestationOfDay(3),
    ThirdAndNextPrestationOfDay(4),
    ;

    companion object {

        fun withCode(derogationMaxNumberCode: Int): InvoicingDerogationMaxNumberCode? {
            for (idc in InvoicingDerogationMaxNumberCode.entries) {
                if (idc.code == derogationMaxNumberCode) {
                    return idc
                }
            }
            return null
        }
    }
}
