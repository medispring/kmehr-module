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

import org.taktik.icure.be.ehealth.samws.v2.core.ChangeNoChangeActionType;

import javax.xml.bind.annotation.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for ChangeAmppComponentType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="ChangeAmppComponentType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:be:fgov:ehealth:samws:v2:actual:common}AmppComponentKeyType">
 *       &lt;sequence>
 *         &lt;sequence minOccurs="0">
 *           &lt;group ref="{urn:be:fgov:ehealth:samws:v2:actual:common}AmppComponentFields"/>
 *           &lt;group ref="{urn:be:fgov:ehealth:samws:v2:actual:common}AmppComponentReferences"/>
 *         &lt;/sequence>
 *         &lt;element name="AmppComponentEquivalent" type="{urn:be:fgov:ehealth:samws:v2:actual:common}ChangeAmppComponentEquivalentType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attGroup ref="{urn:be:fgov:ehealth:samws:v2:core}changeNoChangeMetadata"/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ChangeAmppComponentType", propOrder = {
    "contentType",
    "contentMultiplier",
    "packSpecification",
    "deviceTypeCode",
    "packagingClosureCodes",
    "packagingMaterialCodes",
    "packagingTypeCode",
    "amppComponentEquivalents"
})
public class ChangeAmppComponentType
    extends AmppComponentKeyType
    implements Serializable
{

    private final static long serialVersionUID = 2L;
    @XmlElement(name = "ContentType")
    @XmlSchemaType(name = "string")
    protected ContentTypeType contentType;
    @XmlElement(name = "ContentMultiplier")
    protected Integer contentMultiplier;
    @XmlElement(name = "PackSpecification")
    protected String packSpecification;
    @XmlElement(name = "DeviceTypeCode")
    protected String deviceTypeCode;
    @XmlElement(name = "PackagingClosureCode")
    protected List<String> packagingClosureCodes;
    @XmlElement(name = "PackagingMaterialCode")
    protected List<String> packagingMaterialCodes;
    @XmlElement(name = "PackagingTypeCode")
    protected String packagingTypeCode;
    @XmlElement(name = "AmppComponentEquivalent")
    protected List<ChangeAmppComponentEquivalentType> amppComponentEquivalents;
    @XmlAttribute(name = "action", required = true)
    protected ChangeNoChangeActionType action;
    @XmlAttribute(name = "from")
    protected XMLGregorianCalendar from;
    @XmlAttribute(name = "to")
    protected XMLGregorianCalendar to;

    /**
     * Gets the value of the contentType property.
     *
     * @return
     *     possible object is
     *     {@link ContentTypeType }
     *
     */
    public ContentTypeType getContentType() {
        return contentType;
    }

    /**
     * Sets the value of the contentType property.
     *
     * @param value
     *     allowed object is
     *     {@link ContentTypeType }
     *
     */
    public void setContentType(ContentTypeType value) {
        this.contentType = value;
    }

    /**
     * Gets the value of the contentMultiplier property.
     *
     * @return
     *     possible object is
     *     {@link Integer }
     *
     */
    public Integer getContentMultiplier() {
        return contentMultiplier;
    }

    /**
     * Sets the value of the contentMultiplier property.
     *
     * @param value
     *     allowed object is
     *     {@link Integer }
     *
     */
    public void setContentMultiplier(Integer value) {
        this.contentMultiplier = value;
    }

    /**
     * Gets the value of the packSpecification property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getPackSpecification() {
        return packSpecification;
    }

    /**
     * Sets the value of the packSpecification property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setPackSpecification(String value) {
        this.packSpecification = value;
    }

    /**
     * Gets the value of the deviceTypeCode property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getDeviceTypeCode() {
        return deviceTypeCode;
    }

    /**
     * Sets the value of the deviceTypeCode property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setDeviceTypeCode(String value) {
        this.deviceTypeCode = value;
    }

    /**
     * Gets the value of the packagingClosureCodes property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the packagingClosureCodes property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPackagingClosureCodes().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     *
     *
     */
    public List<String> getPackagingClosureCodes() {
        if (packagingClosureCodes == null) {
            packagingClosureCodes = new ArrayList<String>();
        }
        return this.packagingClosureCodes;
    }

    /**
     * Gets the value of the packagingMaterialCodes property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the packagingMaterialCodes property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPackagingMaterialCodes().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     *
     *
     */
    public List<String> getPackagingMaterialCodes() {
        if (packagingMaterialCodes == null) {
            packagingMaterialCodes = new ArrayList<String>();
        }
        return this.packagingMaterialCodes;
    }

    /**
     * Gets the value of the packagingTypeCode property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getPackagingTypeCode() {
        return packagingTypeCode;
    }

    /**
     * Sets the value of the packagingTypeCode property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setPackagingTypeCode(String value) {
        this.packagingTypeCode = value;
    }

    /**
     * Gets the value of the amppComponentEquivalents property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the amppComponentEquivalents property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAmppComponentEquivalents().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ChangeAmppComponentEquivalentType }
     *
     *
     */
    public List<ChangeAmppComponentEquivalentType> getAmppComponentEquivalents() {
        if (amppComponentEquivalents == null) {
            amppComponentEquivalents = new ArrayList<ChangeAmppComponentEquivalentType>();
        }
        return this.amppComponentEquivalents;
    }

    /**
     * Gets the value of the action property.
     *
     * @return
     *     possible object is
     *     {@link ChangeNoChangeActionType }
     *
     */
    public ChangeNoChangeActionType getAction() {
        return action;
    }

    /**
     * Sets the value of the action property.
     *
     * @param value
     *     allowed object is
     *     {@link ChangeNoChangeActionType }
     *
     */
    public void setAction(ChangeNoChangeActionType value) {
        this.action = value;
    }

    /**
     * Gets the value of the from property.
     *
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public XMLGregorianCalendar getFrom() {
        return from;
    }

    /**
     * Sets the value of the from property.
     *
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public void setFrom(XMLGregorianCalendar value) {
        this.from = value;
    }

    /**
     * Gets the value of the to property.
     *
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public XMLGregorianCalendar getTo() {
        return to;
    }

    /**
     * Sets the value of the to property.
     *
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public void setTo(XMLGregorianCalendar value) {
        this.to = value;
    }

}
