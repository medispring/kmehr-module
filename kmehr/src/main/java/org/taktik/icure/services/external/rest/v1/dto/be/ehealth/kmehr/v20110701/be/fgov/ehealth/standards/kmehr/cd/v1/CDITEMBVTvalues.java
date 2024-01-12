/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2019.06.14 at 03:48:57 PM CEST
//


package org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20110701.be.fgov.ehealth.standards.kmehr.cd.v1;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CD-ITEM-BVTvalues.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="CD-ITEM-BVTvalues">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="referenceid"/>
 *     &lt;enumeration value="patientopposition"/>
 *     &lt;enumeration value="sample"/>
 *     &lt;enumeration value="biopsynumber"/>
 *     &lt;enumeration value="technicalremarks"/>
 *     &lt;enumeration value="lab"/>
 *     &lt;enumeration value="error"/>
 *     &lt;enumeration value="status"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 *
 */
@XmlType(name = "CD-ITEM-BVTvalues")
@XmlEnum
public enum CDITEMBVTvalues {

    @XmlEnumValue("referenceid")
    REFERENCEID("referenceid"),
    @XmlEnumValue("patientopposition")
    PATIENTOPPOSITION("patientopposition"),
    @XmlEnumValue("sample")
    SAMPLE("sample"),
    @XmlEnumValue("biopsynumber")
    BIOPSYNUMBER("biopsynumber"),
    @XmlEnumValue("technicalremarks")
    TECHNICALREMARKS("technicalremarks"),
    @XmlEnumValue("lab")
    LAB("lab"),
    @XmlEnumValue("error")
    ERROR("error"),
    @XmlEnumValue("status")
    STATUS("status");
    private final String value;

    CDITEMBVTvalues(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CDITEMBVTvalues fromValue(String v) {
        for (CDITEMBVTvalues c: CDITEMBVTvalues.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
