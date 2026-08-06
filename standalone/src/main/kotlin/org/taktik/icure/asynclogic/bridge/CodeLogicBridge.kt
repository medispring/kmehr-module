package org.taktik.icure.asynclogic.bridge

import com.icure.cardinal.sdk.CardinalBaseApis
import com.icure.cardinal.sdk.api.raw.RawCodeApi
import com.icure.cardinal.sdk.model.filter.code.CodeIdsByTypeCodeVersionIntervalFilter
import com.icure.utils.InternalIcureApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Service
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.asynclogic.CodeLogic
import org.taktik.icure.asynclogic.bridge.mappers.CodeMapper
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.base.Code
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.exceptions.BridgeException
import org.taktik.icure.pagination.PaginationElement
import java.io.InputStream

@OptIn(InternalIcureApi::class)
@Service
class CodeLogicBridge(
	private val sdk: CardinalBaseApis,
	private val rawCodeApi: RawCodeApi,
	private val codeMapper: CodeMapper,
) : GenericLogicBridge<Code>(), CodeLogic {

	override suspend fun modify(code: Code): Code {
		throw BridgeException()
	}

	override suspend fun create(code: Code): Code =
		sdk.code.createCode(codeMapper.map(code)).let {
			codeMapper.map(it)
		}

	override fun create(codes: List<Code>): Flow<Code> {
		throw BridgeException()
	}

	override fun findCodesBy(type: String?, code: String?, version: String?): Flow<Code> = flow {
		val filter = CodeIdsByTypeCodeVersionIntervalFilter(
			startType = type,
			startCode = code,
			startVersion = version,
			endType = type,
			endCode = code,
			endVersion = version
		)
		val codeIds = rawCodeApi.matchCodesBy(filter).successBody()
		emitAll(
			sdk.code.getCodes(codeIds)
				.map { codeMapper.map(it) }
				.asFlow()
		)
	}

	override fun findCodesBy(region: String?, type: String?, code: String?, version: String?): Flow<Code> {
		throw BridgeException()
	}

	override fun findCodesBy(
		region: String?,
		type: String?,
		code: String?,
		version: String?,
		paginationOffset: PaginationOffset<List<String?>>
	): Flow<PaginationElement> {
		throw BridgeException()
	}

	override fun findCodesByLabel(
		region: String?,
		language: String,
		types: Set<String>,
		label: String,
		version: String?,
		paginationOffset: PaginationOffset<List<String?>>
	): Flow<PaginationElement> {
		throw BridgeException()
	}

	override suspend fun get(id: String): Code? {
		throw BridgeException()
	}

	override suspend fun get(type: String, code: String, version: String): Code? {
		throw BridgeException()
	}

	override fun getCodes(ids: List<String>): Flow<Code> {
		throw BridgeException()
	}

	override suspend fun getOrCreateCode(type: String, code: String, version: String): Code? {
		throw BridgeException()
	}

	override fun getRegions(): List<String> {
		throw BridgeException()
	}

	override fun getTagTypeCandidates(): List<String> {
		throw BridgeException()
	}

	override suspend fun <T : Enum<*>> importCodesFromEnum(e: Class<T>) {
		throw BridgeException()
	}

	override suspend fun importCodesFromJSON(stream: InputStream) {
		throw BridgeException()
	}

	override suspend fun importCodesFromXml(md5: String, type: String, stream: InputStream) {
		throw BridgeException()
	}

	override suspend fun isValid(type: String?, code: String?, version: String?): Boolean =
		if(type != null && code != null)
			sdk.code.isCodeValid(type, code, version).response
		else false

	override suspend fun isValid(code: Code, ofType: String?): Boolean {
		throw BridgeException()
	}

	override suspend fun isValid(code: CodeStub, ofType: String?): Boolean =
		isValid(code.type, code.code, code.version)

	override suspend fun getCodeByLabel(region: String?, label: String, type: String, languages: List<String>): Code? {
		if(region == null) {
			throw IllegalArgumentException("Region cannot be null")
		}
		return sdk.code.getCodeByRegionLanguageTypeLabel(region, label, type, languages.joinToString(","))
			?.let { codeMapper.map(it) }
	}

	override fun listCodeIdsByTypeCodeVersionInterval(
		startType: String?,
		startCode: String?,
		startVersion: String?,
		endType: String?,
		endCode: String?,
		endVersion: String?
	): Flow<String> {
		throw BridgeException()
	}

	override fun findCodesByQualifiedLinkId(
		linkType: String,
		linkedId: String?,
		pagination: PaginationOffset<List<String>>
	): Flow<PaginationElement> {
		throw BridgeException()
	}

	override fun listCodeTypesBy(region: String?, type: String?): Flow<String> {
		throw BridgeException()
	}

	override fun listCodes(
		paginationOffset: PaginationOffset<*>?,
		filterChain: FilterChain<Code>,
		sort: String?,
		desc: Boolean?
	): Flow<ViewQueryResultEvent> {
		throw BridgeException()
	}

	override fun modify(batch: List<Code>): Flow<Code> {
		throw BridgeException()
	}

}
