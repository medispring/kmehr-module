/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2019.06.14 at 03:49:30 PM CEST
//


package org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20141201.be.fgov.ehealth.standards.kmehr.cd.v1;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CD-MAA-REQUESTTYPEvalues.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="CD-MAA-REQUESTTYPEvalues">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="newrequest"/>
 *     &lt;enumeration value="extension"/>
 *     &lt;enumeration value="noncontinuousextension"/>
 *     &lt;enumeration value="complimentaryannex"/>
 *     &lt;enumeration value="cancellation"/>
 *     &lt;enumeration value="closure"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 *
 */
@XmlType(name = "CD-MAA-REQUESTTYPEvalues")
@XmlEnum
public enum CDMAAREQUESTTYPEvalues {

    @XmlEnumValue("newrequest")
    NEWREQUEST("newrequest"),
    @XmlEnumValue("extension")
    EXTENSION("extension"),
    @XmlEnumValue("noncontinuousextension")
    NONCONTINUOUSEXTENSION("noncontinuousextension"),
    @XmlEnumValue("complimentaryannex")
    COMPLIMENTARYANNEX("complimentaryannex"),
    @XmlEnumValue("cancellation")
    CANCELLATION("cancellation"),
    @XmlEnumValue("closure")
    CLOSURE("closure");
    private final String value;

    CDMAAREQUESTTYPEvalues(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CDMAAREQUESTTYPEvalues fromValue(String v) {
        for (CDMAAREQUESTTYPEvalues c: CDMAAREQUESTTYPEvalues.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
