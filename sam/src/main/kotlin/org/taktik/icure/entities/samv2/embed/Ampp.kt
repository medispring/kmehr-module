/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.samv2.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Ampp(
	override val from: Long? = null,
	override val to: Long? = null,
	var index: Double? = null,
	val ctiExtended: String? = null,
	val orphan: Boolean = false,
	val leafletLink: SamText? = null,
	val spcLink: SamText? = null,
	val rmaPatientLink: SamText? = null,
	val rmaProfessionalLink: SamText? = null,
	val parallelCircuit: Int? = null,
	val parallelDistributor: String? = null,
	val packMultiplier: Short? = null,
	val packAmount: Quantity? = null,
	val packDisplayValue: String? = null,
	val status: AmpStatus? = null,
	val atcs: Set<Atc> = emptySet(),
	val crmLink: SamText? = null,
	val deliveryModusCode: String? = null,
	val deliveryModus: SamText? = null,
	val deliveryModusSpecificationCode: String? = null,
	val deliveryModusSpecification: SamText? = null,
	var dhpcLink: SamText? = null,
	val distributorCompany: Company? = null,
	val singleUse: Boolean? = null,
	val speciallyRegulated: Int? = null,
	val abbreviatedName: SamText? = null,
	val prescriptionName: SamText? = null,
	val note: SamText? = null,
	val posologyNote: SamText? = null,
	val noGenericPrescriptionReasons: Set<SamText>? = emptySet(),
	val exFactoryPrice: Double? = null,
	val reimbursementCode: Int? = null,
	val definedDailyDose: Quantity? = null,
	val officialExFactoryPrice: Double? = null,
	val realExFactoryPrice: Double? = null,
	val pricingInformationDecisionDate: Long? = null,
	val components: Set<AmppComponent?>? = null,
	val commercializations: Set<Commercialization>? = null,
	var supplyProblems: Set<SupplyProblem>? = null,
	val dmpps: Set<Dmpp> = emptySet(),
	val vaccineIndicationCodes: Set<String>? = null
) : DataPeriod
