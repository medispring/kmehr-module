/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2019.06.14 at 03:49:54 PM CEST
//


package org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CD-EMERGENCYEVALUATIONvalues.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="CD-EMERGENCYEVALUATIONvalues">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="couldwaitafterwe"/>
 *     &lt;enumeration value="noturgent"/>
 *     &lt;enumeration value="urgent"/>
 *     &lt;enumeration value="lifethreathning"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 *
 */
@XmlType(name = "CD-EMERGENCYEVALUATIONvalues")
@XmlEnum
public enum CDEMERGENCYEVALUATIONvalues {

    @XmlEnumValue("couldwaitafterwe")
    COULDWAITAFTERWE("couldwaitafterwe"),
    @XmlEnumValue("noturgent")
    NOTURGENT("noturgent"),
    @XmlEnumValue("urgent")
    URGENT("urgent"),
    @XmlEnumValue("lifethreathning")
    LIFETHREATHNING("lifethreathning");
    private final String value;

    CDEMERGENCYEVALUATIONvalues(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CDEMERGENCYEVALUATIONvalues fromValue(String v) {
        for (CDEMERGENCYEVALUATIONvalues c: CDEMERGENCYEVALUATIONvalues.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
