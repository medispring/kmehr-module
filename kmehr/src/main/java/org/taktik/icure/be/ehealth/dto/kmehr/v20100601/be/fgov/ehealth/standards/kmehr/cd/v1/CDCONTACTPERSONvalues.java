/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2019.06.14 at 03:49:49 PM CEST
//


package org.taktik.icure.be.ehealth.dto.kmehr.v20100601.be.fgov.ehealth.standards.kmehr.cd.v1;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CD-CONTACT-PERSONvalues.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="CD-CONTACT-PERSONvalues">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="mother"/>
 *     &lt;enumeration value="father"/>
 *     &lt;enumeration value="child"/>
 *     &lt;enumeration value="familymember"/>
 *     &lt;enumeration value="spouse"/>
 *     &lt;enumeration value="husband"/>
 *     &lt;enumeration value="partner"/>
 *     &lt;enumeration value="other"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 */
@XmlType(name = "CD-CONTACT-PERSONvalues")
@XmlEnum
public enum CDCONTACTPERSONvalues {

    @XmlEnumValue("mother")
    MOTHER("mother"),
    @XmlEnumValue("father")
    FATHER("father"),
    @XmlEnumValue("child")
    CHILD("child"),
    @XmlEnumValue("familymember")
    FAMILYMEMBER("familymember"),
    @XmlEnumValue("spouse")
    SPOUSE("spouse"),
    @XmlEnumValue("husband")
    HUSBAND("husband"),
    @XmlEnumValue("partner")
    PARTNER("partner"),
    @XmlEnumValue("other")
    OTHER("other");
    private final String value;

    CDCONTACTPERSONvalues(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CDCONTACTPERSONvalues fromValue(String v) {
        for (CDCONTACTPERSONvalues c : CDCONTACTPERSONvalues.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
