/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2019.05.22 at 08:11:32 PM CEST
//


package org.taktik.icure.be.ehealth.samws.v2.reimbursementlaw.submit;

import org.taktik.icure.be.ehealth.samws.v2.consultation.ConsultLegalTextType;
import org.taktik.icure.be.ehealth.samws.v2.consultation.ConsultRecursiveLegalTextType;
import org.taktik.icure.be.samv2v5.entities.LegalTextFullDataType;

import javax.xml.bind.annotation.*;
import java.io.Serializable;


/**
 * <p>Java class for LegalTextKeyType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="LegalTextKeyType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="key" use="required" type="{urn:be:fgov:ehealth:samws:v2:reimbursementlaw:submit}ReimbursementLawKeyType" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LegalTextKeyType")
@XmlSeeAlso({
    LegalTextFullDataType.class,
    LegalTextType.class,
    ConsultLegalTextType.class,
    ConsultRecursiveLegalTextType.class
})
public class LegalTextKeyType
    implements Serializable
{

    private final static long serialVersionUID = 2L;
    @XmlAttribute(name = "key", required = true)
    protected String key;

    /**
     * Gets the value of the key property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the value of the key property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setKey(String value) {
        this.key = value;
    }

}
