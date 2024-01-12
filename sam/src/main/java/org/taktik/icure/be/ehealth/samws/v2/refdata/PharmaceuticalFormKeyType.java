/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2019.05.22 at 08:11:32 PM CEST
//


package org.taktik.icure.be.ehealth.samws.v2.refdata;

import org.taktik.icure.be.ehealth.samws.v2.consultation.PharmaceuticalFormWithStandardsType;

import javax.xml.bind.annotation.*;
import java.io.Serializable;


/**
 * <p>Java class for PharmaceuticalFormKeyType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="PharmaceuticalFormKeyType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="code" use="required" type="{urn:be:fgov:ehealth:samws:v2:core}String10Type" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PharmaceuticalFormKeyType")
@XmlSeeAlso({
    org.taktik.icure.be.ehealth.samws.v2.refdata.PharmaceuticalFormType.class,
    PharmaceuticalFormWithStandardsType.class,
    org.taktik.icure.be.ehealth.samws.v2.consultation.PharmaceuticalFormType.class
})
public class PharmaceuticalFormKeyType
    implements Serializable
{

    private final static long serialVersionUID = 2L;
    @XmlAttribute(name = "code", required = true)
    protected String code;

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
