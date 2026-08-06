package org.taktik.icure.sam

import com.fasterxml.jackson.databind.ObjectMapper
import io.icure.asyncjacksonhttpclient.net.web.WebClient
import io.kotest.core.spec.style.StringSpec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.taktik.couchdb.Client
import org.taktik.couchdb.ClientImpl
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.get
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.samv2.impl.AmpDAOImpl
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.samv2.Amp
import org.taktik.icure.services.external.rest.v1.mapper.samv2.AmpMapper
import org.taktik.icure.services.external.rest.v2.mapper.samv2.AmpV2Mapper
import org.taktik.icure.test.EnvironmentBootstrapper
import org.taktik.icure.test.KmehrTestApplication
import org.taktik.icure.test.fake.components.TestAmpDAO
import java.net.URI

@OptIn(ExperimentalCoroutinesApi::class)
@SpringBootTest(
	classes = [KmehrTestApplication::class],
	properties = [
		"spring.main.allow-bean-definition-overriding=true",
		"icure.couchdb.url=http://127.0.0.1:15984",
		"icure.couchdb.username=icure",
		"icure.couchdb.password=icure",
	],
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@ContextConfiguration(initializers = [EnvironmentBootstrapper::class])
@ActiveProfiles(profiles = ["sam"])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AmpMappingTest(
	httpClient: WebClient,
	objectMapper: ObjectMapper,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	designDocumentProvider: DesignDocumentProvider,
	private val ampV1Mapper: AmpMapper,
	private val ampV2Mapper: AmpV2Mapper,
) : StringSpec() {
	private val drugsDb = "icure-__-drugs-qa"

	init {
		val remoteDrugDbUrl = System.getenv("REMOTE_DRUG_DB_URL")
		val drugUsername = System.getenv("DRUG_DB_USERNAME")
		val drugPwd = System.getenv("DRUG_DB_PWD")
		if (remoteDrugDbUrl != null && drugUsername != null && drugPwd != null) {
			val dispatcher =
				object : CouchDbDispatcher {
					override suspend fun getClient(
						datastoreInformation: IDatastoreInformation,
						retry: Int,
					): Client =
						ClientImpl(
							httpClient = httpClient,
							couchDBUri = URI(remoteDrugDbUrl),
							dbName = drugsDb,
							objectMapper = objectMapper,
							username = drugUsername,
							password = drugPwd,
						)
				}
			val client =
				runBlocking {
					dispatcher.getClient(datastoreInstanceProvider.getInstanceAndGroup())
				}
			val credentialsAreValid =
				try {
					runBlocking {
						client.get<Amp>("some-id")
					}
					true
				} catch (_: Exception) {
					false
				}
			if (credentialsAreValid) {
				val ampDAO = AmpDAOImpl(
					couchDbDispatcher = dispatcher,
					datastoreInstanceProvider = datastoreInstanceProvider,
					designDocumentProvider = designDocumentProvider
				)
				val testAmpDAO = TestAmpDAO(
					ampDAO = ampDAO,
					couchDbDispatcher = dispatcher,
					datastoreInstanceProvider = datastoreInstanceProvider,
					designDocumentProvider = designDocumentProvider
				)
				testAmpMapping(testAmpDAO)
			} else {
				println("CouchDB credentials for drugs db are not valid")
			}
		} else {
			println("Missing properties, AMP test will be skipped")
		}
	}

	private fun StringSpec.testAmpMapping(testAmpDAO: TestAmpDAO) {
		"Can map all Amps in drugs to V1 and V2 DTOs" {
			val ampsCount =
				testAmpDAO
					.getAllAmps()
					.onEach { amp ->
						ampV1Mapper.map(amp)
						ampV2Mapper.map(amp)
					}.count()
			println("Successfully tested $ampsCount Amps")
		}
	}
}
