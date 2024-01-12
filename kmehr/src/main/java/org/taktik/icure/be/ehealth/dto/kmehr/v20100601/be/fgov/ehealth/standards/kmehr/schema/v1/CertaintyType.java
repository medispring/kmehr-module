/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2019.06.14 at 03:49:49 PM CEST
//


package org.taktik.icure.be.ehealth.dto.kmehr.v20100601.be.fgov.ehealth.standards.kmehr.schema.v1;

import org.taktik.icure.be.ehealth.dto.kmehr.v20100601.be.fgov.ehealth.standards.kmehr.cd.v1.CDCERTAINTY;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;


/**
 * <p>Java class for certaintyType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="certaintyType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="cd" type="{http://www.ehealth.fgov.be/standards/kmehr/cd/v1}CD-CERTAINTY"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "certaintyType", propOrder = {
        "cd"
})
public class CertaintyType
        implements Serializable {

    private final static long serialVersionUID = 20100601L;
    @XmlElement(required = true)
    protected CDCERTAINTY cd;

    /**
     * Gets the value of the cd property.
     *
     * @return possible object is
     * {@link CDCERTAINTY }
     */
    public CDCERTAINTY getCd() {
        return cd;
    }

    /**
     * Sets the value of the cd property.
     *
     * @param value allowed object is
     *              {@link CDCERTAINTY }
     */
    public void setCd(CDCERTAINTY value) {
        this.cd = value;
    }

}
