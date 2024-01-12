package org.taktik.icure.asynclogic.bridge

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.entities.User
import org.taktik.icure.security.jwt.JwtUtils
import org.taktik.icure.services.external.rest.v2.mapper.UnsecureUserV2MapperImpl
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
class UserLogicBridgeTest(
    @Autowired val bridgeConfig: BridgeConfig,
    @Autowired val userMapper: UnsecureUserV2MapperImpl,
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

            val userBridge = UserLogicBridge(
                KmehrTestApplication.fakeSessionLogic,
                bridgeConfig,
                userMapper
            )

            userLogicBridgeTest(userBridge, hcp)
        }
    }
}

private fun StringSpec.userLogicBridgeTest(
    userBridge: UserLogicBridge,
    credentials: UserCredentials
) {

    "Can get a User" {
        withAuthenticatedReactorContext(credentials) {
            val newUser = userBridge.createUser(
                User(
                    id = uuid(),
                    login = uuid(),
                    email = uuid(),
                    healthcarePartyId = uuid()
                )
            )
            newUser shouldNotBe null

            val retrievedUser = userBridge.getUser(newUser!!.id)
            retrievedUser shouldNotBe null
            retrievedUser!!.id shouldBe newUser.id
        }
    }

    "Getting a user that does not exists returns null" {
        withAuthenticatedReactorContext(credentials) {
            userBridge.getUser(uuid()) shouldBe null
        }
    }

    "Can retrieve users by HCP id" {
        withAuthenticatedReactorContext(credentials) {
            val newUsers = List(5) {
                userBridge.createUser(
                    User(
                        id = uuid(),
                        login = uuid(),
                        email = uuid(),
                        healthcarePartyId = uuid()
                    )
                )
            }
            newUsers.size shouldBe 5

            val result = userBridge.listUserIdsByHcpartyId(newUsers.first()!!.healthcarePartyId!!).toList()
            result.size shouldBe 1
            result.first() shouldBe newUsers.first()!!.id
        }
    }

    "Retrieving users by HCP id will return an empty list if no user with that id is found" {
        withAuthenticatedReactorContext(credentials) {
            userBridge.listUserIdsByHcpartyId(uuid()).count() shouldBe 0
        }
    }
}
