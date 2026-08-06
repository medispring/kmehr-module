package org.taktik.icure.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.icure.asyncjacksonhttpclient.net.web.WebClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.id.IDGenerator
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.samv2.AmpDAO
import org.taktik.icure.asyncdao.samv2.NmpDAO
import org.taktik.icure.asyncdao.samv2.ParagraphDAO
import org.taktik.icure.asyncdao.samv2.PharmaceuticalFormDAO
import org.taktik.icure.asyncdao.samv2.ProductIdDAO
import org.taktik.icure.asyncdao.samv2.SubstanceDAO
import org.taktik.icure.asyncdao.samv2.VerseDAO
import org.taktik.icure.asyncdao.samv2.VmpDAO
import org.taktik.icure.asyncdao.samv2.VmpGroupDAO
import org.taktik.icure.asyncdao.samv2.impl.AmpDAOImpl
import org.taktik.icure.asyncdao.samv2.impl.NmpDAOImpl
import org.taktik.icure.asyncdao.samv2.impl.ParagraphDAOImpl
import org.taktik.icure.asyncdao.samv2.impl.PharmaceuticalFormDAOImpl
import org.taktik.icure.asyncdao.samv2.impl.ProductIdDAOImpl
import org.taktik.icure.asyncdao.samv2.impl.SamUpdateDAOImpl
import org.taktik.icure.asyncdao.samv2.impl.SubstanceDAOImpl
import org.taktik.icure.asyncdao.samv2.impl.VerseDAOImpl
import org.taktik.icure.asyncdao.samv2.impl.VmpDAOImpl
import org.taktik.icure.asyncdao.samv2.impl.VmpGroupDAOImpl
import org.taktik.icure.asynclogic.samv2.UpdatesBridge
import org.taktik.icure.asynclogic.samv2.impl.SamV2Updater
import org.taktik.icure.dao.CouchDbDispatcherProvider
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.properties.SAMCouchDbProperties
import org.taktik.icure.security.CouchDbCredentialsProvider

@OptIn(ExperimentalCoroutinesApi::class)
@Configuration
@Profile("sam")
open class SamDaoConfig {
	@Bean
	open fun drugCouchDbDispatcher(
		httpClient: WebClient,
		objectMapper: ObjectMapper,
		credentialsProvider: CouchDbCredentialsProvider,
		samCouchDbProperties: SAMCouchDbProperties,
		couchDbDispatcherProvider: CouchDbDispatcherProvider,
	) = couchDbDispatcherProvider.getDispatcher(
		httpClient,
		objectMapper,
		"icure",
		"drugs${samCouchDbProperties.suffix}",
		credentialsProvider,
		3,
	)

	// Only instantiate if there is a next version suffix
	@ConditionalOnProperty(prefix = "icure.couchdb.sam", name = ["nextVersionSuffix"], matchIfMissing = false)
	@Bean
	open fun drugNextVersionCouchDbDispatcher(
		httpClient: WebClient,
		objectMapper: ObjectMapper,
		credentialsProvider: CouchDbCredentialsProvider,
		samCouchDbProperties: SAMCouchDbProperties,
		couchDbDispatcherProvider: CouchDbDispatcherProvider,
	) = couchDbDispatcherProvider.getDispatcher(
		httpClient,
		objectMapper,
		"icure",
		"drugs${samCouchDbProperties.nextVersionSuffix}",
		credentialsProvider,
		3,
	)

	@Bean
	open fun chapIVCouchDbDispatcher(
		httpClient: WebClient,
		objectMapper: ObjectMapper,
		credentialsProvider: CouchDbCredentialsProvider,
		samCouchDbProperties: SAMCouchDbProperties,
		couchDbDispatcherProvider: CouchDbDispatcherProvider,
	) = couchDbDispatcherProvider.getDispatcher(
		httpClient,
		objectMapper,
		"icure",
		"chapiv${samCouchDbProperties.suffix}",
		credentialsProvider,
		3,
	)

	// Only instantiate if there is a next version suffix
	@ConditionalOnProperty(prefix = "icure.couchdb.sam", name = ["nextVersionSuffix"], matchIfMissing = false)
	@Bean
	open fun chapIVNextVersionCouchDbDispatcher(
		httpClient: WebClient,
		objectMapper: ObjectMapper,
		credentialsProvider: CouchDbCredentialsProvider,
		samCouchDbProperties: SAMCouchDbProperties,
		couchDbDispatcherProvider: CouchDbDispatcherProvider,
	) = couchDbDispatcherProvider.getDispatcher(
		httpClient,
		objectMapper,
		"icure",
		"chapiv${samCouchDbProperties.nextVersionSuffix}",
		credentialsProvider,
		3,
	)

	@Bean
	open fun ampDao(
		@Qualifier("drugCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
		datastoreInstanceProvider: DatastoreInstanceProvider,
		designDocumentProvider: DesignDocumentProvider,
	): AmpDAO =
		AmpDAOImpl(
			couchDbDispatcher = couchDbDispatcher,
			datastoreInstanceProvider = datastoreInstanceProvider,
			designDocumentProvider = designDocumentProvider,
		)

	@Bean
	open fun nmpDao(
		@Qualifier("drugCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
		datastoreInstanceProvider: DatastoreInstanceProvider,
		designDocumentProvider: DesignDocumentProvider,
	): NmpDAO =
		NmpDAOImpl(
			couchDbDispatcher = couchDbDispatcher,
			datastoreInstanceProvider = datastoreInstanceProvider,
			designDocumentProvider = designDocumentProvider,
		)

	@Bean
	open fun vmpDao(
		@Qualifier("drugCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
		datastoreInstanceProvider: DatastoreInstanceProvider,
		designDocumentProvider: DesignDocumentProvider,
	): VmpDAO =
		VmpDAOImpl(
			couchDbDispatcher = couchDbDispatcher,
			datastoreInstanceProvider = datastoreInstanceProvider,
			designDocumentProvider = designDocumentProvider,
		)

	@Bean
	open fun vmpGroupDao(
		@Qualifier("drugCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
		datastoreInstanceProvider: DatastoreInstanceProvider,
		designDocumentProvider: DesignDocumentProvider,
	): VmpGroupDAO =
		VmpGroupDAOImpl(
			couchDbDispatcher = couchDbDispatcher,
			datastoreInstanceProvider = datastoreInstanceProvider,
			designDocumentProvider = designDocumentProvider,
		)

	@Bean
	open fun verseDao(
		@Qualifier("chapIVCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
		datastoreInstanceProvider: DatastoreInstanceProvider,
		designDocumentProvider: DesignDocumentProvider,
	): VerseDAO =
		VerseDAOImpl(
			couchDbDispatcher = couchDbDispatcher,
			datastoreInstanceProvider = datastoreInstanceProvider,
			designDocumentProvider = designDocumentProvider,
		)

	@Bean
	open fun paragraphDao(
		@Qualifier("chapIVCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
		datastoreInstanceProvider: DatastoreInstanceProvider,
		designDocumentProvider: DesignDocumentProvider,
		ampDao: AmpDAO,
	): ParagraphDAO =
		ParagraphDAOImpl(
			couchDbDispatcher = couchDbDispatcher,
			datastoreInstanceProvider = datastoreInstanceProvider,
			designDocumentProvider = designDocumentProvider,
			ampDAO = ampDao,
		)

	@Bean
	open fun pharmaceuticalFormDao(
		@Qualifier("drugCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
		datastoreInstanceProvider: DatastoreInstanceProvider,
		designDocumentProvider: DesignDocumentProvider,
	): PharmaceuticalFormDAO =
		PharmaceuticalFormDAOImpl(
			couchDbDispatcher = couchDbDispatcher,
			datastoreInstanceProvider = datastoreInstanceProvider,
			designDocumentProvider = designDocumentProvider,
		)

	@Bean
	open fun substanceDao(
		@Qualifier("drugCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
		datastoreInstanceProvider: DatastoreInstanceProvider,
		designDocumentProvider: DesignDocumentProvider,
	): SubstanceDAO =
		SubstanceDAOImpl(
			couchDbDispatcher = couchDbDispatcher,
			datastoreInstanceProvider = datastoreInstanceProvider,
			designDocumentProvider = designDocumentProvider,
		)

	@Bean
	open fun productIdDao(
		@Qualifier("drugCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
		idGenerator: IDGenerator,
		datastoreInstanceProvider: DatastoreInstanceProvider,
		designDocumentProvider: DesignDocumentProvider,
	): ProductIdDAO =
		ProductIdDAOImpl(
			couchDbDispatcher = couchDbDispatcher,
			datastoreInstanceProvider = datastoreInstanceProvider,
			designDocumentProvider = designDocumentProvider,
		)

	@Bean
	@ConditionalOnProperty(name = ["icure.sam.updaterUrl"], matchIfMissing = false)
	open fun samV2Updater(
		@Qualifier("drugCouchDbDispatcher") drugsCouchDbDispatcher: CouchDbDispatcher,
		@Qualifier("drugNextVersionCouchDbDispatcher") drugNextVersionCouchDbDispatcher: CouchDbDispatcher?,
		@Qualifier("chapIVCouchDbDispatcher") chapIVCouchDbDispatcher: CouchDbDispatcher,
		@Qualifier("chapIVNextVersionCouchDbDispatcher") chapIVNextVersionCouchDbDispatcher: CouchDbDispatcher?,
		ampDAO: AmpDAO,
		vmpDAO: VmpDAO,
		vmpGroupDAO: VmpGroupDAO,
		nmpDAO: NmpDAO,
		paragraphDAO: ParagraphDAO,
		pharmaceuticalFormDAO: PharmaceuticalFormDAO,
		substanceDAO: SubstanceDAO,
		verseDAO: VerseDAO,
		updatesBridge: UpdatesBridge,
		datastoreInstanceProvider: DatastoreInstanceProvider,
		designDocumentProvider: DesignDocumentProvider,
		samCouchDbProperties: SAMCouchDbProperties,
	) = drugNextVersionCouchDbDispatcher?.let { cdd ->
		checkNotNull(chapIVNextVersionCouchDbDispatcher) { "chapIVNextVersionCouchDbDispatcher should not be null" }
		val ampDAO =
			AmpDAOImpl(
				couchDbDispatcher = cdd,
				datastoreInstanceProvider = datastoreInstanceProvider,
				designDocumentProvider = designDocumentProvider,
			)
		runBlocking {
			SamV2Updater(
				cdd,
				ampDAO.apply { forceInitStandardDesignDocument(true) },
				VmpDAOImpl(
					couchDbDispatcher = cdd,
					datastoreInstanceProvider = datastoreInstanceProvider,
					designDocumentProvider = designDocumentProvider,
				).apply { forceInitStandardDesignDocument(true) },
				VmpGroupDAOImpl(
					couchDbDispatcher = cdd,
					datastoreInstanceProvider = datastoreInstanceProvider,
					designDocumentProvider = designDocumentProvider,
				).apply { forceInitStandardDesignDocument(true) },
				NmpDAOImpl(
					couchDbDispatcher = cdd,
					datastoreInstanceProvider = datastoreInstanceProvider,
					designDocumentProvider = designDocumentProvider,
				).apply { forceInitStandardDesignDocument(true) },
				ParagraphDAOImpl(
					couchDbDispatcher = chapIVNextVersionCouchDbDispatcher,
					datastoreInstanceProvider = datastoreInstanceProvider,
					designDocumentProvider = designDocumentProvider,
					ampDAO = ampDAO,
				).apply { forceInitStandardDesignDocument(true) },
				PharmaceuticalFormDAOImpl(
					couchDbDispatcher = cdd,
					datastoreInstanceProvider = datastoreInstanceProvider,
					designDocumentProvider = designDocumentProvider
				).apply {
					forceInitStandardDesignDocument(true)
				},
				SubstanceDAOImpl(
					couchDbDispatcher = cdd,
					datastoreInstanceProvider = datastoreInstanceProvider,
					designDocumentProvider = designDocumentProvider,
				).apply { forceInitStandardDesignDocument(true) },
				VerseDAOImpl(
					couchDbDispatcher = chapIVNextVersionCouchDbDispatcher,
					datastoreInstanceProvider = datastoreInstanceProvider,
					designDocumentProvider = designDocumentProvider,
				).apply { forceInitStandardDesignDocument(true) },
				SamUpdateDAOImpl(
					couchDbDispatcher = cdd,
					datastoreInstanceProvider = datastoreInstanceProvider,
					designDocumentProvider = designDocumentProvider,
				).apply { forceInitStandardDesignDocument(true) },
				updatesBridge,
				datastoreInstanceProvider,
			)
		}
	} ?: runBlocking {
		SamV2Updater(
			drugsCouchDbDispatcher,
			ampDAO,
			vmpDAO,
			vmpGroupDAO,
			nmpDAO,
			paragraphDAO,
			pharmaceuticalFormDAO,
			substanceDAO,
			verseDAO,
			SamUpdateDAOImpl(
				couchDbDispatcher = drugsCouchDbDispatcher,
				datastoreInstanceProvider = datastoreInstanceProvider,
				designDocumentProvider = designDocumentProvider
			).apply {
				forceInitStandardDesignDocument(true)
			},
			updatesBridge,
			datastoreInstanceProvider,
		)
	}
}
