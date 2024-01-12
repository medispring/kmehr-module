/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2019.06.14 at 03:49:33 PM CEST
//


package org.taktik.icure.be.ehealth.dto.kmehr.v20180901.be.fgov.ehealth.standards.kmehr.cd.v1;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CD-COUNTRYschemes.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="CD-COUNTRYschemes">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="CD-COUNTRY"/>
 *     &lt;enumeration value="CD-FED-COUNTRY"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 *
 */
@XmlType(name = "CD-COUNTRYschemes")
@XmlEnum
public enum CDCOUNTRYschemes {

    @XmlEnumValue("CD-COUNTRY")
    CD_COUNTRY("CD-COUNTRY", "1.0"),
    @XmlEnumValue("CD-FED-COUNTRY")
    CD_FED_COUNTRY("CD-FED-COUNTRY", "1.2");
    private final String value; //
    private final String version;
    CDCOUNTRYschemes(String v, String vs) {
        value = v;
        version = vs;
    }

    public String value() {
        return value;
    } //

    public String version() {
        return version;
    }

    public static CDCOUNTRYschemes fromValue(String v) {
        for (CDCOUNTRYschemes c: CDCOUNTRYschemes.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
