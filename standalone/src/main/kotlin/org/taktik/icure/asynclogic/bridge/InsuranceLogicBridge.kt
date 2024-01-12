package org.taktik.icure.asynclogic.bridge

import io.icure.kraken.client.apis.InsuranceApi
import io.icure.kraken.client.infrastructure.ClientException
import io.icure.kraken.client.security.ExternalJWTProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.icure.asynclogic.InsuranceLogic
import org.taktik.icure.asynclogic.impl.BridgeAsyncSessionLogic
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Insurance
import org.taktik.icure.exceptions.BridgeException
import org.taktik.icure.services.external.rest.v2.mapper.InsuranceV2Mapper

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
@Service
class InsuranceLogicBridge(
    val asyncSessionLogic: BridgeAsyncSessionLogic,
    val bridgeConfig: BridgeConfig,
    private val insuranceMapper: InsuranceV2Mapper
) : GenericLogicBridge<Insurance>(), InsuranceLogic {

    private suspend fun getApi() = asyncSessionLogic.getCurrentJWT()?.let {
        InsuranceApi(basePath = bridgeConfig.iCureUrl, authProvider = ExternalJWTProvider(it))
    }

    override suspend fun createInsurance(insurance: Insurance): Insurance? {
        throw BridgeException()
    }

    override suspend fun deleteInsurance(insuranceId: String): DocIdentifier? {
        throw BridgeException()
    }

    override fun getAllInsurances(paginationOffset: PaginationOffset<Nothing>): Flow<ViewRowWithDoc<Any?, String, Insurance>> {
        throw BridgeException()
    }

    override suspend fun getInsurance(insuranceId: String): Insurance? =
        try {
            getApi()?.getInsurance(insuranceId)?.let {
                insuranceMapper.map(it)
            }
        } catch (_: ClientException) { null }

    override fun getInsurances(ids: Set<String>): Flow<Insurance> {
        throw BridgeException()
    }

    override fun listInsurancesByCode(code: String): Flow<Insurance> {
        throw BridgeException()
    }

    override fun listInsurancesByName(name: String): Flow<Insurance> {
        throw BridgeException()
    }

    override suspend fun modifyInsurance(insurance: Insurance): Insurance? {
        throw BridgeException()
    }
}
