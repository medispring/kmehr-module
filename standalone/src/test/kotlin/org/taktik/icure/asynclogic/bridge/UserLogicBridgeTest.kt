package org.taktik.icure.asynclogic.bridge

import io.icure.kraken.client.apis.UserApi
import io.icure.kraken.client.infrastructure.ClientException
import io.icure.kraken.client.security.JWTProvider
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestInstance
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.entities.User
import org.taktik.icure.entities.security.AuthenticationToken
import org.taktik.icure.security.jwt.JwtUtils
import org.taktik.icure.services.external.rest.v2.dto.UserDto
import org.taktik.icure.services.external.rest.v2.mapper.UnsecureUserV2MapperImpl
import org.taktik.icure.test.*
import java.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserLogicBridgeTest(
    val bridgeConfig: BridgeConfig,
    val userMapper: UnsecureUserV2MapperImpl,
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

            val userBridge = UserLogicBridge(
                KmehrTestApplication.fakeSessionLogic,
                bridgeConfig,
                userMapper
            )

            userLogicBridgeTest(userBridge, hcp)
        }
    }

    @OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
    private suspend fun createUser(user: User) =
        UserApi(basePath = bridgeConfig.iCureUrl, authProvider = JWTProvider(bridgeConfig.iCureUrl, KmehrTestApplication.masterHcp.login, KmehrTestApplication.masterHcp.password))
            .createUser(userMapper.map(user)).let { it: UserDto ->
                userMapper.map(it.copy(
                    authenticationTokens = it.authenticationTokens.mapValues { (_, token) -> token.copy(token = "*") }
                ))
            }

    private fun StringSpec.userLogicBridgeTest(
        userBridge: UserLogicBridge,
        credentials: UserCredentials
    ) {

        "Can get a User" {
            withAuthenticatedReactorContext(credentials) {
                val newUser = createUser(
                    User(
                        id = uuid(),
                        login = uuid(),
                        email = uuid(),
                        healthcarePartyId = uuid(),
                        passwordHash = uuid(),
                        authenticationTokens = mapOf("test" to AuthenticationToken("token", Instant.now()))
                    )
                )

                val retrievedUser = userBridge.getUser(newUser.id).shouldNotBeNull()
                retrievedUser.id shouldBe newUser.id
            }
        }

        "Getting a user that does not exists returns null" {
            withAuthenticatedReactorContext(credentials) {
                shouldThrow<ClientException> { userBridge.getUser(uuid()) }.also {
                    it.statusCode shouldBe 404
                }
            }
        }

        "Can retrieve users by HCP id" {
            withAuthenticatedReactorContext(credentials) {
                val newUsers = List(5) {
                    createUser(
                        User(
                            id = uuid(),
                            login = uuid(),
                            email = uuid(),
                            healthcarePartyId = uuid(),
                            passwordHash = uuid(),
                            authenticationTokens = mapOf("test" to AuthenticationToken("token", Instant.now()))
                        )
                    )
                }
                newUsers.size shouldBe 5

                val result = userBridge.listUserIdsByHcpartyId(newUsers.first().healthcarePartyId.shouldNotBeNull()).toList()
                result.size shouldBe 1
                result.first() shouldBe newUsers.first().id
            }
        }

        "Retrieving users by HCP id will return an empty list if no user with that id is found" {
            withAuthenticatedReactorContext(credentials) {
                userBridge.listUserIdsByHcpartyId(uuid()).count() shouldBe 0
            }
        }

        "The bridge cannot create a user" {
            withAuthenticatedReactorContext(credentials) {
                shouldThrow<UnsupportedOperationException> {
                    userBridge.createUser(User(id = uuid()))
                }
            }
        }

        "The bridge cannot delete a user" {
            withAuthenticatedReactorContext(credentials) {
                shouldThrow<UnsupportedOperationException> {
                    userBridge.deleteUser(uuid())
                }
            }
        }

        "The bridge cannot disable a user" {
            withAuthenticatedReactorContext(credentials) {
                shouldThrow<UnsupportedOperationException> {
                    userBridge.disableUser(uuid())
                }
            }
        }

        "The bridge cannot enable a user" {
            withAuthenticatedReactorContext(credentials) {
                shouldThrow<UnsupportedOperationException> {
                    userBridge.enableUser(uuid())
                }
            }
        }

        "The bridge cannot modify a user" {
            withAuthenticatedReactorContext(credentials) {
                shouldThrow<UnsupportedOperationException> {
                    userBridge.modifyUser(User(id = uuid()))
                }
            }
        }

        "The bridge cannot undelete a user" {
            withAuthenticatedReactorContext(credentials) {
                shouldThrow<UnsupportedOperationException> {
                    userBridge.undeleteUser(uuid())
                }
            }
        }

        "The bridge cannot set the properties for a user" {
            withAuthenticatedReactorContext(credentials) {
                shouldThrow<UnsupportedOperationException> {
                    userBridge.setProperties(uuid(), emptyList())
                }
            }
        }
    }
}


