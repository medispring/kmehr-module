/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.be.efact

/**
 * Created with IntelliJ IDEA.
 * User: aduchate
 * Date: 19/08/15
 * Time: 10:43
 * To change this template use File | Settings | File Templates.
 */
enum class InvoicingTimeOfDay(val code: Int) {
    Other(0),
    Night(1),
    Weekend(2),
    Bankholiday(3),
    Urgent(4),
    ;

    companion object {

        fun withCode(code: Int): InvoicingTimeOfDay? {
            for (itd in InvoicingTimeOfDay.entries) {
                if (itd.code == code) {
                    return itd
                }
            }
            return null
        }
    }
}
