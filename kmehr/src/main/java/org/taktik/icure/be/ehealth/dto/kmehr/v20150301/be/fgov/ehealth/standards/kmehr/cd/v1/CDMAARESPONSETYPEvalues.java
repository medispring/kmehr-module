/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2019.06.14 at 03:49:19 PM CEST
//


package org.taktik.icure.be.ehealth.dto.kmehr.v20150301.be.fgov.ehealth.standards.kmehr.cd.v1;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CD-MAA-RESPONSETYPEvalues.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="CD-MAA-RESPONSETYPEvalues">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="agreement"/>
 *     &lt;enumeration value="refusal"/>
 *     &lt;enumeration value="intreatment"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 *
 */
@XmlType(name = "CD-MAA-RESPONSETYPEvalues")
@XmlEnum
public enum CDMAARESPONSETYPEvalues {

    @XmlEnumValue("agreement")
    AGREEMENT("agreement"),
    @XmlEnumValue("refusal")
    REFUSAL("refusal"),
    @XmlEnumValue("intreatment")
    INTREATMENT("intreatment");
    private final String value;

    CDMAARESPONSETYPEvalues(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CDMAARESPONSETYPEvalues fromValue(String v) {
        for (CDMAARESPONSETYPEvalues c: CDMAARESPONSETYPEvalues.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
