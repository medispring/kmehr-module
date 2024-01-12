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
enum class InvoicingPrescriberCode(val code: Int) {
    None(0),
    OnePrescriber(1),
    SelfPrescriber(3),
    AddedCode(4),
    ManyPrescribers(9),
    ;

    companion object {

        fun withCode(prescriberCode: Int): InvoicingPrescriberCode? {
            for (ipc in InvoicingPrescriberCode.entries) {
                if (ipc.code == prescriberCode) {
                    return ipc
                }
            }
            return null
        }
    }
}
