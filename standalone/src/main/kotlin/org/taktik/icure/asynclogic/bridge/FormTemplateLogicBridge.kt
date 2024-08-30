package org.taktik.icure.asynclogic.bridge

import com.icure.sdk.api.raw.impl.RawFormApiImpl
import com.icure.sdk.crypto.impl.NoAccessControlKeysHeadersProvider
import com.icure.sdk.utils.InternalIcureApi
import com.icure.sdk.utils.Serialization
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.taktik.icure.asynclogic.FormTemplateLogic
import org.taktik.icure.asynclogic.bridge.auth.KmehrAuthProvider
import org.taktik.icure.asynclogic.bridge.mappers.FormTemplateMapper
import org.taktik.icure.asynclogic.impl.BridgeAsyncSessionLogic
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.entities.FormTemplate
import org.taktik.icure.errors.UnauthorizedException
import org.taktik.icure.exceptions.BridgeException

@Service
class FormTemplateLogicBridge(
    private val asyncSessionLogic: BridgeAsyncSessionLogic,
    private val bridgeConfig: BridgeConfig,
    private val formTemplateMapper: FormTemplateMapper
) : GenericLogicBridge<FormTemplate>(), FormTemplateLogic {

    @OptIn(InternalIcureApi::class)
    private suspend fun getApi() = asyncSessionLogic.getCurrentJWT()?.let { token ->
        RawFormApiImpl(
            apiUrl = bridgeConfig.iCureUrl,
            authProvider = KmehrAuthProvider(token),
            httpClient = bridgeHttpClient,
            json = Serialization.json,
            accessControlKeysHeadersProvider = NoAccessControlKeysHeadersProvider
        )
    } ?: throw UnauthorizedException("You must be logged in to perform this operation")

    @OptIn(InternalIcureApi::class)
    override suspend fun createFormTemplate(entity: FormTemplate): FormTemplate =
        getApi().createFormTemplate(formTemplateMapper.map(entity)).successBody().let(formTemplateMapper::map)

    override fun createFormTemplates(
        entities: Collection<FormTemplate>,
        createdEntities: Collection<FormTemplate>
    ): Flow<FormTemplate> {
        throw BridgeException()
    }

    override suspend fun getFormTemplate(formTemplateId: String): FormTemplate? {
        throw BridgeException()
    }

    @OptIn(InternalIcureApi::class)
    override fun getFormTemplatesByGuid(
        userId: String,
        specialityCode: String,
        formTemplateGuid: String
    ): Flow<FormTemplate> = flow {
        if (userId != asyncSessionLogic.getCurrentUserId())
            throw AccessDeniedException("You can only get form templates for the current authenticated user")
        emitAll(
            getApi().getFormTemplatesByGuid(formTemplateGuid, specialityCode)
                .successBody()
                .map(formTemplateMapper::map)
                .asFlow()
        )
    }

    override fun getFormTemplatesBySpecialty(specialityCode: String, loadLayout: Boolean): Flow<FormTemplate> {
        throw BridgeException()
    }

    override fun getFormTemplatesByUser(userId: String, loadLayout: Boolean): Flow<FormTemplate> {
        throw BridgeException()
    }

    override suspend fun modifyFormTemplate(formTemplate: FormTemplate): FormTemplate? {
        throw BridgeException()
    }
}
