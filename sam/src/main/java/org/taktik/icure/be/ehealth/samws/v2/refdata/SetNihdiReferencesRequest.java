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

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for SetNihdiReferencesRequestType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="SetNihdiReferencesRequestType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;group ref="{urn:be:fgov:ehealth:samws:v2:refdata}SetNihdiEntities"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SetNihdiReferencesRequestType", propOrder = {
    "appendixes",
    "formCategories",
    "parameters",
    "reimbursementCriterions"
})
@XmlRootElement(name = "SetNihdiReferencesRequest")
public class SetNihdiReferencesRequest
    implements Serializable
{

    private final static long serialVersionUID = 2L;
    @XmlElement(name = "Appendix")
    protected List<AppendixType> appendixes;
    @XmlElement(name = "FormCategory")
    protected List<FormCategoryType> formCategories;
    @XmlElement(name = "Parameter")
    protected List<ParameterType> parameters;
    @XmlElement(name = "ReimbursementCriterion")
    protected List<ReimbursementCriterionType> reimbursementCriterions;

    /**
     * Gets the value of the appendixes property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the appendixes property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAppendixes().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AppendixType }
     *
     *
     */
    public List<AppendixType> getAppendixes() {
        if (appendixes == null) {
            appendixes = new ArrayList<AppendixType>();
        }
        return this.appendixes;
    }

    /**
     * Gets the value of the formCategories property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the formCategories property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFormCategories().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link FormCategoryType }
     *
     *
     */
    public List<FormCategoryType> getFormCategories() {
        if (formCategories == null) {
            formCategories = new ArrayList<FormCategoryType>();
        }
        return this.formCategories;
    }

    /**
     * Gets the value of the parameters property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the parameters property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getParameters().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ParameterType }
     *
     *
     */
    public List<ParameterType> getParameters() {
        if (parameters == null) {
            parameters = new ArrayList<ParameterType>();
        }
        return this.parameters;
    }

    /**
     * Gets the value of the reimbursementCriterions property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the reimbursementCriterions property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getReimbursementCriterions().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ReimbursementCriterionType }
     *
     *
     */
    public List<ReimbursementCriterionType> getReimbursementCriterions() {
        if (reimbursementCriterions == null) {
            reimbursementCriterions = new ArrayList<ReimbursementCriterionType>();
        }
        return this.reimbursementCriterions;
    }

}
