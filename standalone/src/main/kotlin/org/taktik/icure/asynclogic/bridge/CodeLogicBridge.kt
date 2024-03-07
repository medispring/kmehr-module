package org.taktik.icure.asynclogic.bridge

import io.icure.kraken.client.apis.CodeApi
import io.icure.kraken.client.security.ExternalJWTProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.asynclogic.CodeLogic
import org.taktik.icure.asynclogic.impl.BridgeAsyncSessionLogic
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.base.Code
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.errors.UnauthorizedException
import org.taktik.icure.exceptions.BridgeException
import org.taktik.icure.pagination.PaginationElement
import org.taktik.icure.services.external.rest.v2.mapper.base.CodeV2Mapper
import java.io.InputStream

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
@Service
class CodeLogicBridge(
    private val asyncSessionLogic: BridgeAsyncSessionLogic,
    private val bridgeConfig: BridgeConfig,
    private val codeMapper: CodeV2Mapper,
) : GenericLogicBridge<Code>(), CodeLogic {

    private suspend fun getApi() = asyncSessionLogic.getCurrentJWT()?.let { token ->
        CodeApi(basePath = bridgeConfig.iCureUrl, authProvider = ExternalJWTProvider(token))
    }
    override suspend fun create(batch: List<Code>): List<Code>? {
        throw BridgeException()
    }

    override suspend fun modify(code: Code): Code? {
        throw BridgeException()
    }

    override suspend fun create(code: Code): Code? =
        getApi()?.createCode(codeMapper.map(code))?.let(codeMapper::map)

    override fun findCodesBy(type: String?, code: String?, version: String?): Flow<Code> {
        throw BridgeException()
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

    override fun listCodeIdsByLabel(region: String?, language: String, type: String, label: String?): Flow<String> {
        throw BridgeException()
    }

    override fun findCodesByQualifiedLinkId(
        region: String?,
        linkType: String,
        linkedId: String?,
        pagination: PaginationOffset<List<String>>
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
            getApi()?.isValid(type, code, version, null)
                ?: throw UnauthorizedException("You must be logged in to perform this operation")
        else false

    override suspend fun isValid(code: Code, ofType: String?): Boolean {
        throw BridgeException()
    }

    override suspend fun isValid(code: CodeStub, ofType: String?): Boolean = isValid(code.type, code.code, code.version)
    override suspend fun getCodeByLabel(region: String?, label: String, type: String, languages: List<String>): Code? =
        getApi()?.getCodeByRegionLanguagesTypeLabel(region, label, type, languages)?.let {
            codeMapper.map(it)
        }

    override fun listCodeIdsByQualifiedLinkId(linkType: String, linkedId: String?): Flow<String> {
        throw BridgeException()
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
