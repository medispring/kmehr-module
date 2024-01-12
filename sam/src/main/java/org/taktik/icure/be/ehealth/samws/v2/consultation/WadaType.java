/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2019.05.22 at 08:11:32 PM CEST
//


package org.taktik.icure.be.ehealth.samws.v2.consultation;

import org.taktik.icure.be.ehealth.samws.v2.refdata.WadaKeyType;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;


/**
 * <p>Java class for WadaType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="WadaType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:be:fgov:ehealth:samws:v2:refdata}WadaKeyType">
 *       &lt;sequence>
 *         &lt;element name="Name" type="{urn:be:fgov:ehealth:samws:v2:consultation}ConsultTextType"/>
 *         &lt;element name="Description" type="{urn:be:fgov:ehealth:samws:v2:consultation}ConsultTextType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WadaType", propOrder = {
    "name",
    "description"
})
public class WadaType
    extends WadaKeyType
    implements Serializable
{

    private final static long serialVersionUID = 2L;
    @XmlElement(name = "Name", required = true)
    protected ConsultTextType name;
    @XmlElement(name = "Description")
    protected ConsultTextType description;

    /**
     * Gets the value of the name property.
     *
     * @return
     *     possible object is
     *     {@link ConsultTextType }
     *
     */
    public ConsultTextType getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value
     *     allowed object is
     *     {@link ConsultTextType }
     *
     */
    public void setName(ConsultTextType value) {
        this.name = value;
    }

    /**
     * Gets the value of the description property.
     *
     * @return
     *     possible object is
     *     {@link ConsultTextType }
     *
     */
    public ConsultTextType getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     *
     * @param value
     *     allowed object is
     *     {@link ConsultTextType }
     *
     */
    public void setDescription(ConsultTextType value) {
        this.description = value;
    }

}
