/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2019.06.14 at 03:49:21 PM CEST
//


package org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20150301.be.fgov.ehealth.standards.kmehr.schema.v1;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20150301.be.fgov.ehealth.standards.kmehr.cd.v1.CDFORMULARY;


/**
 * <p>Java class for formularyreferenceType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="formularyreferenceType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;choice>
 *           &lt;element name="cd" type="{http://www.ehealth.fgov.be/standards/kmehr/cd/v1}CD-FORMULARY" maxOccurs="unbounded"/>
 *           &lt;element name="formularyname" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;/choice>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "formularyreferenceType", propOrder = {
    "formularyname",
    "cds"
})
public class FormularyreferenceType
    implements Serializable
{

    private final static long serialVersionUID = 20150301L;
    protected String formularyname;
    @XmlElement(name = "cd")
    protected List<CDFORMULARY> cds;

    /**
     * Gets the value of the formularyname property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getFormularyname() {
        return formularyname;
    }

    /**
     * Sets the value of the formularyname property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setFormularyname(String value) {
        this.formularyname = value;
    }

    /**
     * Gets the value of the cds property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the cds property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCds().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CDFORMULARY }
     *
     *
     */
    public List<CDFORMULARY> getCds() {
        if (cds == null) {
            cds = new ArrayList<CDFORMULARY>();
        }
        return this.cds;
    }

}
