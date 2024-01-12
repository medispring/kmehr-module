/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2019.06.14 at 03:49:37 PM CEST
//


package org.taktik.icure.be.ehealth.dto.kmehr.v20180301.be.fgov.ehealth.standards.kmehr.cd.v1;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CD-GALENICFORMschemes.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="CD-GALENICFORMschemes">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="CD-DRUG-PRESENTATION"/>
 *     &lt;enumeration value="CD-MAGISTRALFORM"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 *
 */
@XmlType(name = "CD-GALENICFORMschemes")
@XmlEnum
public enum CDGALENICFORMschemes {

    @XmlEnumValue("CD-DRUG-PRESENTATION")
    CD_DRUG_PRESENTATION("CD-DRUG-PRESENTATION"),
    @XmlEnumValue("CD-MAGISTRALFORM")
    CD_MAGISTRALFORM("CD-MAGISTRALFORM");
    private final String value;

    CDGALENICFORMschemes(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CDGALENICFORMschemes fromValue(String v) {
        for (CDGALENICFORMschemes c: CDGALENICFORMschemes.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
