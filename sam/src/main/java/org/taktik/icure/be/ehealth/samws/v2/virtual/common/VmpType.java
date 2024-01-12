/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2019.05.22 at 08:11:32 PM CEST
//


package org.taktik.icure.be.ehealth.samws.v2.virtual.common;

import org.taktik.icure.be.ehealth.samws.v2.core.Text255Type;
import org.taktik.icure.be.ehealth.samws.v2.core.VmpKeyType;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for VmpType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="VmpType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:be:fgov:ehealth:samws:v2:core}VmpKeyType">
 *       &lt;sequence>
 *         &lt;group ref="{urn:be:fgov:ehealth:samws:v2:virtual:common}VmpFields"/>
 *         &lt;group ref="{urn:be:fgov:ehealth:samws:v2:virtual:common}VmpReferences"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "VmpType", propOrder = {
    "name",
    "abbreviation",
    "commentedClassificationCodes",
    "vmpGroupCode",
    "vtmCode",
    "wadaCodes"
})
@XmlSeeAlso({
    AddVmpType.class
})
public class VmpType
    extends VmpKeyType
    implements Serializable
{

    private final static long serialVersionUID = 2L;
    @XmlElement(name = "Name", required = true)
    protected Text255Type name;
    @XmlElement(name = "Abbreviation", required = true)
    protected Text255Type abbreviation;
    @XmlElement(name = "CommentedClassificationCode")
    protected List<String> commentedClassificationCodes;
    @XmlElement(name = "VmpGroupCode")
    protected int vmpGroupCode;
    @XmlElement(name = "VtmCode")
    protected Integer vtmCode;
    @XmlElement(name = "WadaCode")
    protected List<String> wadaCodes;

    /**
     * Gets the value of the name property.
     *
     * @return
     *     possible object is
     *     {@link Text255Type }
     *
     */
    public Text255Type getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value
     *     allowed object is
     *     {@link Text255Type }
     *
     */
    public void setName(Text255Type value) {
        this.name = value;
    }

    /**
     * Gets the value of the abbreviation property.
     *
     * @return
     *     possible object is
     *     {@link Text255Type }
     *
     */
    public Text255Type getAbbreviation() {
        return abbreviation;
    }

    /**
     * Sets the value of the abbreviation property.
     *
     * @param value
     *     allowed object is
     *     {@link Text255Type }
     *
     */
    public void setAbbreviation(Text255Type value) {
        this.abbreviation = value;
    }

    /**
     * Gets the value of the commentedClassificationCodes property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the commentedClassificationCodes property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCommentedClassificationCodes().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     *
     *
     */
    public List<String> getCommentedClassificationCodes() {
        if (commentedClassificationCodes == null) {
            commentedClassificationCodes = new ArrayList<String>();
        }
        return this.commentedClassificationCodes;
    }

    /**
     * Gets the value of the vmpGroupCode property.
     *
     */
    public int getVmpGroupCode() {
        return vmpGroupCode;
    }

    /**
     * Sets the value of the vmpGroupCode property.
     *
     */
    public void setVmpGroupCode(int value) {
        this.vmpGroupCode = value;
    }

    /**
     * Gets the value of the vtmCode property.
     *
     * @return
     *     possible object is
     *     {@link Integer }
     *
     */
    public Integer getVtmCode() {
        return vtmCode;
    }

    /**
     * Sets the value of the vtmCode property.
     *
     * @param value
     *     allowed object is
     *     {@link Integer }
     *
     */
    public void setVtmCode(Integer value) {
        this.vtmCode = value;
    }

    /**
     * Gets the value of the wadaCodes property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the wadaCodes property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getWadaCodes().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     *
     *
     */
    public List<String> getWadaCodes() {
        if (wadaCodes == null) {
            wadaCodes = new ArrayList<String>();
        }
        return this.wadaCodes;
    }

}
