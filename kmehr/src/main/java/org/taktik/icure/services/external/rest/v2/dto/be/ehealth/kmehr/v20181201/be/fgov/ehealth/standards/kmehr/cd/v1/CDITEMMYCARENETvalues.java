/*
 *  iCure Data Stack. Copyright (c) 2020 Taktik SA
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public
 *     License along with this program.  If not, see
 *     <https://www.gnu.org/licenses/>.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2019.06.14 at 03:49:17 PM CEST
//


package org.taktik.icure.services.external.rest.v2.dto.be.ehealth.kmehr.v20181201.be.fgov.ehealth.standards.kmehr.cd.v1;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CD-ITEM-MYCARENETvalues.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="CD-ITEM-MYCARENETvalues">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="fee"/>
 *     &lt;enumeration value="financialcontract"/>
 *     &lt;enumeration value="patientfee"/>
 *     &lt;enumeration value="payment"/>
 *     &lt;enumeration value="reimbursement"/>
 *     &lt;enumeration value="refusal"/>
 *     &lt;enumeration value="patientpaid"/>
 *     &lt;enumeration value="supplement"/>
 *     &lt;enumeration value="paymentreceivingparty"/>
 *     &lt;enumeration value="internship"/>
 *     &lt;enumeration value="documentidentity"/>
 *     &lt;enumeration value="invoicingnumber"/>
 *     &lt;enumeration value="reimbursement-fpssi"/>
 *     &lt;enumeration value="reimbursement-pswc"/>
 *     &lt;enumeration value="umc"/>
 *     &lt;enumeration value="mediprimanumber"/>
 *     &lt;enumeration value="pswc"/>
 *     &lt;enumeration value="treatmentreason"/>
 *     &lt;enumeration value="agreementenddate"/>
 *     &lt;enumeration value="agreementstartdate"/>
 *     &lt;enumeration value="agreementtype"/>
 *     &lt;enumeration value="consultationenddate"/>
 *     &lt;enumeration value="consultationstartdate"/>
 *     &lt;enumeration value="authorisationtype"/>
 *     &lt;enumeration value="decisionreference"/>
 *     &lt;enumeration value="orphandrugdeliveryplace"/>
 *     &lt;enumeration value="refusaljustification"/>
 *     &lt;enumeration value="reststrength"/>
 *     &lt;enumeration value="restunitnumber"/>
 *     &lt;enumeration value="legalbasis"/>
 *     &lt;enumeration value="legalunitnumber"/>
 *     &lt;enumeration value="legalstrength"/>
 *     &lt;enumeration value="istrialperiod"/>
 *     &lt;enumeration value="closurejustification"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 *
 */
@XmlType(name = "CD-ITEM-MYCARENETvalues")
@XmlEnum
public enum CDITEMMYCARENETvalues {

    @XmlEnumValue("fee")
    FEE("fee"),
    @XmlEnumValue("financialcontract")
    FINANCIALCONTRACT("financialcontract"),
    @XmlEnumValue("patientfee")
    PATIENTFEE("patientfee"),
    @XmlEnumValue("payment")
    PAYMENT("payment"),
    @XmlEnumValue("reimbursement")
    REIMBURSEMENT("reimbursement"),
    @XmlEnumValue("refusal")
    REFUSAL("refusal"),
    @XmlEnumValue("patientpaid")
    PATIENTPAID("patientpaid"),
    @XmlEnumValue("supplement")
    SUPPLEMENT("supplement"),
    @XmlEnumValue("paymentreceivingparty")
    PAYMENTRECEIVINGPARTY("paymentreceivingparty"),
    @XmlEnumValue("internship")
    INTERNSHIP("internship"),
    @XmlEnumValue("documentidentity")
    DOCUMENTIDENTITY("documentidentity"),
    @XmlEnumValue("invoicingnumber")
    INVOICINGNUMBER("invoicingnumber"),
    @XmlEnumValue("reimbursement-fpssi")
    REIMBURSEMENT_FPSSI("reimbursement-fpssi"),
    @XmlEnumValue("reimbursement-pswc")
    REIMBURSEMENT_PSWC("reimbursement-pswc"),
    @XmlEnumValue("umc")
    UMC("umc"),
    @XmlEnumValue("mediprimanumber")
    MEDIPRIMANUMBER("mediprimanumber"),
    @XmlEnumValue("pswc")
    PSWC("pswc"),
    @XmlEnumValue("treatmentreason")
    TREATMENTREASON("treatmentreason"),
    @XmlEnumValue("agreementenddate")
    AGREEMENTENDDATE("agreementenddate"),
    @XmlEnumValue("agreementstartdate")
    AGREEMENTSTARTDATE("agreementstartdate"),
    @XmlEnumValue("agreementtype")
    AGREEMENTTYPE("agreementtype"),
    @XmlEnumValue("consultationenddate")
    CONSULTATIONENDDATE("consultationenddate"),
    @XmlEnumValue("consultationstartdate")
    CONSULTATIONSTARTDATE("consultationstartdate"),
    @XmlEnumValue("authorisationtype")
    AUTHORISATIONTYPE("authorisationtype"),
    @XmlEnumValue("decisionreference")
    DECISIONREFERENCE("decisionreference"),
    @XmlEnumValue("orphandrugdeliveryplace")
    ORPHANDRUGDELIVERYPLACE("orphandrugdeliveryplace"),
    @XmlEnumValue("refusaljustification")
    REFUSALJUSTIFICATION("refusaljustification"),
    @XmlEnumValue("reststrength")
    RESTSTRENGTH("reststrength"),
    @XmlEnumValue("restunitnumber")
    RESTUNITNUMBER("restunitnumber"),
    @XmlEnumValue("legalbasis")
    LEGALBASIS("legalbasis"),
    @XmlEnumValue("legalunitnumber")
    LEGALUNITNUMBER("legalunitnumber"),
    @XmlEnumValue("legalstrength")
    LEGALSTRENGTH("legalstrength"),
    @XmlEnumValue("istrialperiod")
    ISTRIALPERIOD("istrialperiod"),
    @XmlEnumValue("closurejustification")
    CLOSUREJUSTIFICATION("closurejustification");
    private final String value;

    CDITEMMYCARENETvalues(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CDITEMMYCARENETvalues fromValue(String v) {
        for (CDITEMMYCARENETvalues c: CDITEMMYCARENETvalues.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
