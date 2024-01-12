/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2019.05.22 at 08:11:32 PM CEST
//


package org.taktik.icure.be.ehealth.samws.v2.actual.common;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ContentTypeType.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="ContentTypeType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="ACTIVE_COMPONENT"/>
 *     &lt;enumeration value="SOLVENT"/>
 *     &lt;enumeration value="DEVICE"/>
 *     &lt;enumeration value="EXCIPIENT"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 *
 */
@XmlType(name = "ContentTypeType")
@XmlEnum
public enum ContentTypeType {

    ACTIVE_COMPONENT,
    SOLVENT,
    DEVICE,
    EXCIPIENT;

    public String value() {
        return name();
    }

    public static ContentTypeType fromValue(String v) {
        return valueOf(v);
    }

}
