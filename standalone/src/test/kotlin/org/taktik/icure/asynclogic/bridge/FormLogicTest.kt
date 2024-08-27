package org.taktik.icure.asynclogic.bridge

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestInstance
import org.taktik.icure.asynclogic.bridge.mappers.FormMapper
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.entities.Form
import org.taktik.icure.security.jwt.JwtUtils
import org.taktik.icure.test.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FormLogicTest(
    private val bridgeConfig: BridgeConfig,
    private val formMapper: FormMapper,
    private val jwtUtils: JwtUtils
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

            formLogicBridgeTest(hcp, formLogic)
        }
    }
}

private fun StringSpec.formLogicBridgeTest(
    credentials: UserCredentials,
    formBridge: FormLogicBridge
) {

    "Can create a Form" {
        withAuthenticatedReactorContext(credentials) {
            formBridge.createForm(
                Form(
                    id = uuid(),
                    logicalUuid = uuid(),
                    delegations = mapOf(credentials.dataOwnerId!! to emptySet())
                )
            ).shouldNotBeNull()
        }
    }
}
