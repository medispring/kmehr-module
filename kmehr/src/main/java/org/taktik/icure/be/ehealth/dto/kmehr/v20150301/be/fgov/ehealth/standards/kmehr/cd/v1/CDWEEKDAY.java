/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2019.06.14 at 03:49:19 PM CEST
//


package org.taktik.icure.be.ehealth.dto.kmehr.v20150301.be.fgov.ehealth.standards.kmehr.cd.v1;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * <p>Java class for CD-WEEKDAY complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="CD-WEEKDAY">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.ehealth.fgov.be/standards/kmehr/cd/v1>CD-WEEKDAYvalues">
 *       &lt;attribute name="S" use="required" type="{http://www.w3.org/2001/XMLSchema}string" fixed="CD-WEEKDAY" />
 *       &lt;attribute name="SV" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CD-WEEKDAY", propOrder = {
    "value"
})
public class CDWEEKDAY
    implements Serializable
{

    private final static long serialVersionUID = 20150301L;
    @XmlValue
    protected CDWEEKDAYvalues value;
    @XmlAttribute(name = "S", required = true)
    protected String s = "CD-WEEKDAY";
    @XmlAttribute(name = "SV", required = true)
    protected String sv = "1.0";

    /**
     * Gets the value of the value property.
     *
     * @return
     *     possible object is
     *     {@link CDWEEKDAYvalues }
     *
     */
    public CDWEEKDAYvalues getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     *
     * @param value
     *     allowed object is
     *     {@link CDWEEKDAYvalues }
     *
     */
    public void setValue(CDWEEKDAYvalues value) {
        this.value = value;
    }

    /**
     * Gets the value of the s property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getS() {
        if (s == null) {
            return "CD-WEEKDAY";
        } else {
            return s;
        }
    }

    /**
     * Sets the value of the s property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setS(String value) {
        this.s = value;
    }

    /**
     * Gets the value of the sv property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getSV() {
        return sv;
    }

    /**
     * Sets the value of the sv property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setSV(String value) {
        this.sv = value;
    }

}
