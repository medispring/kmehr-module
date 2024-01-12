/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2019.06.14 at 03:48:52 PM CEST
//


package org.taktik.icure.be.ehealth.dto.kmehr.v20100901.be.fgov.ehealth.standards.kmehr.id.v1;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ID-PATIENTschemes.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="ID-PATIENTschemes">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="ID-PATIENT"/>
 *     &lt;enumeration value="INSS"/>
 *     &lt;enumeration value="LOCAL"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 */
@XmlType(name = "ID-PATIENTschemes")
@XmlEnum
public enum IDPATIENTschemes {

    @XmlEnumValue("ID-PATIENT")
    ID_PATIENT("ID-PATIENT"),
    INSS("INSS"),
    LOCAL("LOCAL");
    private final String value;

    IDPATIENTschemes(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static IDPATIENTschemes fromValue(String v) {
        for (IDPATIENTschemes c : IDPATIENTschemes.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
