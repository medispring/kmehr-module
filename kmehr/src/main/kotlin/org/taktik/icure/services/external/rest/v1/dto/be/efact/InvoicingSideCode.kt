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
enum class InvoicingSideCode(val code: Int) {
    None(0),
    Left(1),
    Right(2),
    ;

    companion object {

        fun withSide(side: Int): InvoicingSideCode? {
            for (s in InvoicingSideCode.entries) {
                if (s.code == side) {
                    return s
                }
            }
            return null
        }
    }
}
