package org.taktik.icure.asynclogic.samv2.impl

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asyncdao.samv2.*
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.asynclogic.samv2.SamV2Logic
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.db.sanitize
import org.taktik.icure.entities.samv2.*
import org.taktik.icure.entities.samv2.embed.SamText
import org.taktik.icure.utils.aggregateResults
import org.taktik.icure.utils.bufferedChunks
import org.taktik.icure.utils.distinct
import java.util.*

@Service
@Profile("sam")
class SamV2LogicImpl(
    private val ampDAO: AmpDAO,
    private val vmpDAO: VmpDAO,
    private val vmpGroupDAO: VmpGroupDAO,
    private val nmpDAO: NmpDAO,
    private val substanceDAO: SubstanceDAO,
    private val pharmaceuticalFormDAO: PharmaceuticalFormDAO,
    private val paragraphDAO: ParagraphDAO,
    private val verseDAO: VerseDAO,
    private val datastoreInstanceProvider: DatastoreInstanceProvider
) : SamV2Logic {
    private val mutex = Mutex()

    private var ampProductIds: Map<String, String>? = null
    private var nmpProductIds: Map<String, String>? = null
    private var vmpProductIds: Map<String, String>? = null

    override fun findVmpsByGroupId(vmpgId: String, paginationOffset: PaginationOffset<String>): Flow<ViewQueryResultEvent> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(vmpDAO.findVmpsByGroupId(datastore, vmpgId, paginationOffset))
    }

    override fun findAmpsByVmpGroupCode(vmpgCode: String, paginationOffset: PaginationOffset<String>): Flow<ViewQueryResultEvent> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(ampDAO.findAmpsByVmpGroupCode(datastore, vmpgCode, paginationOffset))
    }

    override fun findAmpsByVmpGroupId(vmpgId: String, paginationOffset: PaginationOffset<String>): Flow<ViewQueryResultEvent> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(ampDAO.findAmpsByVmpGroupId(datastore, vmpgId, paginationOffset))
    }

    override fun findAmpsByVmpCode(vmpCode: String, paginationOffset: PaginationOffset<String>): Flow<ViewQueryResultEvent> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(ampDAO.findAmpsByVmpCode(datastore, vmpCode, paginationOffset))
    }

    override fun findAmpsByVmpId(vmpId: String, paginationOffset: PaginationOffset<String>): Flow<ViewQueryResultEvent> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(ampDAO.findAmpsByVmpId(datastore, vmpId, paginationOffset))
    }

    override fun findAmpsByDmppCode(dmppCode: String): Flow<ViewQueryResultEvent> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(ampDAO.findAmpsByDmppCode(datastore, dmppCode))
    }

    override fun findAmpsByAmpCode(ampCode: String): Flow<ViewQueryResultEvent> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(ampDAO.findAmpsByAmpCode(datastore, ampCode))
    }

    override fun listVmpIdsByGroupCode(vmpgCode: String): Flow<String> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(vmpDAO.listVmpIdsByGroupCode(datastore, vmpgCode))
    }

    override fun listVmpIdsByGroupId(vmpgId: String): Flow<String> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(vmpDAO.listVmpIdsByGroupId(datastore, vmpgId))
    }

    override fun listAmpIdsByVmpGroupCode(vmpgCode: String): Flow<String> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(ampDAO.listAmpIdsByVmpGroupCode(datastore, vmpgCode))
    }

    override fun listAmpIdsByVmpGroupId(vmpgId: String): Flow<String> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(ampDAO.listAmpIdsByVmpGroupId(datastore, vmpgId))
    }

    override fun listAmpIdsByVmpCode(vmpCode: String): Flow<String> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(ampDAO.listAmpIdsByVmpCode(datastore, vmpCode))
    }

    override fun listAmpIdsByVmpId(vmpId: String): Flow<String> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(ampDAO.listAmpIdsByVmpId(datastore, vmpId))
    }

    override suspend fun getVersion(): SamVersion? {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        return ampDAO.getVersion(datastore)
    }

    override suspend fun listAmpProductIds(ids: Collection<String>): List<ProductId?> {
        mutex.withLock {
            val datastore = datastoreInstanceProvider.getInstanceAndGroup()
            if (this.ampProductIds == null) {
                this.ampProductIds = ampDAO.getProductIdsFromSignature(datastore, "amp")
                return ids.map { id -> this.ampProductIds?.get(id)?.let { ProductId(id = id, productId = it) } }
            }
        }
        return ids.map { id -> this.ampProductIds?.get(id)?.let { ProductId(id = id, productId = it) } }
    }

    override suspend fun listVmpgProductIds(ids: Collection<String>): List<ProductId?> {
        mutex.withLock {
            val datastore = datastoreInstanceProvider.getInstanceAndGroup()
            if (this.vmpProductIds == null) {
                this.vmpProductIds = ampDAO.getProductIdsFromSignature(datastore, "vmp")
                return ids.map { id -> this.vmpProductIds?.get(id)?.let { ProductId(id = id, productId = it) } }
            }
        }
        return ids.map { id -> this.vmpProductIds?.get(id)?.let { ProductId(id = id, productId = it) } }
    }

    override suspend fun listNmpProductIds(ids: Collection<String>): List<ProductId?> {
        mutex.withLock {
            val datastore = datastoreInstanceProvider.getInstanceAndGroup()
            if (this.nmpProductIds == null) {
                this.nmpProductIds = ampDAO.getProductIdsFromSignature(datastore, "nmp")
                return ids.map { id -> this.nmpProductIds?.get(id)?.let { ProductId(id = id, productId = it) } }
            }
        }
        return ids.map { id -> this.nmpProductIds?.get(id)?.let { ProductId(id = id, productId = it) } }
    }

    override fun findVmpsByGroupCode(vmpgCode: String, paginationOffset: PaginationOffset<String>): Flow<ViewQueryResultEvent> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(vmpDAO.findVmpsByGroupCode(datastore, vmpgCode, paginationOffset))
    }

    override fun findAmpsByLabel(language: String?, label: String, paginationOffset: PaginationOffset<List<String>>): Flow<Amp> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        val labelComponents = label.split(" ").mapNotNull { it.sanitize() }
        val ampIds = ampDAO.listAmpIdsByLabel(datastore, language, labelComponents.maxByOrNull { it.length }).toSet(TreeSet())

        emitAll(
            aggregateResults(
                ids = ampIds,
                limit = paginationOffset.limit,
                supplier = { ids -> ampDAO.getEntities(ids) },
                filter = { amp -> labelComponents.all { labelComponent ->
                    listOfNotNull(
                        amp.officialName,
                        amp.abbreviatedName?.localized(language)?.sanitize(),
                        amp.prescriptionName?.localized(language)?.sanitize(),
                        amp.name?.localized(language)?.sanitize(),
                    ).any { it.contains(other = labelComponent, ignoreCase = true) }
                } },
                startDocumentId = paginationOffset.startDocumentId,
            ).asFlow()
        )
    }

    private fun SamText.localized(language: String?) = when (language) {
        "fr" -> this.fr
        "en" -> this.en
        "de" -> this.de
        "nl" -> this.nl
        else -> null
    }

    override fun listAmpIdsByLabel(language: String?, label: String?): Flow<String> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(ampDAO.listAmpIdsByLabel(datastore, language, label))
    }

    override fun findVmpsByLabel(language: String?, label: String?, paginationOffset: PaginationOffset<List<String>>): Flow<ViewQueryResultEvent> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(vmpDAO.findVmpsByLabel(datastore, language, label, paginationOffset))
    }

    override fun listVmpIdsByLabel(language: String?, label: String?): Flow<String> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(vmpDAO.listVmpIdsByLabel(datastore, language, label))
    }

    override fun findVmpGroupsByLabel(language: String?, label: String?, paginationOffset: PaginationOffset<List<String>>): Flow<ViewQueryResultEvent> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(vmpGroupDAO.findVmpGroupsByLabel(datastore, language, label, paginationOffset))
    }

    override fun findVmpGroupsByVmpGroupCode(vmpgCode: String, paginationOffset: PaginationOffset<String>): Flow<ViewQueryResultEvent> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(vmpGroupDAO.findVmpGroupsByVmpGroupCode(datastore, vmpgCode, paginationOffset))
    }

    override fun findVmpGroups(paginationOffset: PaginationOffset<String>): Flow<ViewQueryResultEvent> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(vmpGroupDAO.findVmpGroups(datastore, paginationOffset))
    }

    override fun findVmpsByVmpCode(vmpCode: String, paginationOffset: PaginationOffset<String>): Flow<ViewQueryResultEvent> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(vmpDAO.findVmpsByVmpCode(datastore, vmpCode, paginationOffset))
    }

    override fun listVmpGroupIdsByLabel(language: String?, label: String?): Flow<String> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(vmpGroupDAO.listVmpGroupIdsByLabel(datastore, language, label))
    }

    override fun findNmpsByLabel(language: String?, label: String?, paginationOffset: PaginationOffset<List<String>>): Flow<ViewQueryResultEvent> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(nmpDAO.findNmpsByLabel(datastore, language, label, paginationOffset))
    }

    override fun listNmpIdsByLabel(language: String?, label: String?): Flow<String> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(nmpDAO.listNmpIdsByLabel(datastore, language, label))
    }

    override fun listSubstances(): Flow<Substance> {
        return substanceDAO.getEntities()
    }

    override fun listPharmaceuticalForms(): Flow<PharmaceuticalForm> {
        return pharmaceuticalFormDAO.getEntities()
    }

    override fun listVmpsByVmpCodes(vmpCodes: List<String>): Flow<Vmp> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(vmpDAO.listVmpsByVmpCodes(datastore, vmpCodes))
    }

    override fun findAmpsByAtcCode(atcCode: String, paginationOffset: PaginationOffset<String>): Flow<ViewQueryResultEvent> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(ampDAO.findAmpsByAtc(datastore, atcCode, paginationOffset))
    }

    override fun listVmpsByGroupIds(vmpgIds: List<String>): Flow<Vmp> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(vmpDAO.listVmpsByGroupIds(datastore, vmpgIds))
    }

    override fun listAmpsByGroupCodes(vmpgCodes: List<String>): Flow<Amp> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(ampDAO.listAmpsByVmpGroupCodes(datastore, vmpgCodes))
    }

    override fun listAmpsByDmppCodes(dmppCodes: List<String>): Flow<Amp> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(ampDAO.listAmpsByDmppCodes(datastore, dmppCodes))
    }

    override fun listAmpsByGroupIds(groupIds: List<String>): Flow<Amp> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(ampDAO.listAmpsByVmpGroupIds(datastore, groupIds))
    }

    override fun listAmpsByVmpCodes(vmpgCodes: List<String>): Flow<Amp> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(ampDAO.listAmpsByVmpCodes(datastore, vmpgCodes))
    }

    override fun listAmpsByVmpIds(vmpIds: List<String>): Flow<Amp> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(ampDAO.listAmpsByVmpIds(datastore, vmpIds))
    }

    override fun listVmpGroupsByVmpGroupCodes(vmpgCodes: List<String>): Flow<VmpGroup> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(vmpGroupDAO.listVmpGroupsByVmpGroupCodes(datastore, vmpgCodes))
    }

    override fun listNmpsByCnks(cnks: List<String>): Flow<Nmp> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(nmpDAO.listNmpsByCnks(datastore, cnks))
    }

    override fun findParagraphs(searchString: String, language: String): Flow<Paragraph> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(paragraphDAO.findParagraphs(datastore, searchString, language, PaginationOffset(1000))
            .filterIsInstance<ViewRowWithDoc<ComplexKey, Int, Paragraph>>()
            .map { it.doc })
    }

    override fun findParagraphsWithCnk(cnk: Long, language: String): Flow<Paragraph> = flow  {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(paragraphDAO.findParagraphsWithCnk(datastore, cnk, language))
    }

    override suspend fun getParagraphInfos(chapterName: String, paragraphName: String): Paragraph {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        return paragraphDAO.getParagraph(datastore, chapterName, paragraphName)
    }

    override suspend fun getVersesHierarchy(chapterName: String, paragraphName: String): Verse? {
        val allVerses: List<Verse> = listVerses(chapterName, paragraphName).toList()

        fun fillChildren(v: Verse): Verse = v.copy(children = allVerses.filter { it.verseSeqParent == v.verseSeq }.map { fillChildren(it) })

        return allVerses.takeIf { it.isNotEmpty() }?.let { fillChildren(it.first()) }
    }

    override fun listVerses(
        chapterName: String,
        paragraphName: String
    ) = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(verseDAO.listVerses(datastore, chapterName, paragraphName))
    }

    override fun getAmpsForParagraph(chapterName: String, paragraphName: String): Flow<Amp> = flow {
        val datastore = datastoreInstanceProvider.getInstanceAndGroup()
        emitAll(ampDAO.findAmpsByChapterParagraph(datastore, chapterName, paragraphName, PaginationOffset(1000))
            .filterIsInstance<ViewRowWithDoc<ComplexKey, Int, Amp>>()
            .map { it.doc })
    }

    @ExperimentalCoroutinesApi
    @FlowPreview
    override fun getVtmNamesForParagraph(chapterName: String, paragraphName: String, language: String): Flow<String> {
        return getAmpsForParagraph(chapterName, paragraphName).bufferedChunks(100, 200).flatMapConcat {
            vmpDAO.getEntities(it.mapNotNull { it.vmp?.id }).mapNotNull {
                it.vtm?.name?.let { t ->
                    when (language) {
                        "fr" -> t.fr
                        "en" -> t.en
                        "de" -> t.de
                        "nl" -> t.nl
                        else -> null
                    }
                }
            }
        }.distinct()
    }
}
