package org.taktik.icure.utils

import org.taktik.icure.entities.samv2.Amp
import org.taktik.icure.entities.samv2.embed.Company
import org.taktik.icure.entities.samv2.embed.SamText
import org.taktik.icure.entities.samv2.stub.VmpGroupStub
import org.taktik.icure.entities.samv2.stub.VmpStub

fun Amp?.explainDifferences(other: Amp?): List<Diff> {
	if (this == null || other == null) return listOf()

	val diffs = mutableListOf<Diff>()
	if (from != other.from) diffs.add(Diff("Amp.from"))
	if (to != other.to) diffs.add(Diff("Amp.to"))
	if (code != other.code) diffs.add(Diff("Amp.code"))
	if (vmp != other.vmp) diffs.add(Diff("Amp.vmp", vmp.explainDifferences(other.vmp)))
	if (officialName != other.officialName) diffs.add(Diff("Amp.officialName"))
	if (status != other.status) diffs.add(Diff("Amp.status"))
	if (name != other.name) diffs.add(Diff("Amp.name", name.explainDifferences(other.name)))
	if (blackTriangle != other.blackTriangle) diffs.add(Diff("Amp.blackTriangle"))
	if (medicineType != other.medicineType) diffs.add(Diff("Amp.medicineType"))
	if (company != other.company) diffs.add(Diff("Amp.company", company.explainDifferences(other.company)))
	if (abbreviatedName != other.abbreviatedName) diffs.add(Diff("Amp.abbreviatedName", abbreviatedName.explainDifferences(other.abbreviatedName)))
	if (proprietarySuffix != other.proprietarySuffix) diffs.add(Diff("Amp.proprietarySuffix", proprietarySuffix.explainDifferences(other.proprietarySuffix)))
	if (prescriptionName != other.prescriptionName) diffs.add(Diff("Amp.prescriptionName", prescriptionName.explainDifferences(other.prescriptionName)))
	if (ampps != other.ampps) diffs.add(Diff("Amp.ampps", ampps.differences(other.ampps)))
	if (components != other.components) diffs.add(Diff("Amp.components", components.differences(other.components)))

	return diffs
}

private fun Company?.explainDifferences(other: Company?): List<Diff> {
	if (this == null || other == null) return listOf()

	val diffs = mutableListOf<Diff>()
	if (from != other.from) diffs.add(Diff("Company.from"))
	if (to != other.to) diffs.add(Diff("Company.to"))
	if (authorisationNr != other.authorisationNr) diffs.add(Diff("Company.authorisationNr"))
	if (vatNr != other.vatNr) diffs.add(Diff("Company.vatNr"))
	if (europeanNr != other.europeanNr) diffs.add(Diff("Company.europeanNr"))
	if (denomination != other.denomination) diffs.add(Diff("Company.denomination"))
	if (legalForm != other.legalForm) diffs.add(Diff("Company.legalForm"))
	if (building != other.building) diffs.add(Diff("Company.building"))
	if (streetName != other.streetName) diffs.add(Diff("Company.streetName"))
	if (streetNum != other.streetNum) diffs.add(Diff("Company.streetNum"))
	if (postbox != other.postbox) diffs.add(Diff("Company.postbox"))
	if (postcode != other.postcode) diffs.add(Diff("Company.postcode"))
	if (city != other.city) diffs.add(Diff("Company.city"))
	if (countryCode != other.countryCode) diffs.add(Diff("Company.countryCode"))
	if (phone != other.phone) diffs.add(Diff("Company.phone"))
	if (language != other.language) diffs.add(Diff("Company.language"))
	if (website != other.website) diffs.add(Diff("Company.website"))

	return diffs
}

private fun SamText?.explainDifferences(other: SamText?): List<Diff> {
	if (this == null || other == null) return listOf()

	val diffs = mutableListOf<Diff>()
	if (fr != other.fr) diffs.add(Diff("SamText.fr"))
	if (nl != other.nl) diffs.add(Diff("SamText.nl"))
	if (de != other.de) diffs.add(Diff("SamText.de"))
	if (en != other.en) diffs.add(Diff("SamText.en"))

	return diffs
}

private fun VmpStub?.explainDifferences(other: VmpStub?): List<Diff> {
	if (this == null || other == null) return listOf()

	val diffs = mutableListOf<Diff>()
	if (id != other.id) diffs.add(Diff("VmpStub.id"))
	if (code != other.code) diffs.add(Diff("VmpStub.code"))
	if (vmpGroup != other.vmpGroup) diffs.add(Diff("VmpStub.vmpGroup", vmpGroup.explainDifferences(other.vmpGroup)))
	if (name != other.name) diffs.add(Diff("VmpStub.name", name.explainDifferences(other.name)))

	return diffs
}

private fun VmpGroupStub?.explainDifferences(other: VmpGroupStub?): List<Diff> {
	if (this == null || other == null) return listOf()

	val diffs = mutableListOf<Diff>()
	if (id != other.id) diffs.add(Diff("VmpGroupStub.id"))
	if (code != other.code) diffs.add(Diff("VmpGroupStub.code"))
	if (name != other.name) diffs.add(Diff("VmpGroupStub.name"))

	return diffs
}
