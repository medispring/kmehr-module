/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2019.05.22 at 08:11:32 PM CEST
//


package org.taktik.icure.be.ehealth.samws.v2.core;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;


/**
 * <p>Java class for TextType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="TextType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Fr" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="Nl" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="De" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="En" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TextType", propOrder = {
    "fr",
    "nl",
    "de",
    "en"
})
public class TextType
    implements Serializable
{

    private final static long serialVersionUID = 2L;
    @XmlElement(name = "Fr", required = true)
    protected String fr;
    @XmlElement(name = "Nl", required = true)
    protected String nl;
    @XmlElement(name = "De")
    protected String de;
    @XmlElement(name = "En")
    protected String en;

    /**
     * Gets the value of the fr property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getFr() {
        return fr;
    }

    /**
     * Sets the value of the fr property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setFr(String value) {
        this.fr = value;
    }

    /**
     * Gets the value of the nl property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getNl() {
        return nl;
    }

    /**
     * Sets the value of the nl property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setNl(String value) {
        this.nl = value;
    }

    /**
     * Gets the value of the de property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getDe() {
        return de;
    }

    /**
     * Sets the value of the de property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setDe(String value) {
        this.de = value;
    }

    /**
     * Gets the value of the en property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getEn() {
        return en;
    }

    /**
     * Sets the value of the en property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setEn(String value) {
        this.en = value;
    }

}
