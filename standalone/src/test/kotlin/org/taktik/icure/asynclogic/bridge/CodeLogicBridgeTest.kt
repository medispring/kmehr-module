package org.taktik.icure.asynclogic.bridge

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.entities.base.Code
import org.taktik.icure.security.jwt.JwtUtils
import org.taktik.icure.services.external.rest.v2.mapper.base.CodeV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.base.CodeV2MapperImpl
import org.taktik.icure.test.*

@SpringBootTest(
    classes = [KmehrTestApplication::class],
    properties = [
        "spring.main.allow-bean-definition-overriding=true"
    ],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles(profiles = ["kmehr"])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CodeLogicBridgeTest(
    val bridgeConfig: BridgeConfig,
    val codeMapper: CodeV2MapperImpl,
    val jwtUtils: JwtUtils
) : StringSpec() {

    init {
        runBlocking {
            val hcp = createHealthcarePartyUser(
                bridgeConfig.iCureUrl,
                KmehrTestApplication.masterHcp.login,
                KmehrTestApplication.masterHcp.password,
                jwtUtils
            )

            val codeBridge = CodeLogicBridge(
                KmehrTestApplication.fakeSessionLogic,
                bridgeConfig,
                codeMapper
            )

            codeLogicBridgeTest(codeBridge, hcp)
        }
    }
}

private fun StringSpec.codeLogicBridgeTest(
    codeBridge: CodeLogicBridge,
    credentials: UserCredentials
) {

    "Should be able of creating a code" {
        withAuthenticatedReactorContext(credentials) {
            val type = uuid().substring(0, 6)
            val code = uuid().substring(0, 6)
            val version = uuid().substring(0, 6)
            codeBridge.create(
                Code(
                    id = "$type|$code|$version",
                    type = type,
                    code = code,
                    version = version
                )
            ).let {
                it shouldNotBe null
                it!!.type shouldBe type
                it.code shouldBe code
                it.version shouldBe version
            }
        }
    }

    "Should be able to check if a code is valid" {
        withAuthenticatedReactorContext(credentials) {
            val type = uuid().substring(0, 6)
            val code = uuid().substring(0, 6)
            val version = uuid().substring(0, 6)
            val newCode = codeBridge.create(
                Code(
                    id = "$type|$code|$version",
                    type = type,
                    code = code,
                    version = version
                )
            )
            newCode shouldNotBe null
            codeBridge.isValid(newCode!!.type, newCode.code, newCode.version) shouldBe true
        }
    }

    "Should be able to check if a code is not valid" {
        withAuthenticatedReactorContext(credentials) {
            val type = uuid().substring(0, 6)
            val code = uuid().substring(0, 6)
            val version = uuid().substring(0, 6)
            codeBridge.isValid(type, code, version) shouldBe false
        }
    }

    "isValid returns false if the code passed as parameter has the wrong version" {
        withAuthenticatedReactorContext(credentials) {
            val type = uuid().substring(0, 6)
            val code = uuid().substring(0, 6)
            val version = uuid().substring(0, 6)
            val newCode = codeBridge.create(
                Code(
                    id = "$type|$code|$version",
                    type = type,
                    code = code,
                    version = version
                )
            )
            newCode shouldNotBe null
            codeBridge.isValid(newCode!!.type, newCode.code, uuid().substring(0, 6)) shouldBe false
        }
    }

    "isValid returns true if the code passed exists and no version is passed" {
        withAuthenticatedReactorContext(credentials) {
            val type = uuid().substring(0, 6)
            val code = uuid().substring(0, 6)
            val version = uuid().substring(0, 6)
            val newCode = codeBridge.create(
                Code(
                    id = "$type|$code|$version",
                    type = type,
                    code = code,
                    version = version
                )
            )
            newCode shouldNotBe null
            codeBridge.isValid(newCode!!.type, newCode.code, null) shouldBe true
        }
    }

    "Should be able of retrieving a code by region, language, type and label" {
        withAuthenticatedReactorContext(credentials) {
            val type = uuid().substring(0, 6)
            val code = uuid().substring(0, 6)
            val version = uuid().substring(0, 6)
            val newCode = codeBridge.create(
                Code(
                    id = "$type|$code|$version",
                    type = type,
                    code = code,
                    version = version,
                    regions = setOf(uuid().substring(0, 6)),
                    label = mapOf(uuid().substring(0, 6) to uuid().substring(0, 6))
                )
            )
            newCode shouldNotBe null
            codeBridge.getCodeByLabel(
                newCode!!.regions.first(),
                newCode.label!!.values.first(),
                newCode.type!!,
                newCode.label!!.keys.toList()
            ).let {
                it shouldNotBe null
                it?.id shouldBe newCode.id
            }
        }
    }

    "If no languages are passed, fr and nl are used by default" {
        withAuthenticatedReactorContext(credentials) {
            val type = uuid().substring(0, 6)
            val code = uuid().substring(0, 6)
            val version = uuid().substring(0, 6)
            val newCode = codeBridge.create(
                Code(
                    id = "$type|$code|$version",
                    type = type,
                    code = code,
                    version = version,
                    regions = setOf(uuid().substring(0, 6)),
                    label = mapOf("fr" to uuid().substring(0, 6))
                )
            )
            newCode shouldNotBe null
            codeBridge.getCodeByLabel(
                newCode!!.regions.first(),
                newCode.label!!.values.first(),
                newCode.type!!
            ).let {
                it shouldNotBe null
                it?.id shouldBe newCode.id
            }
        }
    }

    "If the code is not found, null is returned" {
        withAuthenticatedReactorContext(credentials) {
            codeBridge.getCodeByLabel(
                uuid().substring(0, 6),
                uuid().substring(0, 6),
                uuid().substring(0, 6)
            ) shouldBe null
        }
    }
}
