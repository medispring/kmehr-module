/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2019.05.22 at 08:11:32 PM CEST
//


package org.taktik.icure.be.ehealth.samws.v2.company.submit;

import org.taktik.icure.be.ehealth.samws.v2.core.StandardResponseType;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the be.fgov.ehealth.samws.v2.company.submit package.
 * <p>An ObjectFactory allows you to programatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups.  Factory methods for each of these are
 * provided in this class.
 *
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _AddCompanyResponse_QNAME = new QName("urn:be:fgov:ehealth:samws:v2:company:submit", "AddCompanyResponse");
    private final static QName _RemoveCompanyResponse_QNAME = new QName("urn:be:fgov:ehealth:samws:v2:company:submit", "RemoveCompanyResponse");
    private final static QName _ChangeCompanyResponse_QNAME = new QName("urn:be:fgov:ehealth:samws:v2:company:submit", "ChangeCompanyResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: be.fgov.ehealth.samws.v2.company.submit
     *
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link AddCompanyRequest }
     *
     */
    public AddCompanyRequest createAddCompanyRequest() {
        return new AddCompanyRequest();
    }

    /**
     * Create an instance of {@link AddCompanyType }
     *
     */
    public AddCompanyType createAddCompanyType() {
        return new AddCompanyType();
    }

    /**
     * Create an instance of {@link ChangeCompanyRequest }
     *
     */
    public ChangeCompanyRequest createChangeCompanyRequest() {
        return new ChangeCompanyRequest();
    }

    /**
     * Create an instance of {@link ChangeCompanyType }
     *
     */
    public ChangeCompanyType createChangeCompanyType() {
        return new ChangeCompanyType();
    }

    /**
     * Create an instance of {@link RemoveCompanyRequest }
     *
     */
    public RemoveCompanyRequest createRemoveCompanyRequest() {
        return new RemoveCompanyRequest();
    }

    /**
     * Create an instance of {@link RemoveCompanyType }
     *
     */
    public RemoveCompanyType createRemoveCompanyType() {
        return new RemoveCompanyType();
    }

    /**
     * Create an instance of {@link VatNrPerCountryType }
     *
     */
    public VatNrPerCountryType createVatNrPerCountryType() {
        return new VatNrPerCountryType();
    }

    /**
     * Create an instance of {@link CompanyKeyType }
     *
     */
    public CompanyKeyType createCompanyKeyType() {
        return new CompanyKeyType();
    }

    /**
     * Create an instance of {@link CompanyType }
     *
     */
    public CompanyType createCompanyType() {
        return new CompanyType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link StandardResponseType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "urn:be:fgov:ehealth:samws:v2:company:submit", name = "AddCompanyResponse")
    public JAXBElement<StandardResponseType> createAddCompanyResponse(StandardResponseType value) {
        return new JAXBElement<StandardResponseType>(_AddCompanyResponse_QNAME, StandardResponseType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link StandardResponseType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "urn:be:fgov:ehealth:samws:v2:company:submit", name = "RemoveCompanyResponse")
    public JAXBElement<StandardResponseType> createRemoveCompanyResponse(StandardResponseType value) {
        return new JAXBElement<StandardResponseType>(_RemoveCompanyResponse_QNAME, StandardResponseType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link StandardResponseType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "urn:be:fgov:ehealth:samws:v2:company:submit", name = "ChangeCompanyResponse")
    public JAXBElement<StandardResponseType> createChangeCompanyResponse(StandardResponseType value) {
        return new JAXBElement<StandardResponseType>(_ChangeCompanyResponse_QNAME, StandardResponseType.class, null, value);
    }

}
