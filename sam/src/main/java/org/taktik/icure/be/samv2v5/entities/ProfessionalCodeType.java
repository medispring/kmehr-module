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
import java.math.BigDecimal;


/**
 * <p>Java class for ProfessionalCodeType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="ProfessionalCodeType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="NameId" type="{urn:be:fgov:ehealth:samws:v2:core}Number10Type"/>
 *         &lt;element name="ProfessionalName" type="{urn:be:fgov:ehealth:samws:v2:core}String50Type" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="ProfessionalCV" use="required" type="{urn:be:fgov:ehealth:samws:v2:core}String10Type" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ProfessionalCodeType", namespace = "urn:be:fgov:ehealth:samws:v2:refdata", propOrder = {
    "nameId",
    "professionalName"
})
public class ProfessionalCodeType {

    @XmlElement(name = "NameId", namespace = "urn:be:fgov:ehealth:samws:v2:refdata", required = true)
    protected BigDecimal nameId;
    @XmlElement(name = "ProfessionalName", namespace = "urn:be:fgov:ehealth:samws:v2:refdata")
    protected String professionalName;
    @XmlAttribute(name = "ProfessionalCV", required = true)
    protected String professionalCV;

    /**
     * Gets the value of the nameId property.
     *
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *
     */
    public BigDecimal getNameId() {
        return nameId;
    }

    /**
     * Sets the value of the nameId property.
     *
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *
     */
    public void setNameId(BigDecimal value) {
        this.nameId = value;
    }

    /**
     * Gets the value of the professionalName property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getProfessionalName() {
        return professionalName;
    }

    /**
     * Sets the value of the professionalName property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setProfessionalName(String value) {
        this.professionalName = value;
    }

    /**
     * Gets the value of the professionalCV property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getProfessionalCV() {
        return professionalCV;
    }

    /**
     * Sets the value of the professionalCV property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setProfessionalCV(String value) {
        this.professionalCV = value;
    }

}
