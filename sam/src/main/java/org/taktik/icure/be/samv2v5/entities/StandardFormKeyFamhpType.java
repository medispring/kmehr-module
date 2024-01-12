/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2020.10.15 at 03:32:18 PM CEST
//


package org.taktik.icure.be.samv2v5.entities;

import javax.xml.bind.annotation.*;


/**
 * <p>Java class for StandardFormKeyFamhpType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="StandardFormKeyFamhpType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="standard" use="required" type="{urn:be:fgov:ehealth:samws:v2:refdata}StdFrmFamhpStandardsType" />
 *       &lt;attribute name="code" use="required" type="{urn:be:fgov:ehealth:samws:v2:core}String20Type" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StandardFormKeyFamhpType", namespace = "urn:be:fgov:ehealth:samws:v2:refdata")
@XmlSeeAlso({
    StandardFormFamhpType.class
})
public class StandardFormKeyFamhpType {

    @XmlAttribute(name = "standard", required = true)
    protected StdFrmFamhpStandardsType standard;
    @XmlAttribute(name = "code", required = true)
    protected String code;

    /**
     * Gets the value of the standard property.
     *
     * @return
     *     possible object is
     *     {@link StdFrmFamhpStandardsType }
     *
     */
    public StdFrmFamhpStandardsType getStandard() {
        return standard;
    }

    /**
     * Sets the value of the standard property.
     *
     * @param value
     *     allowed object is
     *     {@link StdFrmFamhpStandardsType }
     *
     */
    public void setStandard(StdFrmFamhpStandardsType value) {
        this.standard = value;
    }

    /**
     * Gets the value of the code property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getCode() {
        return code;
    }

    /**
     * Sets the value of the code property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setCode(String value) {
        this.code = value;
    }

}
