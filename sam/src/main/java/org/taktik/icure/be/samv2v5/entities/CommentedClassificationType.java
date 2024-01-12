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

import javax.xml.bind.annotation.*;


/**
 * <p>Java class for CommentedClassificationType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="CommentedClassificationType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:be:fgov:ehealth:samws:v2:virtual:common}CommentedClassificationKeyType">
 *       &lt;sequence minOccurs="0">
 *         &lt;group ref="{urn:be:fgov:ehealth:samws:v2:virtual:common}CommentedClassificationFields"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CommentedClassificationType", namespace = "urn:be:fgov:ehealth:samws:v2:virtual:common", propOrder = {
    "title",
    "content",
    "posologyNote",
    "url"
})
@XmlSeeAlso({
    ChangeCommentedClassificationType.class,
    AddCommentedClassificationType.class
})
public class CommentedClassificationType
    extends CommentedClassificationKeyType
{

    @XmlElement(name = "Title", namespace = "urn:be:fgov:ehealth:samws:v2:virtual:common")
    protected Text255Type title;
    @XmlElement(name = "Content", namespace = "urn:be:fgov:ehealth:samws:v2:virtual:common")
    protected TextType content;
    @XmlElement(name = "PosologyNote", namespace = "urn:be:fgov:ehealth:samws:v2:virtual:common")
    protected TextType posologyNote;
    @XmlElement(name = "Url", namespace = "urn:be:fgov:ehealth:samws:v2:virtual:common")
    protected Text255Type url;

    /**
     * Gets the value of the title property.
     *
     * @return
     *     possible object is
     *     {@link Text255Type }
     *
     */
    public Text255Type getTitle() {
        return title;
    }

    /**
     * Sets the value of the title property.
     *
     * @param value
     *     allowed object is
     *     {@link Text255Type }
     *
     */
    public void setTitle(Text255Type value) {
        this.title = value;
    }

    /**
     * Gets the value of the content property.
     *
     * @return
     *     possible object is
     *     {@link TextType }
     *
     */
    public TextType getContent() {
        return content;
    }

    /**
     * Sets the value of the content property.
     *
     * @param value
     *     allowed object is
     *     {@link TextType }
     *
     */
    public void setContent(TextType value) {
        this.content = value;
    }

    /**
     * Gets the value of the posologyNote property.
     *
     * @return
     *     possible object is
     *     {@link TextType }
     *
     */
    public TextType getPosologyNote() {
        return posologyNote;
    }

    /**
     * Sets the value of the posologyNote property.
     *
     * @param value
     *     allowed object is
     *     {@link TextType }
     *
     */
    public void setPosologyNote(TextType value) {
        this.posologyNote = value;
    }

    /**
     * Gets the value of the url property.
     *
     * @return
     *     possible object is
     *     {@link Text255Type }
     *
     */
    public Text255Type getUrl() {
        return url;
    }

    /**
     * Sets the value of the url property.
     *
     * @param value
     *     allowed object is
     *     {@link Text255Type }
     *
     */
    public void setUrl(Text255Type value) {
        this.url = value;
    }

}
