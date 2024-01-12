/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2019.06.14 at 03:49:43 PM CEST
//


package org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20190301.be.fgov.ehealth.standards.kmehr.schema.v1;

import java.io.Serializable;
import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20190301.be.fgov.ehealth.standards.kmehr.dt.v1.TextType;


/**
 * frequency of applying the periodic posology, only in case of a constant scheme during a period of time
 *
 * <p>Java class for frequencyType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="frequencyType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice>
 *         &lt;sequence>
 *           &lt;element name="nominator">
 *             &lt;complexType>
 *               &lt;complexContent>
 *                 &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                   &lt;sequence>
 *                     &lt;element name="quantity" type="{http://www.ehealth.fgov.be/standards/kmehr/schema/v1}timequantityType"/>
 *                   &lt;/sequence>
 *                 &lt;/restriction>
 *               &lt;/complexContent>
 *             &lt;/complexType>
 *           &lt;/element>
 *           &lt;element name="denominator">
 *             &lt;complexType>
 *               &lt;complexContent>
 *                 &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                   &lt;sequence>
 *                     &lt;element name="quantity" type="{http://www.ehealth.fgov.be/standards/kmehr/schema/v1}timequantityType"/>
 *                   &lt;/sequence>
 *                 &lt;/restriction>
 *               &lt;/complexContent>
 *             &lt;/complexType>
 *           &lt;/element>
 *         &lt;/sequence>
 *         &lt;sequence>
 *           &lt;element name="decimal" type="{http://www.w3.org/2001/XMLSchema}decimal"/>
 *           &lt;element name="unit" type="{http://www.ehealth.fgov.be/standards/kmehr/schema/v1}unitType"/>
 *         &lt;/sequence>
 *         &lt;element name="text" type="{http://www.ehealth.fgov.be/standards/kmehr/dt/v1}textType"/>
 *         &lt;element name="periodicity" type="{http://www.ehealth.fgov.be/standards/kmehr/schema/v1}periodicityType"/>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "frequencyType", propOrder = {
    "periodicity",
    "text",
    "decimal",
    "unit",
    "nominator",
    "denominator"
})
public class FrequencyType
    implements Serializable
{

    private final static long serialVersionUID = 20190301L;
    protected PeriodicityType periodicity;
    protected TextType text;
    protected BigDecimal decimal;
    protected UnitType unit;
    protected FrequencyType.Nominator nominator;
    protected FrequencyType.Denominator denominator;

    /**
     * Gets the value of the periodicity property.
     *
     * @return
     *     possible object is
     *     {@link PeriodicityType }
     *
     */
    public PeriodicityType getPeriodicity() {
        return periodicity;
    }

    /**
     * Sets the value of the periodicity property.
     *
     * @param value
     *     allowed object is
     *     {@link PeriodicityType }
     *
     */
    public void setPeriodicity(PeriodicityType value) {
        this.periodicity = value;
    }

    /**
     * Gets the value of the text property.
     *
     * @return
     *     possible object is
     *     {@link TextType }
     *
     */
    public TextType getText() {
        return text;
    }

    /**
     * Sets the value of the text property.
     *
     * @param value
     *     allowed object is
     *     {@link TextType }
     *
     */
    public void setText(TextType value) {
        this.text = value;
    }

    /**
     * Gets the value of the decimal property.
     *
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *
     */
    public BigDecimal getDecimal() {
        return decimal;
    }

    /**
     * Sets the value of the decimal property.
     *
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *
     */
    public void setDecimal(BigDecimal value) {
        this.decimal = value;
    }

    /**
     * Gets the value of the unit property.
     *
     * @return
     *     possible object is
     *     {@link UnitType }
     *
     */
    public UnitType getUnit() {
        return unit;
    }

    /**
     * Sets the value of the unit property.
     *
     * @param value
     *     allowed object is
     *     {@link UnitType }
     *
     */
    public void setUnit(UnitType value) {
        this.unit = value;
    }

    /**
     * Gets the value of the nominator property.
     *
     * @return
     *     possible object is
     *     {@link FrequencyType.Nominator }
     *
     */
    public FrequencyType.Nominator getNominator() {
        return nominator;
    }

    /**
     * Sets the value of the nominator property.
     *
     * @param value
     *     allowed object is
     *     {@link FrequencyType.Nominator }
     *
     */
    public void setNominator(FrequencyType.Nominator value) {
        this.nominator = value;
    }

    /**
     * Gets the value of the denominator property.
     *
     * @return
     *     possible object is
     *     {@link FrequencyType.Denominator }
     *
     */
    public FrequencyType.Denominator getDenominator() {
        return denominator;
    }

    /**
     * Sets the value of the denominator property.
     *
     * @param value
     *     allowed object is
     *     {@link FrequencyType.Denominator }
     *
     */
    public void setDenominator(FrequencyType.Denominator value) {
        this.denominator = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     *
     * <p>The following schema fragment specifies the expected content contained within this class.
     *
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="quantity" type="{http://www.ehealth.fgov.be/standards/kmehr/schema/v1}timequantityType"/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     *
     *
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "quantity"
    })
    public static class Denominator
        implements Serializable
    {

        private final static long serialVersionUID = 20190301L;
        @XmlElement(required = true)
        protected TimequantityType quantity;

        /**
         * Gets the value of the quantity property.
         *
         * @return
         *     possible object is
         *     {@link TimequantityType }
         *
         */
        public TimequantityType getQuantity() {
            return quantity;
        }

        /**
         * Sets the value of the quantity property.
         *
         * @param value
         *     allowed object is
         *     {@link TimequantityType }
         *
         */
        public void setQuantity(TimequantityType value) {
            this.quantity = value;
        }

    }


    /**
     * <p>Java class for anonymous complex type.
     *
     * <p>The following schema fragment specifies the expected content contained within this class.
     *
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="quantity" type="{http://www.ehealth.fgov.be/standards/kmehr/schema/v1}timequantityType"/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     *
     *
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "quantity"
    })
    public static class Nominator
        implements Serializable
    {

        private final static long serialVersionUID = 20190301L;
        @XmlElement(required = true)
        protected TimequantityType quantity;

        /**
         * Gets the value of the quantity property.
         *
         * @return
         *     possible object is
         *     {@link TimequantityType }
         *
         */
        public TimequantityType getQuantity() {
            return quantity;
        }

        /**
         * Sets the value of the quantity property.
         *
         * @param value
         *     allowed object is
         *     {@link TimequantityType }
         *
         */
        public void setQuantity(TimequantityType value) {
            this.quantity = value;
        }

    }

}
