/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2019.06.14 at 03:48:57 PM CEST
//


package org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20110701.be.fgov.ehealth.standards.kmehr.id.v1;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * <p>Java class for INSS complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="INSS">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attribute name="S" use="required" type="{http://www.ehealth.fgov.be/standards/kmehr/id/v1}ID-PATIENTschemes" />
 *       &lt;attribute name="SV" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="SL" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "INSS", propOrder = {
    "value"
})
public class INSS
    implements Serializable
{

    private final static long serialVersionUID = 20110701L;
    @XmlValue
    protected String value;
    @XmlAttribute(name = "S", required = true)
    protected IDPATIENTschemes s;
    @XmlAttribute(name = "SV", required = true)
    protected String sv;
    @XmlAttribute(name = "SL")
    protected String sl;

    /**
     * Gets the value of the value property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the value of the s property.
     *
     * @return
     *     possible object is
     *     {@link IDPATIENTschemes }
     *
     */
    public IDPATIENTschemes getS() {
        return s;
    }

    /**
     * Sets the value of the s property.
     *
     * @param value
     *     allowed object is
     *     {@link IDPATIENTschemes }
     *
     */
    public void setS(IDPATIENTschemes value) {
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

    /**
     * Gets the value of the sl property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getSL() {
        return sl;
    }

    /**
     * Sets the value of the sl property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setSL(String value) {
        this.sl = value;
    }

}
