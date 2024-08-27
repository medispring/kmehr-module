package org.taktik.icure.asynclogic.bridge

import com.icure.sdk.api.raw.impl.RawInsuranceApiImpl
import com.icure.sdk.api.raw.successBodyOrNull404
import com.icure.sdk.utils.InternalIcureApi
import com.icure.sdk.utils.Serialization
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service
import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.asynclogic.InsuranceLogic
import org.taktik.icure.asynclogic.bridge.auth.KmehrAuthProvider
import org.taktik.icure.asynclogic.bridge.mappers.InsuranceMapper
import org.taktik.icure.asynclogic.impl.BridgeAsyncSessionLogic
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Insurance
import org.taktik.icure.errors.UnauthorizedException
import org.taktik.icure.exceptions.BridgeException
import org.taktik.icure.pagination.PaginationElement

@Service
class InsuranceLogicBridge(
    val asyncSessionLogic: BridgeAsyncSessionLogic,
    val bridgeConfig: BridgeConfig,
    private val insuranceMapper: InsuranceMapper
) : GenericLogicBridge<Insurance>(), InsuranceLogic {

    @OptIn(InternalIcureApi::class)
    private suspend fun getApi() = asyncSessionLogic.getCurrentJWT()?.let { token ->
        RawInsuranceApiImpl(
            apiUrl = bridgeConfig.iCureUrl,
            authProvider = KmehrAuthProvider(token),
            httpClient = bridgeHttpClient,
            json = Serialization.json
        )
    } ?: throw UnauthorizedException("You must be logged in to perform this operation")

    override suspend fun createInsurance(insurance: Insurance): Insurance? {
        throw BridgeException()
    }

    override suspend fun deleteInsurance(insuranceId: String): DocIdentifier? {
        throw BridgeException()
    }

    override fun getAllInsurances(paginationOffset: PaginationOffset<Nothing>): Flow<PaginationElement> {
        throw BridgeException()
    }

    @OptIn(InternalIcureApi::class)
    override suspend fun getInsurance(insuranceId: String): Insurance? =
        getApi().getInsurance(insuranceId).successBodyOrNull404()?.let(insuranceMapper::map)


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
