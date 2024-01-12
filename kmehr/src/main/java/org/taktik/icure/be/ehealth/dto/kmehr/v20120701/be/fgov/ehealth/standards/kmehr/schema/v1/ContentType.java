/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2019.06.14 at 03:48:48 PM CEST
//


package org.taktik.icure.be.ehealth.dto.kmehr.v20120701.be.fgov.ehealth.standards.kmehr.schema.v1;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;
import org.taktik.icure.be.ehealth.dto.kmehr.v20120701.be.fgov.ehealth.standards.kmehr.cd.v1.CDCONTENT;
import org.taktik.icure.be.ehealth.dto.kmehr.v20120701.be.fgov.ehealth.standards.kmehr.cd.v1.CDDRUGCNK;
import org.taktik.icure.be.ehealth.dto.kmehr.v20120701.be.fgov.ehealth.standards.kmehr.cd.v1.CDINNCLUSTER;
import org.taktik.icure.be.ehealth.dto.kmehr.v20120701.be.fgov.ehealth.standards.kmehr.cd.v1.LnkType;
import org.taktik.icure.be.ehealth.dto.kmehr.v20120701.be.fgov.ehealth.standards.kmehr.dt.v1.TextType;
import org.taktik.icure.be.ehealth.dto.kmehr.v20120701.be.fgov.ehealth.standards.kmehr.id.v1.IDKMEHR;


/**
 * to specify the value of the item
 *
 * <p>Java class for contentType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="contentType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;choice>
 *           &lt;element name="id" type="{http://www.ehealth.fgov.be/standards/kmehr/id/v1}ID-KMEHR" maxOccurs="unbounded"/>
 *           &lt;element name="cd" type="{http://www.ehealth.fgov.be/standards/kmehr/cd/v1}CD-CONTENT" maxOccurs="unbounded"/>
 *           &lt;element name="decimal" type="{http://www.w3.org/2001/XMLSchema}decimal"/>
 *           &lt;element name="unsignedInt" type="{http://www.w3.org/2001/XMLSchema}unsignedInt"/>
 *           &lt;element name="boolean" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *           &lt;element name="text" type="{http://www.ehealth.fgov.be/standards/kmehr/dt/v1}textType" maxOccurs="unbounded"/>
 *           &lt;choice>
 *             &lt;choice>
 *               &lt;element name="year" type="{http://www.w3.org/2001/XMLSchema}gYear"/>
 *               &lt;element name="yearmonth" type="{http://www.w3.org/2001/XMLSchema}gYearMonth"/>
 *             &lt;/choice>
 *             &lt;sequence>
 *               &lt;element name="date" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/>
 *               &lt;element name="time" type="{http://www.w3.org/2001/XMLSchema}time" minOccurs="0"/>
 *             &lt;/sequence>
 *           &lt;/choice>
 *           &lt;element name="hcparty" type="{http://www.ehealth.fgov.be/standards/kmehr/schema/v1}hcpartyType"/>
 *           &lt;element name="person" type="{http://www.ehealth.fgov.be/standards/kmehr/schema/v1}personType"/>
 *           &lt;element name="insurance" type="{http://www.ehealth.fgov.be/standards/kmehr/schema/v1}insuranceType"/>
 *           &lt;element name="incapacity" type="{http://www.ehealth.fgov.be/standards/kmehr/schema/v1}incapacityType"/>
 *           &lt;element name="error" type="{http://www.ehealth.fgov.be/standards/kmehr/schema/v1}errorType"/>
 *           &lt;choice>
 *             &lt;sequence>
 *               &lt;choice>
 *                 &lt;element name="medicinalproduct">
 *                   &lt;complexType>
 *                     &lt;complexContent>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                         &lt;sequence>
 *                           &lt;element name="intendedcd" type="{http://www.ehealth.fgov.be/standards/kmehr/cd/v1}CD-DRUG-CNK"/>
 *                           &lt;element name="deliveredcd" type="{http://www.ehealth.fgov.be/standards/kmehr/cd/v1}CD-DRUG-CNK" minOccurs="0"/>
 *                           &lt;element name="intendedname" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                           &lt;element name="deliveredname" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/>
 *                         &lt;/sequence>
 *                       &lt;/restriction>
 *                     &lt;/complexContent>
 *                   &lt;/complexType>
 *                 &lt;/element>
 *                 &lt;element name="substanceproduct">
 *                   &lt;complexType>
 *                     &lt;complexContent>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                         &lt;sequence>
 *                           &lt;element name="intendedcd" type="{http://www.ehealth.fgov.be/standards/kmehr/cd/v1}CD-INNCLUSTER"/>
 *                           &lt;element name="deliveredcd" type="{http://www.ehealth.fgov.be/standards/kmehr/cd/v1}CD-DRUG-CNK" minOccurs="0"/>
 *                           &lt;element name="intendedname" type="{http://www.w3.org/2001/XMLSchema}anyType"/>
 *                           &lt;element name="deliveredname" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/>
 *                         &lt;/sequence>
 *                       &lt;/restriction>
 *                     &lt;/complexContent>
 *                   &lt;/complexType>
 *                 &lt;/element>
 *                 &lt;element name="compoundprescription" type="{http://www.ehealth.fgov.be/standards/kmehr/dt/v1}textType"/>
 *               &lt;/choice>
 *             &lt;/sequence>
 *             &lt;element name="medication" type="{http://www.ehealth.fgov.be/standards/kmehr/schema/v1}medicationType"/>
 *           &lt;/choice>
 *           &lt;element name="holter" type="{http://www.ehealth.fgov.be/standards/kmehr/schema/v1}holterType"/>
 *           &lt;element name="ecg" type="{http://www.ehealth.fgov.be/standards/kmehr/dt/v1}textType"/>
 *           &lt;element name="bacteriology" type="{http://www.ehealth.fgov.be/standards/kmehr/dt/v1}textType"/>
 *           &lt;element name="lnk" type="{http://www.ehealth.fgov.be/standards/kmehr/cd/v1}lnkType" maxOccurs="unbounded"/>
 *           &lt;element name="location" type="{http://www.ehealth.fgov.be/standards/kmehr/schema/v1}locationBirthPlaceType"/>
 *         &lt;/choice>
 *         &lt;element name="unit" type="{http://www.ehealth.fgov.be/standards/kmehr/schema/v1}unitType" minOccurs="0"/>
 *         &lt;element name="minref" type="{http://www.ehealth.fgov.be/standards/kmehr/schema/v1}minrefType" minOccurs="0"/>
 *         &lt;element name="maxref" type="{http://www.ehealth.fgov.be/standards/kmehr/schema/v1}maxrefType" minOccurs="0"/>
 *         &lt;element name="refscope" type="{http://www.ehealth.fgov.be/standards/kmehr/schema/v1}refscopeType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "contentType", propOrder = {
    "location",
    "lnks",
    "bacteriology",
    "ecg",
    "holter",
    "medication",
    "compoundprescription",
    "substanceproduct",
    "medicinalproduct",
    "error",
    "incapacity",
    "insurance",
    "person",
    "hcparty",
    "date",
    "time",
    "yearmonth",
    "year",
    "texts",
    "_boolean",
    "unsignedInt",
    "decimal",
    "cds",
    "ids",
    "unit",
    "minref",
    "maxref",
    "refscopes"
})
public class ContentType
    implements Serializable
{

    private final static long serialVersionUID = 20120701L;
    protected LocationBirthPlaceType location;
    @XmlElement(name = "lnk")
    protected List<LnkType> lnks;
    protected TextType bacteriology;
    protected TextType ecg;
    protected HolterType holter;
    protected MedicationType medication;
    protected TextType compoundprescription;
    protected ContentType.Substanceproduct substanceproduct;
    protected ContentType.Medicinalproduct medicinalproduct;
    protected ErrorType error;
    protected IncapacityType incapacity;
    protected InsuranceType insurance;
    protected PersonType person;
    protected HcpartyType hcparty;
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar date;
    @XmlSchemaType(name = "time")
    protected XMLGregorianCalendar time;
    @XmlSchemaType(name = "gYearMonth")
    protected XMLGregorianCalendar yearmonth;
    @XmlSchemaType(name = "gYear")
    protected XMLGregorianCalendar year;
    @XmlElement(name = "text")
    protected List<TextType> texts;
    @XmlElement(name = "boolean")
    protected Boolean _boolean;
    @XmlSchemaType(name = "unsignedInt")
    protected Long unsignedInt;
    protected BigDecimal decimal;
    @XmlElement(name = "cd")
    protected List<CDCONTENT> cds;
    @XmlElement(name = "id")
    protected List<IDKMEHR> ids;
    protected UnitType unit;
    protected MinrefType minref;
    protected MaxrefType maxref;
    @XmlElement(name = "refscope")
    protected List<RefscopeType> refscopes;

    /**
     * Gets the value of the location property.
     *
     * @return
     *     possible object is
     *     {@link LocationBirthPlaceType }
     *
     */
    public LocationBirthPlaceType getLocation() {
        return location;
    }

    /**
     * Sets the value of the location property.
     *
     * @param value
     *     allowed object is
     *     {@link LocationBirthPlaceType }
     *
     */
    public void setLocation(LocationBirthPlaceType value) {
        this.location = value;
    }

    /**
     * Gets the value of the lnks property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the lnks property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getLnks().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link LnkType }
     *
     *
     */
    public List<LnkType> getLnks() {
        if (lnks == null) {
            lnks = new ArrayList<LnkType>();
        }
        return this.lnks;
    }

    /**
     * Gets the value of the bacteriology property.
     *
     * @return
     *     possible object is
     *     {@link TextType }
     *
     */
    public TextType getBacteriology() {
        return bacteriology;
    }

    /**
     * Sets the value of the bacteriology property.
     *
     * @param value
     *     allowed object is
     *     {@link TextType }
     *
     */
    public void setBacteriology(TextType value) {
        this.bacteriology = value;
    }

    /**
     * Gets the value of the ecg property.
     *
     * @return
     *     possible object is
     *     {@link TextType }
     *
     */
    public TextType getEcg() {
        return ecg;
    }

    /**
     * Sets the value of the ecg property.
     *
     * @param value
     *     allowed object is
     *     {@link TextType }
     *
     */
    public void setEcg(TextType value) {
        this.ecg = value;
    }

    /**
     * Gets the value of the holter property.
     *
     * @return
     *     possible object is
     *     {@link HolterType }
     *
     */
    public HolterType getHolter() {
        return holter;
    }

    /**
     * Sets the value of the holter property.
     *
     * @param value
     *     allowed object is
     *     {@link HolterType }
     *
     */
    public void setHolter(HolterType value) {
        this.holter = value;
    }

    /**
     * Gets the value of the medication property.
     *
     * @return
     *     possible object is
     *     {@link MedicationType }
     *
     */
    public MedicationType getMedication() {
        return medication;
    }

    /**
     * Sets the value of the medication property.
     *
     * @param value
     *     allowed object is
     *     {@link MedicationType }
     *
     */
    public void setMedication(MedicationType value) {
        this.medication = value;
    }

    /**
     * Gets the value of the compoundprescription property.
     *
     * @return
     *     possible object is
     *     {@link TextType }
     *
     */
    public TextType getCompoundprescription() {
        return compoundprescription;
    }

    /**
     * Sets the value of the compoundprescription property.
     *
     * @param value
     *     allowed object is
     *     {@link TextType }
     *
     */
    public void setCompoundprescription(TextType value) {
        this.compoundprescription = value;
    }

    /**
     * Gets the value of the substanceproduct property.
     *
     * @return
     *     possible object is
     *     {@link ContentType.Substanceproduct }
     *
     */
    public ContentType.Substanceproduct getSubstanceproduct() {
        return substanceproduct;
    }

    /**
     * Sets the value of the substanceproduct property.
     *
     * @param value
     *     allowed object is
     *     {@link ContentType.Substanceproduct }
     *
     */
    public void setSubstanceproduct(ContentType.Substanceproduct value) {
        this.substanceproduct = value;
    }

    /**
     * Gets the value of the medicinalproduct property.
     *
     * @return
     *     possible object is
     *     {@link ContentType.Medicinalproduct }
     *
     */
    public ContentType.Medicinalproduct getMedicinalproduct() {
        return medicinalproduct;
    }

    /**
     * Sets the value of the medicinalproduct property.
     *
     * @param value
     *     allowed object is
     *     {@link ContentType.Medicinalproduct }
     *
     */
    public void setMedicinalproduct(ContentType.Medicinalproduct value) {
        this.medicinalproduct = value;
    }

    /**
     * Gets the value of the error property.
     *
     * @return
     *     possible object is
     *     {@link ErrorType }
     *
     */
    public ErrorType getError() {
        return error;
    }

    /**
     * Sets the value of the error property.
     *
     * @param value
     *     allowed object is
     *     {@link ErrorType }
     *
     */
    public void setError(ErrorType value) {
        this.error = value;
    }

    /**
     * Gets the value of the incapacity property.
     *
     * @return
     *     possible object is
     *     {@link IncapacityType }
     *
     */
    public IncapacityType getIncapacity() {
        return incapacity;
    }

    /**
     * Sets the value of the incapacity property.
     *
     * @param value
     *     allowed object is
     *     {@link IncapacityType }
     *
     */
    public void setIncapacity(IncapacityType value) {
        this.incapacity = value;
    }

    /**
     * Gets the value of the insurance property.
     *
     * @return
     *     possible object is
     *     {@link InsuranceType }
     *
     */
    public InsuranceType getInsurance() {
        return insurance;
    }

    /**
     * Sets the value of the insurance property.
     *
     * @param value
     *     allowed object is
     *     {@link InsuranceType }
     *
     */
    public void setInsurance(InsuranceType value) {
        this.insurance = value;
    }

    /**
     * Gets the value of the person property.
     *
     * @return
     *     possible object is
     *     {@link PersonType }
     *
     */
    public PersonType getPerson() {
        return person;
    }

    /**
     * Sets the value of the person property.
     *
     * @param value
     *     allowed object is
     *     {@link PersonType }
     *
     */
    public void setPerson(PersonType value) {
        this.person = value;
    }

    /**
     * Gets the value of the hcparty property.
     *
     * @return
     *     possible object is
     *     {@link HcpartyType }
     *
     */
    public HcpartyType getHcparty() {
        return hcparty;
    }

    /**
     * Sets the value of the hcparty property.
     *
     * @param value
     *     allowed object is
     *     {@link HcpartyType }
     *
     */
    public void setHcparty(HcpartyType value) {
        this.hcparty = value;
    }

    /**
     * Gets the value of the date property.
     *
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public XMLGregorianCalendar getDate() {
        return date;
    }

    /**
     * Sets the value of the date property.
     *
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public void setDate(XMLGregorianCalendar value) {
        this.date = value;
    }

    /**
     * Gets the value of the time property.
     *
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public XMLGregorianCalendar getTime() {
        return time;
    }

    /**
     * Sets the value of the time property.
     *
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public void setTime(XMLGregorianCalendar value) {
        this.time = value;
    }

    /**
     * Gets the value of the yearmonth property.
     *
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public XMLGregorianCalendar getYearmonth() {
        return yearmonth;
    }

    /**
     * Sets the value of the yearmonth property.
     *
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public void setYearmonth(XMLGregorianCalendar value) {
        this.yearmonth = value;
    }

    /**
     * Gets the value of the year property.
     *
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public XMLGregorianCalendar getYear() {
        return year;
    }

    /**
     * Sets the value of the year property.
     *
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public void setYear(XMLGregorianCalendar value) {
        this.year = value;
    }

    /**
     * Gets the value of the texts property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the texts property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTexts().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TextType }
     *
     *
     */
    public List<TextType> getTexts() {
        if (texts == null) {
            texts = new ArrayList<TextType>();
        }
        return this.texts;
    }

    /**
     * Gets the value of the boolean property.
     *
     * @return
     *     possible object is
     *     {@link Boolean }
     *
     */
    public Boolean isBoolean() {
        return _boolean;
    }

    /**
     * Sets the value of the boolean property.
     *
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *
     */
    public void setBoolean(Boolean value) {
        this._boolean = value;
    }

    /**
     * Gets the value of the unsignedInt property.
     *
     * @return
     *     possible object is
     *     {@link Long }
     *
     */
    public Long getUnsignedInt() {
        return unsignedInt;
    }

    /**
     * Sets the value of the unsignedInt property.
     *
     * @param value
     *     allowed object is
     *     {@link Long }
     *
     */
    public void setUnsignedInt(Long value) {
        this.unsignedInt = value;
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
     * {@link CDCONTENT }
     *
     *
     */
    public List<CDCONTENT> getCds() {
        if (cds == null) {
            cds = new ArrayList<CDCONTENT>();
        }
        return this.cds;
    }

    /**
     * Gets the value of the ids property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the ids property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getIds().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link IDKMEHR }
     *
     *
     */
    public List<IDKMEHR> getIds() {
        if (ids == null) {
            ids = new ArrayList<IDKMEHR>();
        }
        return this.ids;
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
     * Gets the value of the minref property.
     *
     * @return
     *     possible object is
     *     {@link MinrefType }
     *
     */
    public MinrefType getMinref() {
        return minref;
    }

    /**
     * Sets the value of the minref property.
     *
     * @param value
     *     allowed object is
     *     {@link MinrefType }
     *
     */
    public void setMinref(MinrefType value) {
        this.minref = value;
    }

    /**
     * Gets the value of the maxref property.
     *
     * @return
     *     possible object is
     *     {@link MaxrefType }
     *
     */
    public MaxrefType getMaxref() {
        return maxref;
    }

    /**
     * Sets the value of the maxref property.
     *
     * @param value
     *     allowed object is
     *     {@link MaxrefType }
     *
     */
    public void setMaxref(MaxrefType value) {
        this.maxref = value;
    }

    /**
     * Gets the value of the refscopes property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the refscopes property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRefscopes().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link RefscopeType }
     *
     *
     */
    public List<RefscopeType> getRefscopes() {
        if (refscopes == null) {
            refscopes = new ArrayList<RefscopeType>();
        }
        return this.refscopes;
    }


    /**
     * a medicinal product can be identified unambiguously by a CNK code identifying a package. The descriptive identification is only mandatory in case of absence of a package ID.
     *
     * <p>Java class for anonymous complex type.
     *
     * <p>The following schema fragment specifies the expected content contained within this class.
     *
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="intendedcd" type="{http://www.ehealth.fgov.be/standards/kmehr/cd/v1}CD-DRUG-CNK"/>
     *         &lt;element name="deliveredcd" type="{http://www.ehealth.fgov.be/standards/kmehr/cd/v1}CD-DRUG-CNK" minOccurs="0"/>
     *         &lt;element name="intendedname" type="{http://www.w3.org/2001/XMLSchema}string"/>
     *         &lt;element name="deliveredname" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/>
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
        "intendedcd",
        "deliveredcd",
        "intendedname",
        "deliveredname"
    })
    public static class Medicinalproduct
        implements Serializable
    {

        private final static long serialVersionUID = 20120701L;
        @XmlElement(required = true)
        protected CDDRUGCNK intendedcd;
        protected CDDRUGCNK deliveredcd;
        @XmlElement(required = true)
        protected String intendedname;
        protected Object deliveredname;

        /**
         * Gets the value of the intendedcd property.
         *
         * @return
         *     possible object is
         *     {@link CDDRUGCNK }
         *
         */
        public CDDRUGCNK getIntendedcd() {
            return intendedcd;
        }

        /**
         * Sets the value of the intendedcd property.
         *
         * @param value
         *     allowed object is
         *     {@link CDDRUGCNK }
         *
         */
        public void setIntendedcd(CDDRUGCNK value) {
            this.intendedcd = value;
        }

        /**
         * Gets the value of the deliveredcd property.
         *
         * @return
         *     possible object is
         *     {@link CDDRUGCNK }
         *
         */
        public CDDRUGCNK getDeliveredcd() {
            return deliveredcd;
        }

        /**
         * Sets the value of the deliveredcd property.
         *
         * @param value
         *     allowed object is
         *     {@link CDDRUGCNK }
         *
         */
        public void setDeliveredcd(CDDRUGCNK value) {
            this.deliveredcd = value;
        }

        /**
         * Gets the value of the intendedname property.
         *
         * @return
         *     possible object is
         *     {@link String }
         *
         */
        public String getIntendedname() {
            return intendedname;
        }

        /**
         * Sets the value of the intendedname property.
         *
         * @param value
         *     allowed object is
         *     {@link String }
         *
         */
        public void setIntendedname(String value) {
            this.intendedname = value;
        }

        /**
         * Gets the value of the deliveredname property.
         *
         * @return
         *     possible object is
         *     {@link Object }
         *
         */
        public Object getDeliveredname() {
            return deliveredname;
        }

        /**
         * Sets the value of the deliveredname property.
         *
         * @param value
         *     allowed object is
         *     {@link Object }
         *
         */
        public void setDeliveredname(Object value) {
            this.deliveredname = value;
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
     *         &lt;element name="intendedcd" type="{http://www.ehealth.fgov.be/standards/kmehr/cd/v1}CD-INNCLUSTER"/>
     *         &lt;element name="deliveredcd" type="{http://www.ehealth.fgov.be/standards/kmehr/cd/v1}CD-DRUG-CNK" minOccurs="0"/>
     *         &lt;element name="intendedname" type="{http://www.w3.org/2001/XMLSchema}anyType"/>
     *         &lt;element name="deliveredname" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/>
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
        "intendedcd",
        "deliveredcd",
        "intendedname",
        "deliveredname"
    })
    public static class Substanceproduct
        implements Serializable
    {

        private final static long serialVersionUID = 20120701L;
        @XmlElement(required = true)
        protected CDINNCLUSTER intendedcd;
        protected CDDRUGCNK deliveredcd;
        @XmlElement(required = true)
        protected Object intendedname;
        protected Object deliveredname;

        /**
         * Gets the value of the intendedcd property.
         *
         * @return
         *     possible object is
         *     {@link CDINNCLUSTER }
         *
         */
        public CDINNCLUSTER getIntendedcd() {
            return intendedcd;
        }

        /**
         * Sets the value of the intendedcd property.
         *
         * @param value
         *     allowed object is
         *     {@link CDINNCLUSTER }
         *
         */
        public void setIntendedcd(CDINNCLUSTER value) {
            this.intendedcd = value;
        }

        /**
         * Gets the value of the deliveredcd property.
         *
         * @return
         *     possible object is
         *     {@link CDDRUGCNK }
         *
         */
        public CDDRUGCNK getDeliveredcd() {
            return deliveredcd;
        }

        /**
         * Sets the value of the deliveredcd property.
         *
         * @param value
         *     allowed object is
         *     {@link CDDRUGCNK }
         *
         */
        public void setDeliveredcd(CDDRUGCNK value) {
            this.deliveredcd = value;
        }

        /**
         * Gets the value of the intendedname property.
         *
         * @return
         *     possible object is
         *     {@link Object }
         *
         */
        public Object getIntendedname() {
            return intendedname;
        }

        /**
         * Sets the value of the intendedname property.
         *
         * @param value
         *     allowed object is
         *     {@link Object }
         *
         */
        public void setIntendedname(Object value) {
            this.intendedname = value;
        }

        /**
         * Gets the value of the deliveredname property.
         *
         * @return
         *     possible object is
         *     {@link Object }
         *
         */
        public Object getDeliveredname() {
            return deliveredname;
        }

        /**
         * Sets the value of the deliveredname property.
         *
         * @param value
         *     allowed object is
         *     {@link Object }
         *
         */
        public void setDeliveredname(Object value) {
            this.deliveredname = value;
        }

    }

}
