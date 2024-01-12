/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2019.06.14 at 03:49:07 PM CEST
//


package org.taktik.icure.be.ehealth.dto.kmehr.v20170301.be.fgov.ehealth.standards.kmehr.cd.v1;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CD-MAA-COVERAGETYPEvalues.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="CD-MAA-COVERAGETYPEvalues">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="chapter4limited"/>
 *     &lt;enumeration value="chapter4unlimited"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 *
 */
@XmlType(name = "CD-MAA-COVERAGETYPEvalues")
@XmlEnum
public enum CDMAACOVERAGETYPEvalues {

    @XmlEnumValue("chapter4limited")
    CHAPTER_4_LIMITED("chapter4limited"),
    @XmlEnumValue("chapter4unlimited")
    CHAPTER_4_UNLIMITED("chapter4unlimited");
    private final String value;

    CDMAACOVERAGETYPEvalues(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CDMAACOVERAGETYPEvalues fromValue(String v) {
        for (CDMAACOVERAGETYPEvalues c: CDMAACOVERAGETYPEvalues.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
