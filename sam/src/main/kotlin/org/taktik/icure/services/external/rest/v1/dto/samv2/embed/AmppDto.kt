/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.samv2.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class AmppDto(
	override val from: Long? = null,
	override val to: Long? = null,
	val index: Double? = null,
	val ctiExtended: String? = null,
	val orphan: Boolean = false,
	val leafletLink: SamTextDto? = null,
	val spcLink: SamTextDto? = null,
	val rmaPatientLink: SamTextDto? = null,
	val rmaProfessionalLink: SamTextDto? = null,
	val parallelCircuit: Int? = null,
	val parallelDistributor: String? = null,
	val packMultiplier: Short? = null,
	val packAmount: QuantityDto? = null,
	val packDisplayValue: String? = null,
	val status: AmpStatusDto? = null,
	val atcs: List<AtcDto> = emptyList(),
	val crmLink: SamTextDto? = null,
	val deliveryModusCode: String? = null,
	val deliveryModus: SamTextDto? = null,
	val deliveryModusSpecificationCode: String? = null,
	val deliveryModusSpecification: SamTextDto? = null,
	val dhpcLink: SamTextDto? = null,
	val distributorCompany: CompanyDto? = null,
	val singleUse: Boolean? = null,
	val speciallyRegulated: Int? = null,
	val abbreviatedName: SamTextDto? = null,
	val prescriptionName: SamTextDto? = null,
	val note: SamTextDto? = null,
	val posologyNote: SamTextDto? = null,
	val noGenericPrescriptionReasons: List<SamTextDto>? = emptyList(),
	val exFactoryPrice: Double? = null,
	val reimbursementCode: Int? = null,
	val definedDailyDose: QuantityDto? = null,
	val officialExFactoryPrice: Double? = null,
	val realExFactoryPrice: Double? = null,
	val pricingInformationDecisionDate: Long? = null,
	val components: List<AmppComponentDto?>? = null,
	val commercializations: List<CommercializationDto>? = emptyList(),
	val supplyProblems: List<SupplyProblemDto>? = null,
	val dmpps: List<DmppDto> = emptyList(),
	val vaccineIndicationCodes: List<String>? = emptyList()
) : DataPeriodDto
