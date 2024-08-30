package org.taktik.icure.asynclogic.bridge.mappers

import com.fasterxml.jackson.databind.ObjectMapper
import com.icure.sdk.utils.Serialization
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

abstract class AbstractEntityMapper<ICURE_TYPE : Any, SDK_TYPE : Any, DTO_TYPE>(
	private val objectMapper: ObjectMapper,
	private val sdkTypeClass: KClass<SDK_TYPE>,
	private val dtoTypeClass: Class<DTO_TYPE>,
	private val domainToDto: (ICURE_TYPE) -> DTO_TYPE,
	private val dtoToDomain: (DTO_TYPE) -> ICURE_TYPE
) {

	@JvmName("mapIcureTypeToSdkType")
	@OptIn(InternalSerializationApi::class)
	fun map(entity: ICURE_TYPE): SDK_TYPE = Serialization.json.decodeFromString(
		sdkTypeClass.serializer(),
		objectMapper.writeValueAsString(domainToDto(entity))
	)

	@JvmName("mapSdkTypeToIcureType")
	@OptIn(InternalSerializationApi::class)
	fun map(entity: SDK_TYPE): ICURE_TYPE = objectMapper.readValue(
		Serialization.json.encodeToString(sdkTypeClass.serializer(), entity),
		dtoTypeClass
	).let(dtoToDomain)

}