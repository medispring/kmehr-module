package org.taktik.icure.asynclogic.bridge

import io.icure.kraken.client.apis.FormApi
import io.icure.kraken.client.security.ExternalJWTProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.taktik.icure.asynclogic.FormTemplateLogic
import org.taktik.icure.asynclogic.impl.BridgeAsyncSessionLogic
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.entities.FormTemplate
import org.taktik.icure.services.external.rest.v2.mapper.FormTemplateV2Mapper

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
@Service
class FormTemplateLogicBridge(
    private val asyncSessionLogic: BridgeAsyncSessionLogic,
    private val bridgeConfig: BridgeConfig,
    private val formTemplateMapper: FormTemplateV2Mapper
) : GenericLogicBridge<FormTemplate>(), FormTemplateLogic {

    private suspend fun getApi() = asyncSessionLogic.getCurrentJWT()?.let {
        FormApi(basePath = bridgeConfig.iCureUrl, authProvider = ExternalJWTProvider(it))
    }

    override suspend fun createFormTemplate(entity: FormTemplate): FormTemplate =
        getApi()?.createFormTemplate(formTemplateMapper.map(entity))?.let(formTemplateMapper::map)
            ?: throw IllegalStateException("Cannot create the form template")

    override fun createFormTemplates(
        entities: Collection<FormTemplate>,
        createdEntities: Collection<FormTemplate>
    ): Flow<FormTemplate> {
        throw IllegalStateException("Bridge method not implemented")
    }

    override suspend fun getFormTemplate(formTemplateId: String): FormTemplate? {
        throw IllegalStateException("Bridge method not implemented")
    }

    override fun getFormTemplatesByGuid(
        userId: String,
        specialityCode: String,
        formTemplateGuid: String
    ): Flow<FormTemplate> = flow {
        if (userId != asyncSessionLogic.getCurrentUserId())
            throw AccessDeniedException("You can only get form templates for the current authenticated user")
        emitAll(
            getApi()?.getFormTemplatesByGuid(formTemplateGuid, specialityCode)
                ?.map(formTemplateMapper::map)
                ?.asFlow() ?: emptyFlow()
        )
    }

    override fun getFormTemplatesBySpecialty(specialityCode: String, loadLayout: Boolean): Flow<FormTemplate> {
        throw IllegalStateException("Bridge method not implemented")
    }

    override fun getFormTemplatesByUser(userId: String, loadLayout: Boolean): Flow<FormTemplate> {
        throw IllegalStateException("Bridge method not implemented")
    }

    override suspend fun modifyFormTemplate(formTemplate: FormTemplate): FormTemplate? {
        throw IllegalStateException("Bridge method not implemented")
    }
}
