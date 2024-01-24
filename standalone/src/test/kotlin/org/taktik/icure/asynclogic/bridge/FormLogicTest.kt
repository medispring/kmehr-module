package org.taktik.icure.asynclogic.bridge

import io.icure.kraken.client.apis.FormApi
import io.icure.kraken.client.security.ExternalJWTProvider
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestInstance
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.entities.Form
import org.taktik.icure.security.jwt.JwtUtils
import org.taktik.icure.services.external.rest.v2.mapper.FormV2Mapper
import org.taktik.icure.test.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FormLogicTest(
    val bridgeConfig: BridgeConfig,
    val formMapper: FormV2Mapper,
    val jwtUtils: JwtUtils
) : BaseKmehrTest() {
    init {
        runBlocking {
            val hcp = createHealthcarePartyUser(
                bridgeConfig.iCureUrl,
                KmehrTestApplication.masterHcp.login,
                KmehrTestApplication.masterHcp.password,
                jwtUtils
            )

            val formLogic = FormLogicBridge(
                KmehrTestApplication.fakeSessionLogic,
                bridgeConfig,
                formMapper
            )

            formLogicBridgeTest(bridgeConfig.iCureUrl, hcp, formLogic)
        }
    }
}

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
private fun StringSpec.formLogicBridgeTest(
    iCureUrl: String,
    credentials: UserCredentials,
    formBridge: FormLogicBridge
) {

    "Can create a Form" {
        withAuthenticatedReactorContext(credentials) {
            val createdForm = formBridge.createForm(
                Form(
                    id = uuid(),
                    logicalUuid = uuid(),
                    delegations = mapOf(credentials.dataOwnerId!! to emptySet())
                )
            )
            createdForm shouldNotBe null
            FormApi(basePath = iCureUrl, authProvider = ExternalJWTProvider(credentials.authJWT!!))
                .getForm(createdForm!!.id)
                .let {
                    it.id shouldBe createdForm.id
                    it.logicalUuid shouldBe createdForm.logicalUuid
                }
        }
    }
}
