package org.taktik.icure.service.external.rest.shared.controllers.be

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.taktik.couchdb.create
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.samv2.AmpDAO
import org.taktik.icure.asyncdao.samv2.NmpDAO
import org.taktik.icure.asyncdao.samv2.VmpDAO
import org.taktik.icure.asyncdao.samv2.VmpGroupDAO
import org.taktik.icure.asynclogic.datastore.impl.SAMDatastoreInformation
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.entities.samv2.Amp
import org.taktik.icure.entities.samv2.Nmp
import org.taktik.icure.entities.samv2.SamVersion
import org.taktik.icure.entities.samv2.Vmp
import org.taktik.icure.entities.samv2.VmpGroup
import org.taktik.icure.entities.samv2.embed.Ampp
import org.taktik.icure.entities.samv2.embed.Atc
import org.taktik.icure.entities.samv2.embed.Dmpp
import org.taktik.icure.entities.samv2.embed.SamText
import org.taktik.icure.entities.samv2.stub.VmpGroupStub
import org.taktik.icure.entities.samv2.stub.VmpStub
import org.taktik.icure.properties.CouchDbProperties
import org.taktik.icure.security.jwt.JwtUtils
import org.taktik.icure.services.external.rest.v1.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v1.dto.PaginatedList
import org.taktik.icure.services.external.rest.v1.dto.samv2.AmpDto
import org.taktik.icure.services.external.rest.v1.dto.samv2.NmpDto
import org.taktik.icure.services.external.rest.v1.dto.samv2.VmpDto
import org.taktik.icure.services.external.rest.v1.dto.samv2.VmpGroupDto
import org.taktik.icure.test.KmehrTestApplication
import org.taktik.icure.test.TestHttpClient
import org.taktik.icure.test.UserCredentials
import org.taktik.icure.test.createHealthcarePartyUser
import org.taktik.icure.test.createPatientUser
import org.taktik.icure.test.uuid
import org.taktik.icure.utils.GzipDeflateInputStream
import java.io.InputStream
import java.net.URI
import java.nio.ByteBuffer

enum class CredentialsType { HCP, PATIENT }

@SpringBootTest(
    classes = [KmehrTestApplication::class],
    properties = [
        "spring.main.allow-bean-definition-overriding=true",
        "icure.couchdb.url=http://127.0.0.1:15984",
        "icure.couchdb.username=icure",
        "icure.couchdb.password=icure"
    ],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles(profiles = ["sam"])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SAMV2ControllerE2ETest(
    @LocalServerPort val port: Int,
    val httpClient: TestHttpClient,
    val bridgeConfig: BridgeConfig,
    val jwtUtils: JwtUtils,
    val ampDAO: AmpDAO,
    val vmpDAO: VmpDAO,
    val nmpDAO: NmpDAO,
    val vmpGroupDAO: VmpGroupDAO,
    val objectMapper: ObjectMapper,
    @Qualifier("drugCouchDbDispatcher") val couchDbDispatcher: CouchDbDispatcher,
    val couchDbProperties: CouchDbProperties
) : StringSpec() {

    private fun InputStream.toFlow() = flow {
        do {
            val buffer = ByteArray(512)
            val read = this@toFlow.read(buffer, 0, 512)
            if (read > 0) {
                emit(ByteBuffer.wrap(buffer, 0, read))
            }
        } while (read >= 0)
    }.flowOn(Dispatchers.IO)

    private fun makeSignatures(nmpProductIds: Map<String, String>) =
        GzipDeflateInputStream(
            nmpProductIds.entries.sortedBy { it.key }
                .joinToString("\n") { "${it.key}|${it.value}" }
                .byteInputStream(Charsets.UTF_8)
        ).toFlow()

    inner class CredentialsProvider {
        private val credentials = mutableMapOf<CredentialsType, UserCredentials>()

        suspend fun getCredentials(type: CredentialsType): UserCredentials =
            credentials[type]?.takeIf { jwtUtils.isNotExpired(it.authJWT!!) } ?: when(type) {
                CredentialsType.HCP -> createHealthcarePartyUser(
                    bridgeConfig.iCureUrl,
                    KmehrTestApplication.masterHcp.login,
                    KmehrTestApplication.masterHcp.password,
                    jwtUtils
                )
                CredentialsType.PATIENT -> createPatientUser(
                    bridgeConfig.iCureUrl,
                    KmehrTestApplication.masterHcp.login,
                    KmehrTestApplication.masterHcp.password
                )
            }.also {
                credentials[type] = it
            }
    }

    private suspend fun createVersionAttachment(type: String) {
        val datastore = SAMDatastoreInformation(URI.create(couchDbProperties.url))
        val drugsClient = couchDbDispatcher.getClient(datastore)
        ampDAO.getSignature(datastore, type) ?: run {
            val samVersion = drugsClient.create(entity = SamVersion(
                id = "org.taktik.icure.samv2.signatures.$type",
                version = "E.20230316_022134",
                date = 20230316
            ))
            drugsClient.createAttachment(
                samVersion.id,
                "signatures",
                samVersion.rev.shouldNotBeNull(),
                "application/gzip",
                makeSignatures(emptyMap())
            )
        }
    }

    init {
        runBlocking {
            val credentialsProvider = CredentialsProvider()

            createVersionAttachment("amp")
            createVersionAttachment("vmp")
            createVersionAttachment("nmp")

            listOf("v1", "v2").forEach {
                findAmpsByLabelE2ETest(httpClient, credentialsProvider, objectMapper, ampDAO, it, "http://127.0.0.1:$port")
                findVmpsByLabelE2ETest(httpClient, credentialsProvider, objectMapper, vmpDAO, it, "http://127.0.0.1:$port")
                findVmpsByGroupCodeE2ETest(httpClient, credentialsProvider, objectMapper, vmpDAO, it, "http://127.0.0.1:$port")
                findVmpsByCodeE2ETest(httpClient, credentialsProvider, objectMapper, vmpDAO, it, "http://127.0.0.1:$port")
                findNmpsByLabelE2ETest(httpClient, credentialsProvider, objectMapper, nmpDAO, it, "http://127.0.0.1:$port")
                findVmpsByGroupIdE2ETest(httpClient, credentialsProvider, objectMapper, vmpDAO, it, "http://127.0.0.1:$port")
                findAmpsByVmpGroupCodeE2ETest(httpClient, credentialsProvider, objectMapper, ampDAO, it, "http://127.0.0.1:$port")
                findAmpsByVmpGroupIdE2ETest(httpClient, credentialsProvider, objectMapper, ampDAO, it, "http://127.0.0.1:$port")
                findAmpsByVmpCodeE2ETest(httpClient, credentialsProvider, objectMapper, ampDAO, it, "http://127.0.0.1:$port")
                findAmpsByAtcE2ETest(httpClient, credentialsProvider, objectMapper, ampDAO, it, "http://127.0.0.1:$port")
                findAmpsByVmpIdE2ETest(httpClient, credentialsProvider, objectMapper, ampDAO, it, "http://127.0.0.1:$port")
                findAmpsByDmppE2ETest(httpClient, credentialsProvider, objectMapper, ampDAO, it, "http://127.0.0.1:$port")
                findVmpGroupsByLabelE2ETest(httpClient, credentialsProvider, objectMapper, vmpGroupDAO, it, "http://127.0.0.1:$port")
                findVmpGroupsByCodeE2ETest(httpClient, credentialsProvider, objectMapper, vmpGroupDAO, it, "http://127.0.0.1:$port")
                findNmpsByCnkE2ETest(httpClient, credentialsProvider, objectMapper, nmpDAO, it, "http://127.0.0.1:$port")
            }
        }
    }
}


private fun StringSpec.findNmpsByCnkE2ETest(
    httpClient: TestHttpClient,
    credentialsProvider: SAMV2ControllerE2ETest.CredentialsProvider,
    objectMapper: ObjectMapper,
    nmpDAO: NmpDAO,
    apiVersion: String,
    samUrl: String
) {

    "$apiVersion - An HCP can get Nmps by CNK" {
        val cnk = uuid()
        val size = 10
        val createdAmps = nmpDAO.save(List(size) {
            Nmp(
                id = uuid(),
                code = cnk
            )
        }).mapNotNull { it.id }.toList()
        createdAmps.size shouldBe size

        nmpDAO.save(List(size) {
            Nmp(
                id = uuid(),
                code = uuid()
            )
        })

        val responseString = httpClient.makePostRequest(
            "$samUrl/rest/$apiVersion/be_samv2/nmp/byCnks",
            objectMapper.writeValueAsString(ListOfIdsDto( ids = listOf(cnk) )),
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<List<NmpDto>>(responseString)

        response.onEach {
            createdAmps shouldContain it.id
            it.code shouldBe cnk
        }.size shouldBe size
    }

    "$apiVersion - An HCP is provided an empty result if no Nmp matching the cnk is found" {
        nmpDAO.save(List(10) {
            Nmp(
                id = uuid(),
                code = uuid()
            )
        })

        val responseString = httpClient.makePostRequest(
            "$samUrl/rest/$apiVersion/be_samv2/nmp/byCnks",
            objectMapper.writeValueAsString(ListOfIdsDto( ids = List(10) { uuid() } )),
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<List<NmpDto>>(responseString)

        response.size shouldBe 0
    }

    "$apiVersion - A Patient cannot get Nmps by cnk" {
        httpClient.makePostRequest(
            "$samUrl/rest/$apiVersion/be_samv2/nmp/byCnks",
            objectMapper.writeValueAsString(ListOfIdsDto( ids = List(10) { uuid() } )),
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.PATIENT).authJWT!!}"),
            403
        )
    }

}


private fun StringSpec.findVmpGroupsByCodeE2ETest(
    httpClient: TestHttpClient,
    credentialsProvider: SAMV2ControllerE2ETest.CredentialsProvider,
    objectMapper: ObjectMapper,
    vmpGroupDAO: VmpGroupDAO,
    apiVersion: String,
    samUrl: String
) {

    "$apiVersion - An HCP can get Vmp groups by code" {
        val groupCode = uuid()
        val size = 10
        val createdAmps = vmpGroupDAO.save(List(size) {
            VmpGroup(
                id = uuid(),
                code = groupCode,
                name = SamText(en = uuid(), fr = uuid())
            )
        }).mapNotNull { it.id }.toList()
        createdAmps.size shouldBe size

        vmpGroupDAO.save(List(size) {
            VmpGroup(
                id = uuid(),
                code = uuid(),
                name = SamText(en = uuid(), fr = uuid())
            )
        })

        val responseString = httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/vmpgroup/byGroupCode/${groupCode}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<PaginatedList<VmpGroupDto>>(responseString)

        response.rows.onEach {
            createdAmps shouldContain it.id
            it.code shouldBe groupCode
        }.size shouldBe size
    }

    "$apiVersion - An HCP can get Vmp groups by code using POST" {
        val size = 10
        val groupsToCreate = List(size) {
            VmpGroup(
                id = uuid(),
                code = uuid(),
                name = SamText(en = uuid(), fr = uuid())
            )
        }
        val createdAmps = vmpGroupDAO.save(groupsToCreate).mapNotNull { it.id }.toList()
        createdAmps.size shouldBe size
        val groupCodes = groupsToCreate.map { it.code!! }

        vmpGroupDAO.save(List(size) {
            VmpGroup(
                id = uuid(),
                code = uuid(),
                name = SamText(en = uuid(), fr = uuid())
            )
        })

        val responseString = httpClient.makePostRequest(
            "$samUrl/rest/$apiVersion/be_samv2/vmpgroup/byGroupCodes",
            objectMapper.writeValueAsString(ListOfIdsDto( ids = groupCodes )),
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<List<VmpGroupDto>>(responseString)

        response.onEach {
            createdAmps shouldContain it.id
            groupCodes shouldContain it.code
        }.size shouldBe size
    }

    "$apiVersion - An HCP is provided an empty result if no Vmp group matching the code is found" {
        vmpGroupDAO.save(List(10) {
            VmpGroup(
                id = uuid(),
                code = uuid(),
                name = SamText(en = uuid(), fr = uuid())
            )
        })

        val responseString = httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/vmpgroup/byGroupCode/${uuid()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<PaginatedList<VmpGroupDto>>(responseString)

        response.rows.size shouldBe 0
    }

    "$apiVersion - An HCP is provided an empty result if no Vmp group matching the code is found with POST" {
        vmpGroupDAO.save(List(10) {
            VmpGroup(
                id = uuid(),
                code = uuid(),
                name = SamText(en = uuid(), fr = uuid())
            )
        })

        val responseString = httpClient.makePostRequest(
            "$samUrl/rest/$apiVersion/be_samv2/vmpgroup/byGroupCodes",
            objectMapper.writeValueAsString(ListOfIdsDto( ids = List(10) { uuid() } )),
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<List<VmpGroupDto>>(responseString)

        response.size shouldBe 0
    }

    "$apiVersion - A Patient cannot get Vmp groups by code" {
        httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/vmpgroup/byGroupCode/${uuid()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.PATIENT).authJWT!!}"),
            403
        )
    }

    "$apiVersion - A Patient cannot get Vmp groups by code with POST" {
        httpClient.makePostRequest(
            "$samUrl/rest/$apiVersion/be_samv2/vmpgroup/byGroupCodes",
            objectMapper.writeValueAsString(ListOfIdsDto( ids = List(10) { uuid() } )),
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.PATIENT).authJWT!!}"),
            403
        )
    }

}

private fun StringSpec.findVmpGroupsByLabelE2ETest(
    httpClient: TestHttpClient,
    credentialsProvider: SAMV2ControllerE2ETest.CredentialsProvider,
    objectMapper: ObjectMapper,
    vmpGroupDAO: VmpGroupDAO,
    apiVersion: String,
    samUrl: String
) {

    "$apiVersion - An HCP can get Vmp groups by label" {
        val enLabel = uuid().split("-")
        val frLabel = uuid().split("-")
        val size = 10
        val createdAmps = vmpGroupDAO.save(List(size) {
            VmpGroup(
                id = uuid(),
                name = SamText(
                    en = enLabel.joinToString(" "),
                    fr = frLabel.joinToString(" ")
                )
            )
        }).mapNotNull { it.id }.toList()
        createdAmps.size shouldBe size

        vmpGroupDAO.save(List(size) {
            VmpGroup(
                id = uuid(),
                name = SamText(
                    en = uuid(),
                    fr = uuid()
                )
            )
        })

        val responseString = httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/vmpgroup?language=en&label=${enLabel.first()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<PaginatedList<VmpGroupDto>>(responseString)

        response.rows.onEach {
            createdAmps shouldContain it.id
            it.name?.en shouldBe enLabel.joinToString(" ")
        }.size shouldBe size
    }

    "$apiVersion - An HCP is provided an empty result if no Vmp group matching the label is found" {
        vmpGroupDAO.save(List(10) {
            VmpGroup(
                id = uuid(),
                name = SamText(
                    en = uuid(),
                    fr = uuid()
                )
            )
        })

        val responseString = httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/vmpgroup?language=en&label=${uuid()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<PaginatedList<VmpGroupDto>>(responseString)

        response.rows.size shouldBe 0
    }

    "$apiVersion - A Patient cannot get Vmp groups by label" {
        httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/vmpgroup?language=en&label=${uuid()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.PATIENT).authJWT!!}"),
            403
        )
    }

}

private fun StringSpec.findAmpsByDmppE2ETest(
    httpClient: TestHttpClient,
    credentialsProvider: SAMV2ControllerE2ETest.CredentialsProvider,
    objectMapper: ObjectMapper,
    ampDAO: AmpDAO,
    apiVersion: String,
    samUrl: String
) {

    "$apiVersion - An HCP can get Amps by Dmpp code" {
        val dmppCode = uuid()
        val size = 10
        val createdAmps = ampDAO.save(List(size) {
            Amp(
                id = uuid(),
                name = SamText(en = uuid()),
                ampps = setOf(
                    Ampp(
                        dmpps = setOf(Dmpp(code = dmppCode ))
                    )
                )
            )
        }).mapNotNull { it.id }.toList()
        createdAmps.size shouldBe size

        ampDAO.save(List(size) {
            Amp(
                id = uuid(),
                name = SamText(en = uuid()),
                ampps = setOf(
                    Ampp(
                        dmpps = setOf(Dmpp(code = uuid()))
                    )
                )
            )
        })

        val responseString = httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byDmppCode/${dmppCode}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<List<AmpDto>>(responseString)

        response.onEach {
            createdAmps shouldContain it.id
            it.ampps.firstOrNull()?.dmpps?.firstOrNull()?.code shouldBe dmppCode
        }.size shouldBe size
    }

    "$apiVersion - An HCP can get Amps by Dmpp code using POST" {
        val size = 10
        val ampsToCreate = List(size) {
            Amp(
                id = uuid(),
                name = SamText(en = uuid()),
                ampps = setOf(
                    Ampp(
                        dmpps = setOf(Dmpp(code = uuid()))
                    )
                )
            )
        }
        val createdAmps = ampDAO.save(ampsToCreate).mapNotNull { it.id }.toList()
        createdAmps.size shouldBe size
        val dmpCodes = ampsToCreate.flatMap { it.ampps.flatMap { a -> a.dmpps.map { d -> d.code!! } } }

        ampDAO.save(List(size) {
            Amp(
                id = uuid(),
                name = SamText(en = uuid()),
                ampps = setOf(
                    Ampp(
                        dmpps = setOf(Dmpp(code = uuid()))
                    )
                )
            )
        })

        val responseString = httpClient.makePostRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byDmppCodes",
            objectMapper.writeValueAsString(ListOfIdsDto(ids = dmpCodes)),
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<List<AmpDto>>(responseString)

        response.onEach {
            createdAmps shouldContain it.id
            dmpCodes shouldContain it.ampps.firstOrNull()?.dmpps?.firstOrNull()?.code
        }.size shouldBe size
    }

    "$apiVersion - An HCP is provided an empty result if no Amp matching the Dmpp code is found" {
        ampDAO.save(List(10) {
            Amp(
                id = uuid(),
                name = SamText(en = uuid()),
                ampps = setOf(
                    Ampp(
                        dmpps = setOf(Dmpp(code = uuid() ))
                    )
                )
            )
        })

        val responseString = httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byDmppCode/${uuid()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<List<VmpDto>>(responseString)

        response.size shouldBe 0
    }

    "$apiVersion - An HCP is provided an empty result if no Amp matching the Dmpp code is found with POST" {
        ampDAO.save(List(10) {
            Amp(
                id = uuid(),
                name = SamText(en = uuid()),
                ampps = setOf(
                    Ampp(
                        dmpps = setOf(Dmpp(code = uuid() ))
                    )
                )
            )
        })

        val responseString = httpClient.makePostRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byDmppCodes",
            objectMapper.writeValueAsString(ListOfIdsDto(ids = List(10) { uuid() })),
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<List<VmpDto>>(responseString)

        response.size shouldBe 0
    }

    "$apiVersion - A Patient cannot get Amp by Dmpp code" {
        httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byDmppCode/${uuid()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.PATIENT).authJWT!!}"),
            403
        )
    }

    "$apiVersion - A Patient cannot get Amp by Dmpp code with POST" {
        httpClient.makePostRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byDmppCodes",
            objectMapper.writeValueAsString(ListOfIdsDto(ids = List(10) { uuid() })),
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.PATIENT).authJWT!!}"),
            403
        )
    }

}

private fun StringSpec.findAmpsByVmpIdE2ETest(
    httpClient: TestHttpClient,
    credentialsProvider: SAMV2ControllerE2ETest.CredentialsProvider,
    objectMapper: ObjectMapper,
    ampDAO: AmpDAO,
    apiVersion: String,
    samUrl: String
) {

    "$apiVersion - An HCP can get Amps by Vmp id" {
        val vmpId = uuid()
        val size = 10
        val createdAmps = ampDAO.save(List(size) {
            Amp(
                id = uuid(),
                vmp = VmpStub(
                    id = vmpId,
                    code = uuid()
                ),
                name = SamText(en = uuid())
            )
        }).mapNotNull { it.id }.toList()
        createdAmps.size shouldBe size

        ampDAO.save(List(size) {
            Amp(
                id = uuid(),
                vmp = VmpStub(
                    id = uuid(),
                    code = uuid()
                ),
                name = SamText(en = uuid())
            )
        })

        val responseString = httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byVmpId/${vmpId}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<PaginatedList<AmpDto>>(responseString)

        response.rows.onEach {
            createdAmps shouldContain it.id
            it.vmp?.id shouldBe vmpId
        }.size shouldBe size
    }

    "$apiVersion - An HCP can get Amps by Vmp ids using POST" {
        val size = 10
        val ampsToCreate = List(size) {
            Amp(
                id = uuid(),
                vmp = VmpStub(
                    id = uuid(),
                    code = uuid()
                ),
                name = SamText(en = uuid())
            )
        }
        val createdAmps = ampDAO.save(ampsToCreate).mapNotNull { it.id }.toList()
        createdAmps.size shouldBe size
        val vmpIds = ampsToCreate.map { it.vmp!!.id }

        ampDAO.save(List(size) {
            Amp(
                id = uuid(),
                vmp = VmpStub(
                    id = uuid(),
                    code = uuid()
                ),
                name = SamText(en = uuid())
            )
        })

        val responseString = httpClient.makePostRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byVmpIds",
            objectMapper.writeValueAsString(ListOfIdsDto(ids = vmpIds)),
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<List<AmpDto>>(responseString)

        response.onEach {
            createdAmps shouldContain it.id
            vmpIds shouldContain it.vmp?.id
        }.size shouldBe size
    }

    "$apiVersion - An HCP is provided an empty result if no Amp matching the Vmp id is found" {
        ampDAO.save(List(10) {
            Amp(
                id = uuid(),
                vmp = VmpStub(
                    id = uuid(),
                    code = uuid()
                ),
                name = SamText(en = uuid())
            )
        })

        val responseString = httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byVmpId/${uuid()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<PaginatedList<VmpDto>>(responseString)

        response.rows.size shouldBe 0
    }

    "$apiVersion - An HCP is provided an empty result if no Amp matching the Vmp id is found using POST" {
        ampDAO.save(List(10) {
            Amp(
                id = uuid(),
                vmp = VmpStub(
                    id = uuid(),
                    code = uuid()
                ),
                name = SamText(en = uuid())
            )
        })

        val responseString = httpClient.makePostRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byVmpIds",
            objectMapper.writeValueAsString(ListOfIdsDto(ids = List(10) { uuid() })),
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<List<VmpDto>>(responseString)

        response.size shouldBe 0
    }

    "$apiVersion - A Patient cannot get Amp by Vmp id" {
        httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byVmpId/${uuid()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.PATIENT).authJWT!!}"),
            403
        )
    }

    "$apiVersion - A Patient cannot get Amp by Vmp id using POST" {
        httpClient.makePostRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byVmpIds",
            objectMapper.writeValueAsString(ListOfIdsDto( ids = List(10) { uuid() } )),
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.PATIENT).authJWT!!}"),
            403
        )
    }
}

private fun StringSpec.findAmpsByAtcE2ETest(
    httpClient: TestHttpClient,
    credentialsProvider: SAMV2ControllerE2ETest.CredentialsProvider,
    objectMapper: ObjectMapper,
    ampDAO: AmpDAO,
    apiVersion: String,
    samUrl: String
) {

    "$apiVersion - An HCP can get Amps by Atc" {
        val atcCode = uuid()
        val size = 10
        val createdAmps = ampDAO.save(List(size) {
            Amp(
                id = uuid(),
                name = SamText(en = uuid()),
                ampps = setOf(
                    Ampp(
                        atcs = setOf(Atc(code = atcCode ))
                    )
                )
            )
        }).mapNotNull { it.id }.toList()
        createdAmps.size shouldBe size

        ampDAO.save(List(size) {
            Amp(
                id = uuid(),
                name = SamText(en = uuid()),
                ampps = setOf(
                    Ampp(
                        atcs = setOf(Atc(code = uuid() ))
                    )
                )
            )
        })

        val responseString = httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byAtc/${atcCode}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<PaginatedList<AmpDto>>(responseString)

        response.rows.onEach {
            createdAmps shouldContain it.id
            it.ampps.firstOrNull()?.atcs?.firstOrNull()?.code shouldBe atcCode
        }.size shouldBe size
    }

    "$apiVersion - An HCP is provided an empty result if no Amp matching the Atc is found" {
        ampDAO.save(List(10) {
            Amp(
                id = uuid(),
                name = SamText(en = uuid()),
                ampps = setOf(
                    Ampp(
                        atcs = setOf(Atc(code = uuid() ))
                    )
                )
            )
        })

        val responseString = httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byAtc/${uuid()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<PaginatedList<VmpDto>>(responseString)

        response.rows.size shouldBe 0
    }

    "$apiVersion - A Patient cannot get Amp by Atc" {
        httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/vmp/byAtc/${uuid()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.PATIENT).authJWT!!}"),
            403
        )
    }

}

private fun StringSpec.findAmpsByVmpCodeE2ETest(
    httpClient: TestHttpClient,
    credentialsProvider: SAMV2ControllerE2ETest.CredentialsProvider,
    objectMapper: ObjectMapper,
    ampDAO: AmpDAO,
    apiVersion: String,
    samUrl: String
) {

    "$apiVersion - An HCP can get Amps by Vmp code" {
        val vmpCode = uuid()
        val size = 10
        val createdAmps = ampDAO.save(List(size) {
            Amp(
                id = uuid(),
                vmp = VmpStub(
                    id = uuid(),
                    code = vmpCode
                ),
                name = SamText(en = uuid())
            )
        }).mapNotNull { it.id }.toList()
        createdAmps.size shouldBe size

        ampDAO.save(List(size) {
            Amp(
                id = uuid(),
                vmp = VmpStub(
                    id = uuid(),
                    code = uuid()
                ),
                name = SamText(en = uuid())
            )
        })

        val responseString = httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byVmpCode/${vmpCode}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<PaginatedList<AmpDto>>(responseString)

        response.rows.onEach {
            createdAmps shouldContain it.id
            it.vmp?.code shouldBe vmpCode
        }.size shouldBe size
    }

    "$apiVersion - An HCP can get Amps by Vmp codes using POST" {
        val size = 10
        val ampsToCreate = List(size) {
            Amp(
                id = uuid(),
                vmp = VmpStub(
                    id = uuid(),
                    code = uuid()
                ),
                name = SamText(en = uuid())
            )
        }
        val createdAmps = ampDAO.save(ampsToCreate).mapNotNull { it.id }.toList()
        createdAmps.size shouldBe size
        val vmpCodes = ampsToCreate.map { it.vmp!!.code!! }

        ampDAO.save(List(size) {
            Amp(
                id = uuid(),
                vmp = VmpStub(
                    id = uuid(),
                    code = uuid()
                ),
                name = SamText(en = uuid())
            )
        })

        val responseString = httpClient.makePostRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byVmpCodes",
            objectMapper.writeValueAsString(ListOfIdsDto(ids = vmpCodes)),
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<List<AmpDto>>(responseString)

        response.onEach {
            createdAmps shouldContain it.id
            vmpCodes shouldContain it.vmp?.code
        }.size shouldBe size
    }

    "$apiVersion - An HCP is provided an empty result if no Amp matching the Vmp code is found" {
        ampDAO.save(List(10) {
            Amp(
                id = uuid(),
                vmp = VmpStub(
                    id = uuid(),
                    code = uuid()
                ),
                name = SamText(en = uuid())
            )
        })

        val responseString = httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byVmpCode/${uuid()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<PaginatedList<VmpDto>>(responseString)

        response.rows.size shouldBe 0
    }

    "$apiVersion - An HCP is provided an empty result if no Amp matching the Vmp code is found using POST" {
        ampDAO.save(List(10) {
            Amp(
                id = uuid(),
                vmp = VmpStub(
                    id = uuid(),
                    code = uuid()
                ),
                name = SamText(en = uuid())
            )
        })

        val responseString = httpClient.makePostRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byVmpCodes",
            objectMapper.writeValueAsString(ListOfIdsDto(ids = List(10) { uuid() })),
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<List<VmpDto>>(responseString)

        response.size shouldBe 0
    }

    "$apiVersion - A Patient cannot get Amp by Vmp code" {
        httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byVmpCode/${uuid()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.PATIENT).authJWT!!}"),
            403
        )
    }

    "$apiVersion - A Patient cannot get Amp by Vmp code using POST" {
        httpClient.makePostRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byVmpCodes",
            objectMapper.writeValueAsString(ListOfIdsDto(ids = List(10) { uuid() })),
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.PATIENT).authJWT!!}"),
            403
        )
    }

}

private fun StringSpec.findAmpsByVmpGroupIdE2ETest(
    httpClient: TestHttpClient,
    credentialsProvider: SAMV2ControllerE2ETest.CredentialsProvider,
    objectMapper: ObjectMapper,
    ampDAO: AmpDAO,
    apiVersion: String,
    samUrl: String
) {

    "$apiVersion - An HCP can get Amps by Vmp group id" {
        val groupId = uuid()
        val size = 10
        val createdAmps = ampDAO.save(List(size) {
            Amp(
                id = uuid(),
                vmp = VmpStub(
                    id = uuid(),
                    vmpGroup = VmpGroupStub(
                        id = groupId,
                        code = uuid()
                    )
                ),
                name = SamText(en = uuid())
            )
        }).mapNotNull { it.id }.toList()
        createdAmps.size shouldBe size

        ampDAO.save(List(size) {
            Amp(
                id = uuid(),
                vmp = VmpStub(
                    id = uuid(),
                    vmpGroup = VmpGroupStub(
                        id = uuid(),
                        code = uuid()
                    )
                ),
                name = SamText(en = uuid())
            )
        })

        val responseString = httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byGroupId/${groupId}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<PaginatedList<AmpDto>>(responseString)

        response.rows.onEach {
            createdAmps shouldContain it.id
            it.vmp?.vmpGroup?.id shouldBe groupId
        }.size shouldBe size
    }

    "$apiVersion - An HCP can get Amps by Vmp group id using POST" {
        val size = 10
        val ampsToCreate = List(size) {
            Amp(
                id = uuid(),
                vmp = VmpStub(
                    id = uuid(),
                    vmpGroup = VmpGroupStub(
                        id = uuid(),
                        code = uuid()
                    )
                ),
                name = SamText(en = uuid())
            )
        }
        val createdAmps = ampDAO.save(ampsToCreate).mapNotNull { it.id }.toList()
        createdAmps.size shouldBe size

        ampDAO.save(List(size) {
            Amp(
                id = uuid(),
                vmp = VmpStub(
                    id = uuid(),
                    vmpGroup = VmpGroupStub(
                        id = uuid(),
                        code = uuid()
                    )
                ),
                name = SamText(en = uuid())
            )
        })

        val responseString = httpClient.makePostRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byGroupIds",
            objectMapper.writeValueAsString(ListOfIdsDto(ids = ampsToCreate.map { it.vmp!!.vmpGroup!!.id })),
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<List<AmpDto>>(responseString)

        response.onEach {
            createdAmps shouldContain it.id
            ampsToCreate.map { a -> a.vmp!!.vmpGroup!!.id } shouldContain it.vmp?.vmpGroup?.id
        }.size shouldBe size
    }

    "$apiVersion - An HCP is provided an empty result if no Amp matching the Vmp group id is found" {
        ampDAO.save(List(10) {
            Amp(
                id = uuid(),
                vmp = VmpStub(
                    id = uuid(),
                    vmpGroup = VmpGroupStub(
                        id = uuid(),
                        code = uuid()
                    )
                ),
                name = SamText(en = uuid())
            )
        })

        val responseString = httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byGroupId/${uuid()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<PaginatedList<VmpDto>>(responseString)

        response.rows.size shouldBe 0
    }

    "$apiVersion - An HCP is provided an empty result if no Amp matching the Vmp group id is found using POST" {
        ampDAO.save(List(10) {
            Amp(
                id = uuid(),
                vmp = VmpStub(
                    id = uuid(),
                    vmpGroup = VmpGroupStub(
                        id = uuid(),
                        code = uuid()
                    )
                ),
                name = SamText(en = uuid())
            )
        })

        val responseString = httpClient.makePostRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byGroupIds",
            objectMapper.writeValueAsString(ListOfIdsDto( ids = List(10) { uuid() } )),
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<List<VmpDto>>(responseString)

        response.size shouldBe 0
    }

    "$apiVersion - A Patient cannot get Amp by Vmp group id" {
        httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byGroupId/${uuid()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.PATIENT).authJWT!!}"),
            403
        )
    }

    "$apiVersion - A Patient cannot get Amp by Vmp group id using POST" {
        httpClient.makePostRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byGroupIds",
            objectMapper.writeValueAsString(ListOfIdsDto( ids = List(10) { uuid() } )),
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.PATIENT).authJWT!!}"),
            403
        )
    }

}

private fun StringSpec.findAmpsByVmpGroupCodeE2ETest(
    httpClient: TestHttpClient,
    credentialsProvider: SAMV2ControllerE2ETest.CredentialsProvider,
    objectMapper: ObjectMapper,
    ampDAO: AmpDAO,
    apiVersion: String,
    samUrl: String
) {

    "$apiVersion - An HCP can get Amps by Vmp group code" {
        val groupCode = uuid()
        val size = 10
        val createdAmps = ampDAO.save(List(size) {
            Amp(
                id = uuid(),
                vmp = VmpStub(
                    id = uuid(),
                    vmpGroup = VmpGroupStub(
                        id = uuid(),
                        code = groupCode
                    )
                ),
                name = SamText(en = uuid())
            )
        }).mapNotNull { it.id }.toList()
        createdAmps.size shouldBe size

        ampDAO.save(List(size) {
            Amp(
                id = uuid(),
                vmp = VmpStub(
                    id = uuid(),
                    vmpGroup = VmpGroupStub(
                        id = uuid(),
                        code = uuid()
                    )
                ),
                name = SamText(en = uuid())
            )
        })

        val responseString = httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byGroupCode/${groupCode}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<PaginatedList<AmpDto>>(responseString)

        response.rows.onEach {
            createdAmps shouldContain it.id
            it.vmp?.vmpGroup?.code shouldBe groupCode
        }.size shouldBe size
    }

    "$apiVersion - An HCP can get Amps by Vmp group code using POST" {
        val size = 10
        val ampsToCreate = List(size) {
            Amp(
                id = uuid(),
                vmp = VmpStub(
                    id = uuid(),
                    vmpGroup = VmpGroupStub(
                        id = uuid(),
                        code = uuid()
                    )
                ),
                name = SamText(en = uuid())
            )
        }
        val createdAmps = ampDAO.save(ampsToCreate).mapNotNull { it.id }.toList()
        createdAmps.size shouldBe size

        ampDAO.save(List(size) {
            Amp(
                id = uuid(),
                vmp = VmpStub(
                    id = uuid(),
                    vmpGroup = VmpGroupStub(
                        id = uuid(),
                        code = uuid()
                    )
                ),
                name = SamText(en = uuid())
            )
        })

        val responseString = httpClient.makePostRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byGroupCodes",
            objectMapper.writeValueAsString(ListOfIdsDto(ids = ampsToCreate.map { it.vmp!!.vmpGroup!!.code!! })),
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<List<AmpDto>>(responseString)

        response.onEach {
            createdAmps shouldContain it.id
            ampsToCreate.map { a -> a.vmp!!.vmpGroup!!.code!! } shouldContain it.vmp?.vmpGroup?.code
        }.size shouldBe size
    }

    "$apiVersion - An HCP is provided an empty result if no Amp matching the Vmp group code is found" {
        ampDAO.save(List(10) {
            Amp(
                id = uuid(),
                vmp = VmpStub(
                    id = uuid(),
                    vmpGroup = VmpGroupStub(
                        id = uuid(),
                        code = uuid()
                    )
                ),
                name = SamText(en = uuid())
            )
        })

        val responseString = httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byGroupCode/${uuid()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<PaginatedList<VmpDto>>(responseString)

        response.rows.size shouldBe 0
    }

    "$apiVersion - An HCP is provided an empty result if no Amp matching the Vmp group code is found using POST" {
        ampDAO.save(List(10) {
            Amp(
                id = uuid(),
                vmp = VmpStub(
                    id = uuid(),
                    vmpGroup = VmpGroupStub(
                        id = uuid(),
                        code = uuid()
                    )
                ),
                name = SamText(en = uuid())
            )
        })

        val responseString = httpClient.makePostRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byGroupCodes",
            objectMapper.writeValueAsString(ListOfIdsDto( ids = List(10) { uuid() } )),
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<List<VmpDto>>(responseString)

        response.size shouldBe 0
    }

    "$apiVersion - A Patient cannot get Amp by Vmp group code" {
        httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byGroupCode/${uuid()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.PATIENT).authJWT!!}"),
            403
        )
    }

    "$apiVersion - A Patient cannot get Amp by Vmp group code using POST" {
        httpClient.makePostRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp/byGroupCodes",
            objectMapper.writeValueAsString(ListOfIdsDto( ids = List(10) { uuid() } )),
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.PATIENT).authJWT!!}"),
            403
        )
    }

}

private fun StringSpec.findVmpsByGroupIdE2ETest(
    httpClient: TestHttpClient,
    credentialsProvider: SAMV2ControllerE2ETest.CredentialsProvider,
    objectMapper: ObjectMapper,
    vmpDAO: VmpDAO,
    apiVersion: String,
    samUrl: String
) {

    "$apiVersion - An HCP can get Vmps by group id" {
        val groupId = uuid()
        val size = 10
        val createdAmps = vmpDAO.save(List(size) {
            Vmp(
                id = uuid(),
                vmpGroup = VmpGroupStub(
                    id = groupId,
                    code = uuid()
                ),
                name = SamText(en = uuid())
            )
        }).mapNotNull { it.id }.toList()
        createdAmps.size shouldBe size

        vmpDAO.save(List(size) {
            Vmp(
                id = uuid(),
                vmpGroup = VmpGroupStub(
                    id = uuid(),
                    code = uuid()
                ),
                name = SamText(en = uuid())
            )
        })

        val responseString = httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/vmp/byGroupId/${groupId}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<PaginatedList<VmpDto>>(responseString)

        response.rows.onEach {
            createdAmps shouldContain it.id
            it.vmpGroup?.id shouldBe groupId
        }.size shouldBe size
    }

    "$apiVersion - An HCP can get Vmps by group id using POST" {
        val size = 10
        val vmpToCreate = List(size) {
            Vmp(
                id = uuid(),
                vmpGroup = VmpGroupStub(
                    id = uuid(),
                    code = uuid()
                ),
                name = SamText(en = uuid())
            )
        }
        val createdAmps = vmpDAO.save(vmpToCreate).mapNotNull { it.id }.toList()
        createdAmps.size shouldBe size

        vmpDAO.save(List(size) {
            Vmp(
                id = uuid(),
                vmpGroup = VmpGroupStub(
                    id = uuid(),
                    code = uuid()
                ),
                name = SamText(en = uuid())
            )
        })

        val responseString = httpClient.makePostRequest(
            "$samUrl/rest/$apiVersion/be_samv2/vmp/byGroupIds",
            objectMapper.writeValueAsString(ListOfIdsDto(ids = vmpToCreate.map { it.vmpGroup!!.id })),
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<List<VmpDto>>(responseString)

        response.onEach {
            createdAmps shouldContain it.id
            vmpToCreate.map { v -> v.vmpGroup!!.id } shouldContain it.vmpGroup?.id
        }.size shouldBe size
    }

    "$apiVersion - An HCP cis provided an empty result if no Vmp matching the group id is found" {
        vmpDAO.save(List(10) {
            Vmp(
                id = uuid(),
                vmpGroup = VmpGroupStub(
                    id = uuid(),
                    code = uuid()
                ),
                name = SamText(en = uuid())
            )
        })

        val responseString = httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/vmp/byGroupId/${uuid()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<PaginatedList<VmpDto>>(responseString)

        response.rows.size shouldBe 0
    }

    "$apiVersion - An HCP cis provided an empty result if no Vmp matching the group id is found with POST" {
        vmpDAO.save(List(10) {
            Vmp(
                id = uuid(),
                vmpGroup = VmpGroupStub(
                    id = uuid(),
                    code = uuid()
                ),
                name = SamText(en = uuid())
            )
        })

        val responseString = httpClient.makePostRequest(
            "$samUrl/rest/$apiVersion/be_samv2/vmp/byGroupIds",
            objectMapper.writeValueAsString(ListOfIdsDto( ids = List(10) { uuid() } )),
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<List<VmpDto>>(responseString)

        response.size shouldBe 0
    }

    "$apiVersion - A Patient cannot get Vmps by group id" {
        httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/vmp/byGroupId/${uuid()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.PATIENT).authJWT!!}"),
            403
        )
    }

    "$apiVersion - A Patient cannot get Vmps by group id using POST" {
        httpClient.makePostRequest(
            "$samUrl/rest/$apiVersion/be_samv2/vmp/byGroupIds",
            objectMapper.writeValueAsString(ListOfIdsDto( ids = List(10) { uuid() } )),
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.PATIENT).authJWT!!}"),
            403
        )
    }

}

private fun StringSpec.findNmpsByLabelE2ETest(
    httpClient: TestHttpClient,
    credentialsProvider: SAMV2ControllerE2ETest.CredentialsProvider,
    objectMapper: ObjectMapper,
    nmpDAO: NmpDAO,
    apiVersion: String,
    samUrl: String
) {

    "$apiVersion - An HCP can get Nmps by label" {
        val enLabel = uuid().split("-")
        val frLabel = uuid().split("-")
        val size = 10
        val createdAmps = nmpDAO.save(List(size) {
            Nmp(
                id = uuid(),
                name = SamText(
                    en = enLabel.joinToString(" "),
                    fr = frLabel.joinToString(" ")
                )
            )
        }).mapNotNull { it.id }.toList()
        createdAmps.size shouldBe size

        nmpDAO.save(List(size) {
            Nmp(
                id = uuid(),
                name = SamText(
                    en = uuid(),
                    fr = uuid()
                )
            )
        })

        val responseString = httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/nmp?language=en&label=${enLabel.first()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<PaginatedList<VmpDto>>(responseString)

        response.rows.onEach {
            createdAmps shouldContain it.id
            it.name?.en shouldBe enLabel.joinToString(" ")
        }.size shouldBe size
    }

    "$apiVersion - An HCP is provided an empty result if no Nmp matching the label is found" {
        nmpDAO.save(List(10) {
            Nmp(
                id = uuid(),
                name = SamText(
                    en = uuid(),
                    fr = uuid()
                )
            )
        })

        val responseString = httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/nmp?language=en&label=${uuid()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<PaginatedList<VmpDto>>(responseString)

        response.rows.size shouldBe 0
    }

    "$apiVersion - A Patient cannot get Nmps by label" {
        httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/nmp?language=en&label=${uuid()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.PATIENT).authJWT!!}"),
            403
        )
    }

}

private fun StringSpec.findVmpsByCodeE2ETest(
    httpClient: TestHttpClient,
    credentialsProvider: SAMV2ControllerE2ETest.CredentialsProvider,
    objectMapper: ObjectMapper,
    vmpDAO: VmpDAO,
    apiVersion: String,
    samUrl: String
) {

    "$apiVersion - An HCP can get Vmps by code" {
        val code = uuid()
        val size = 10
        val createdAmps = vmpDAO.save(List(size) {
            Vmp(
                id = uuid(),
                code = code,
                name = SamText(en = uuid())
            )
        }).mapNotNull { it.id }.toList()
        createdAmps.size shouldBe size

        vmpDAO.save(List(size) {
            Vmp(
                id = uuid(),
                code = uuid(),
                name = SamText(en = uuid())
            )
        })

        val responseString = httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/vmp/byVmpCode/${code}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<PaginatedList<VmpDto>>(responseString)

        response.rows.onEach {
            createdAmps shouldContain it.id
            it.code shouldBe code
        }.size shouldBe size
    }

    "$apiVersion - An HCP can get Vmps by code using a POST request" {
        val size = 10
        val vmpToCreate = List(size) {
            Vmp(
                id = uuid(),
                code = uuid(),
                name = SamText(en = uuid())
            )
        }
        val createdVmps = vmpDAO.save(vmpToCreate).mapNotNull { it.id }.toList()
        createdVmps.size shouldBe size

        vmpDAO.save(List(size) {
            Vmp(
                id = uuid(),
                code = uuid(),
                name = SamText(en = uuid())
            )
        })

        val responseString = httpClient.makePostRequest(
            "$samUrl/rest/$apiVersion/be_samv2/vmp/byVmpCodes",
            objectMapper.writeValueAsString(ListOfIdsDto(ids = vmpToCreate.map { it.code!! })),
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<List<VmpDto>>(responseString)

        response.onEach {
            createdVmps shouldContain it.id
            vmpToCreate.map { c -> c.code!! } shouldContain it.code
        }.size shouldBe size
    }

    "$apiVersion - An HCP is provided an empty result if no Vmp matching the code is found" {
        vmpDAO.save(List(10) {
            Vmp(
                id = uuid(),
                code = uuid(),
                name = SamText(en = uuid())
            )
        })

        val responseString = httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/vmp/byVmpCode/${uuid()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<PaginatedList<VmpDto>>(responseString)

        response.rows.size shouldBe 0
    }

    "$apiVersion - An HCP is provided an empty result if no Vmp matching the code is found with POST" {
        vmpDAO.save(List(10) {
            Vmp(
                id = uuid(),
                code = uuid(),
                name = SamText(en = uuid())
            )
        })

        val responseString = httpClient.makePostRequest(
            "$samUrl/rest/$apiVersion/be_samv2/vmp/byVmpCodes",
            objectMapper.writeValueAsString(ListOfIdsDto( ids = List(10) { uuid() } )),
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<List<VmpDto>>(responseString)

        response.size shouldBe 0
    }

    "$apiVersion - A Patient cannot get Vmps by code" {
        httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/vmp/byVmpCode/${uuid()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.PATIENT).authJWT!!}"),
            403
        )
    }

    "$apiVersion - A Patient cannot get Vmps by code using POST" {
        httpClient.makePostRequest(
            "$samUrl/rest/$apiVersion/be_samv2/vmp/byVmpCodes",
            objectMapper.writeValueAsString(ListOfIdsDto( ids = List(10) { uuid() } )),
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.PATIENT).authJWT!!}"),
            403
        )
    }

}

private fun StringSpec.findVmpsByGroupCodeE2ETest(
    httpClient: TestHttpClient,
    credentialsProvider: SAMV2ControllerE2ETest.CredentialsProvider,
    objectMapper: ObjectMapper,
    vmpDAO: VmpDAO,
    apiVersion: String,
    samUrl: String
) {

    "$apiVersion - An HCP can get Vmps by group code" {
        val groupCode = uuid()
        val size = 10
        val createdAmps = vmpDAO.save(List(size) {
            Vmp(
                id = uuid(),
                vmpGroup = VmpGroupStub(
                    id = uuid(),
                    code = groupCode
                ),
                name = SamText(en = uuid())
            )
        }).mapNotNull { it.id }.toList()
        createdAmps.size shouldBe size

        vmpDAO.save(List(size) {
            Vmp(
                id = uuid(),
                vmpGroup = VmpGroupStub(
                    id = uuid(),
                    code = uuid()
                ),
                name = SamText(en = uuid())
            )
        })

        val responseString = httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/vmp/byGroupCode/${groupCode}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<PaginatedList<VmpDto>>(responseString)

        response.rows.onEach {
            createdAmps shouldContain it.id
            it.vmpGroup?.code shouldBe groupCode
        }.size shouldBe size
    }

    "$apiVersion - An HCP is provided an empty result if no Vmp matching the group code is found" {
        vmpDAO.save(List(10) {
            Vmp(
                id = uuid(),
                vmpGroup = VmpGroupStub(
                    id = uuid(),
                    code = uuid()
                ),
                name = SamText(en = uuid())
            )
        })

        val responseString = httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/vmp/byGroupCode/${uuid()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<PaginatedList<VmpDto>>(responseString)

        response.rows.size shouldBe 0
    }

    "$apiVersion - A Patient cannot get Vmps by group code" {
        httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/vmp/byGroupCode/${uuid()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.PATIENT).authJWT!!}"),
            403
        )
    }

}

private fun StringSpec.findVmpsByLabelE2ETest(
    httpClient: TestHttpClient,
    credentialsProvider: SAMV2ControllerE2ETest.CredentialsProvider,
    objectMapper: ObjectMapper,
    vmpDAO: VmpDAO,
    apiVersion: String,
    samUrl: String
) {

    "$apiVersion - An HCP can get Vmps by label" {
        val enLabel = uuid().split("-")
        val frLabel = uuid().split("-")
        val size = 10
        val createdAmps = vmpDAO.save(List(size) {
            Vmp(
                id = uuid(),
                name = SamText(
                    en = enLabel.joinToString(" "),
                    fr = frLabel.joinToString(" ")
                )
            )
        }).mapNotNull { it.id }.toList()
        createdAmps.size shouldBe size

        vmpDAO.save(List(size) {
            Vmp(
                id = uuid(),
                name = SamText(
                    en = uuid(),
                    fr = uuid()
                )
            )
        })

        val responseString = httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/vmp?language=en&label=${enLabel.first()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<PaginatedList<VmpDto>>(responseString)

        response.rows.onEach {
            createdAmps shouldContain it.id
            it.name?.en shouldBe enLabel.joinToString(" ")
        }.size shouldBe size
    }

    "$apiVersion - An HCP cis provided an empty result if no Vmp matching the label is found" {
        vmpDAO.save(List(10) {
            Vmp(
                id = uuid(),
                name = SamText(
                    en = uuid(),
                    fr = uuid()
                )
            )
        })

        val responseString = httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/vmp?language=en&label=${uuid()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<PaginatedList<VmpDto>>(responseString)

        response.rows.size shouldBe 0
    }

    "$apiVersion - A Patient cannot get Vmps by label" {
        httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/vmp?language=en&label=${uuid()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.PATIENT).authJWT!!}"),
            403
        )
    }

}

private fun StringSpec.findAmpsByLabelE2ETest(
    httpClient: TestHttpClient,
    credentialsProvider: SAMV2ControllerE2ETest.CredentialsProvider,
    objectMapper: ObjectMapper,
    ampDAO: AmpDAO,
    apiVersion: String,
    samUrl: String
) {

    "$apiVersion - An HCP can get Amps by label" {
        val enLabel = uuid().split("-")
        val frLabel = uuid().split("-")
        val size = 10
        val createdAmps = ampDAO.save(List(size) {
            Amp(
                id = uuid(),
                name = SamText(
                    en = enLabel.joinToString(" "),
                    fr = frLabel.joinToString(" ")
                )
            )
        }).mapNotNull { it.id }.toList()
        createdAmps.size shouldBe size

        ampDAO.save(List(size) {
            Amp(
                id = uuid(),
                name = SamText(
                    en = uuid(),
                    fr = uuid()
                )
            )
        })

        val responseString = httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp?language=en&label=${enLabel.first()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<PaginatedList<AmpDto>>(responseString)

        response.rows.onEach {
            createdAmps shouldContain it.id
            it.name?.en shouldBe enLabel.joinToString(" ")
        }.size shouldBe size
    }

    "$apiVersion - An HCP can get Amps by label with pagination" {
        val enLabel = uuid().split("-")
        val frLabel = uuid().split("-")
        val size = 10
        val limit = (size / 2) + 1
        val createdAmps = ampDAO.save(List(size) {
            Amp(
                id = uuid(),
                prescriptionName = SamText(
                    en = enLabel.joinToString(" "),
                    fr = frLabel.joinToString(" ")
                )
            )
        }).mapNotNull { it.id }.toList()
        createdAmps.size shouldBe size

        ampDAO.save(List(size) {
            Amp(
                id = uuid(),
                prescriptionName = SamText(
                    en = uuid(),
                    fr = uuid()
                )
            )
        })

        val responseFirstPage = httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp?language=en&label=${enLabel.first()}&limit=$limit",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        ).shouldNotBeNull().let { objectMapper.readValue<PaginatedList<AmpDto>>(it) }

        responseFirstPage.rows.onEach {
            it.prescriptionName?.en?.lowercase() shouldContain enLabel.first()
        }.size shouldBe limit

        val startDocId = responseFirstPage.nextKeyPair.shouldNotBeNull().startKeyDocId.shouldNotBeNull()
        val responseSecondPage = httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp?language=en&label=${enLabel.first()}&limit=$limit&startDocumentId=$startDocId",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        ).shouldNotBeNull().let { objectMapper.readValue<PaginatedList<AmpDto>>(it) }

        responseSecondPage.rows.onEach {
            it.prescriptionName?.en?.lowercase() shouldContain enLabel.first()
        }.size shouldBeLessThanOrEqual limit
        responseSecondPage.nextKeyPair shouldBe null
    }

    "$apiVersion - An HCP is provided an empty result if no Amp matching the label is found" {
        ampDAO.save(List(10) {
            Amp(
                id = uuid(),
                name = SamText(
                    en = uuid(),
                    fr = uuid()
                )
            )
        })

        val responseString = httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp?language=en&label=${uuid()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}")
        )
        responseString shouldNotBe null
        val response = objectMapper.readValue<PaginatedList<AmpDto>>(responseString)

        response.rows.size shouldBe 0
    }

    "$apiVersion - A Patient cannot get Amps by label" {
        httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp?language=en&label=${uuid()}",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.PATIENT).authJWT!!}"),
            403
        )
    }

    "$apiVersion - A HCP cannot get Amps if the passed label is too short" {
        httpClient.makeGetRequest(
            "$samUrl/rest/$apiVersion/be_samv2/amp?language=en&label=a",
            mapOf("Authorization" to "Bearer ${credentialsProvider.getCredentials(CredentialsType.HCP).authJWT!!}"),
            400
        )
    }

}
