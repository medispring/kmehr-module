/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2019.06.14 at 03:49:26 PM CEST
//


package org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20150901.be.fgov.ehealth.standards.kmehr.cd.v1;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CD-MS-ADAPTATIONvalues.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="CD-MS-ADAPTATIONvalues">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="nochanges"/>
 *     &lt;enumeration value="medication"/>
 *     &lt;enumeration value="posology"/>
 *     &lt;enumeration value="treatmentsuspension"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 *
 */
@XmlType(name = "CD-MS-ADAPTATIONvalues")
@XmlEnum
public enum CDMSADAPTATIONvalues {

    @XmlEnumValue("nochanges")
    NOCHANGES("nochanges"),
    @XmlEnumValue("medication")
    MEDICATION("medication"),
    @XmlEnumValue("posology")
    POSOLOGY("posology"),
    @XmlEnumValue("treatmentsuspension")
    TREATMENTSUSPENSION("treatmentsuspension");
    private final String value;

    CDMSADAPTATIONvalues(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CDMSADAPTATIONvalues fromValue(String v) {
        for (CDMSADAPTATIONvalues c: CDMSADAPTATIONvalues.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
