/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2019.06.14 at 03:48:37 PM CEST
//


package org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20131001.be.fgov.ehealth.standards.kmehr.cd.v1;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CD-EBIRTH-CHILDPOSITIONvalues.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="CD-EBIRTH-CHILDPOSITIONvalues">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="head-down"/>
 *     &lt;enumeration value="other-head"/>
 *     &lt;enumeration value="breech"/>
 *     &lt;enumeration value="transverse"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 *
 */
@XmlType(name = "CD-EBIRTH-CHILDPOSITIONvalues")
@XmlEnum
public enum CDEBIRTHCHILDPOSITIONvalues {

    @XmlEnumValue("head-down")
    HEAD_DOWN("head-down"),
    @XmlEnumValue("other-head")
    OTHER_HEAD("other-head"),
    @XmlEnumValue("breech")
    BREECH("breech"),
    @XmlEnumValue("transverse")
    TRANSVERSE("transverse");
    private final String value;

    CDEBIRTHCHILDPOSITIONvalues(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CDEBIRTHCHILDPOSITIONvalues fromValue(String v) {
        for (CDEBIRTHCHILDPOSITIONvalues c: CDEBIRTHCHILDPOSITIONvalues.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
