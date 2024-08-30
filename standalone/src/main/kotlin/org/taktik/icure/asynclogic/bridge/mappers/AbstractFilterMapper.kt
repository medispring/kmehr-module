package org.taktik.icure.asynclogic.bridge.mappers

import com.fasterxml.jackson.databind.ObjectMapper
import com.icure.sdk.utils.Serialization
import com.icure.sdk.model.filter.AbstractFilter as SdkAbstractFilter
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.couchdb.id.Identifiable
import com.icure.sdk.model.base.Identifiable as SdkIdentifiable

abstract class AbstractFilterMapper<T : Identifiable<String>, U: SdkIdentifiable<String>>(
	protected val objectMapper: ObjectMapper
) {

	protected inline fun <reified ICURE_FILTER : AbstractFilter<T>, reified SDK_FILTER: SdkAbstractFilter<U>> doMap(filter: ICURE_FILTER): SDK_FILTER =
		Serialization.lenientJson.decodeFromString(objectMapper.writeValueAsString(filter))

}