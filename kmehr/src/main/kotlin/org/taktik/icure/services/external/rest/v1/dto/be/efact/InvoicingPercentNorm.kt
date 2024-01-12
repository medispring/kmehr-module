/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.be.efact

/**
 * Created with IntelliJ IDEA.
 * User: aduchate
 * Date: 19/08/15
 * Time: 11:13
 * To change this template use File | Settings | File Templates.
 */
enum class InvoicingPercentNorm(val code: Int) {
    None(0),
    SurgicalAid1(1),
    SurgicalAid2(2),
    ReducedFee(3),
    Ah1n1(4),
    HalfPriceSecondAct(5),
    InvoiceException(6),
    ForInformation(7),
    ;

    companion object {

        fun withCode(prescriberCode: Int): InvoicingPercentNorm? {
            for (ipc in InvoicingPercentNorm.entries) {
                if (ipc.code == prescriberCode) {
                    return ipc
                }
            }
            return null
        }
    }
}
