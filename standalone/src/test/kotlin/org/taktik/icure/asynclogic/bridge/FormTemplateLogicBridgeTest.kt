package org.taktik.icure.asynclogic.bridge

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.access.AccessDeniedException
import org.springframework.test.context.ActiveProfiles
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.entities.FormTemplate
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.security.jwt.JwtUtils
import org.taktik.icure.services.external.rest.v2.mapper.FormTemplateV2MapperImpl
import org.taktik.icure.test.KmehrTestApplication
import org.taktik.icure.test.UserCredentials
import org.taktik.icure.test.createHealthcarePartyUser
import org.taktik.icure.test.uuid
import org.taktik.icure.test.withAuthenticatedReactorContext

@SpringBootTest(
    classes = [KmehrTestApplication::class],
    properties = [
        "spring.main.allow-bean-definition-overriding=true"
    ],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles(profiles = ["kmehr"])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FormTemplateLogicBridgeTest(
    @Autowired val bridgeConfig: BridgeConfig,
    @Autowired val formTemplateMapper: FormTemplateV2MapperImpl,
    @Autowired val jwtUtils: JwtUtils
) : StringSpec() {

    init {
        runBlocking {
            val hcp = createHealthcarePartyUser(
                bridgeConfig.iCureUrl,
                KmehrTestApplication.masterHcp.login,
                KmehrTestApplication.masterHcp.password,
                jwtUtils
            )


            val formTemplateLogic = FormTemplateLogicBridge(
                KmehrTestApplication.fakeSessionLogic,
                bridgeConfig,
                formTemplateMapper
            )

            formTemplateLogicBridgeTest(hcp, formTemplateLogic)
        }
    }
}

private fun StringSpec.formTemplateLogicBridgeTest(
    credentials: UserCredentials,
    formTemplateBridge: FormTemplateLogicBridge
) {

    "Can retrieve a Form Template by GUID" {
        withAuthenticatedReactorContext(credentials) {
            val type = uuid().substring(0, 6)
            val code = uuid().substring(0, 6)
            val version = uuid().substring(0, 6)
            val template = formTemplateBridge.createFormTemplate(
                FormTemplate(
                    id = uuid(),
                    guid = uuid(),
                    specialty = CodeStub(
                        id = "$type|$code|$version",
                        type = type,
                        code = code,
                        version = version
                    )
                )
            )
            formTemplateBridge.getFormTemplatesByGuid(credentials.userId, code, template.guid!!).toList().let {
                it.size shouldBe 1
                it.first().id shouldBe template.id
            }
        }
    }

    "Cannot retrieve a Form Template as another user" {
        withAuthenticatedReactorContext(credentials) {
            val template = formTemplateBridge.createFormTemplate(
                FormTemplate(
                    id = uuid(),
                    guid = uuid()
                )
            )

            shouldThrow<AccessDeniedException> {
                formTemplateBridge.getFormTemplatesByGuid(uuid(), uuid(), template.guid!!).toList()
            }
        }
    }

    "If no Form Template matches the parameters, an empty flow is returned" {
        withAuthenticatedReactorContext(credentials) {
            formTemplateBridge.getFormTemplatesByGuid(credentials.userId, uuid(), uuid()).count() shouldBe 0
        }
    }

}
