package org.taktik.icure.asynclogic.bridge

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.base.Code
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.conflicts.ConflictResolutionResult
import org.taktik.icure.entities.conflicts.ConflictResolutionStrategy
import org.taktik.icure.entities.conflicts.MergeResult
import org.taktik.icure.entities.utils.ExternalFilterKey
import org.taktik.icure.exceptions.BridgeException

@Suppress("UNUSED_PARAMETER", "RedundantSuspendModifier", "unused")
open class GenericLogicBridge<E : StoredDocument> {

	open fun matchEntitiesBy(filter: AbstractFilter<*>): Flow<String> {
		throw BridgeException()
	}

	fun solveConflicts(limit: Int?, ids: List<String>?, strategy: ConflictResolutionStrategy): Flow<MergeResult> {
		throw BridgeException()
	}

	fun solveConflicts(groupId: String, limit: Int?): Flow<IdAndRev> {
		throw BridgeException()
	}

	fun filter(filter: FilterChain<E>): Flow<E> {
		throw BridgeException()
	}

	open suspend fun createEntity(entity: E): E {
		throw BridgeException()
	}

	open fun purgeEntities(identifiers: Collection<IdAndRev>): Flow<DocIdentifier> {
		throw BridgeException()
	}

	open suspend fun modifyEntity(entity: E): E {
		throw BridgeException()
	}

	open fun createEntities(entities: Collection<E>): Flow<E> {
		throw BridgeException()
	}

	fun createEntities(entities: Flow<E>): Flow<E> {
		throw BridgeException()
	}

	suspend fun exists(id: String): Boolean {
		throw BridgeException()
	}

	fun getEntities(): Flow<E> {
		throw BridgeException()
	}

	fun getEntities(identifiers: Collection<String>): Flow<E> {
		throw BridgeException()
	}

	fun getEntities(identifiers: Flow<String>): Flow<E> {
		throw BridgeException()
	}

	suspend fun getEntity(id: String): E? {
		throw BridgeException()
	}

	fun getEntityIds(): Flow<String> {
		throw BridgeException()
	}

	suspend fun hasEntities(): Boolean {
		throw BridgeException()
	}

	open fun modifyEntities(entities: Collection<E>): Flow<E> {
		throw BridgeException()
	}

	open fun modifyEntities(entities: Flow<E>): Flow<E> {
		throw BridgeException()
	}

	fun undeleteByIds(identifiers: Collection<String>): Flow<DocIdentifier> {
		throw BridgeException()
	}

	fun listEntityIdsInCustomView(
		viewName: String,
		partitionName: String,
		startKey: ExternalFilterKey?,
		endKey: ExternalFilterKey?
	): Flow<String> {
		throw BridgeException()
	}

	suspend fun deleteEntity(id: String, rev: String?): E {
		throw BridgeException()
	}

	fun deleteEntities(identifiers: Collection<IdAndRev>): Flow<E> {
		throw BridgeException()
	}

	suspend fun undeleteEntity(id: String, rev: String?): E {
		throw BridgeException()
	}

	fun undeleteEntities(identifiers: Collection<IdAndRev>): Flow<E> {
		throw BridgeException()
	}

	suspend fun purgeEntity(id: String, rev: String): DocIdentifier {
		throw BridgeException()
	}

	suspend fun getEntity(id: String, rev: String?): E? {
		throw BridgeException()
	}

	fun getConflictingEntitiesIds(): Flow<String> {
		throw BridgeException()
	}

	fun getConflictsFor(entityId: String): Flow<E> {
		throw BridgeException()
	}

	suspend fun declareConflictWinner(
		entity: E,
		conflictsToPurge: List<E>
	): ConflictResolutionResult<E> {
		throw BridgeException()
	}

	suspend fun getBypassingCache(
		id: String,
		rev: String
	): E? {
		throw BridgeException()
	}
}
