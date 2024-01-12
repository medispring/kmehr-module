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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for AddCompoundingIngredientRequestType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="AddCompoundingIngredientRequestType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="CompoundingIngredient" type="{urn:be:fgov:ehealth:samws:v2:compounding:common}AddCompoundingIngredientType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AddCompoundingIngredientRequestType", namespace = "urn:be:fgov:ehealth:samws:v2:compounding:common", propOrder = {
    "compoundingIngredient"
})
public class AddCompoundingIngredientRequestType {

    @XmlElement(name = "CompoundingIngredient", namespace = "urn:be:fgov:ehealth:samws:v2:compounding:common", required = true)
    protected List<AddCompoundingIngredientType> compoundingIngredient;

    /**
     * Gets the value of the compoundingIngredient property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the compoundingIngredient property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCompoundingIngredient().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AddCompoundingIngredientType }
     *
     *
     */
    public List<AddCompoundingIngredientType> getCompoundingIngredient() {
        if (compoundingIngredient == null) {
            compoundingIngredient = new ArrayList<AddCompoundingIngredientType>();
        }
        return this.compoundingIngredient;
    }

}
