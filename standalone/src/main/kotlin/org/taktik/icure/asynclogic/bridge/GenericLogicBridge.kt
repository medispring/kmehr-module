package org.taktik.icure.asynclogic.bridge

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.utils.ExternalFilterKey
import org.taktik.icure.exceptions.BridgeException

@Suppress("UNUSED_PARAMETER", "RedundantSuspendModifier")
open class GenericLogicBridge<E : Identifiable<String>> {

    fun solveConflicts(limit: Int?, ids: List<String>?): Flow<IdAndRev> {
        throw BridgeException()
    }

    fun solveConflicts(groupId: String, limit: Int?): Flow<IdAndRev> {
        throw IllegalStateException("Bridge method not implemented")
    }

    fun filter(filter: FilterChain<E>): Flow<E> {
        throw IllegalStateException("Bridge method not implemented")
    }

    open fun createEntities(entities: Collection<E>): Flow<E> {
        throw IllegalStateException("Bridge method not implemented")
    }

    fun createEntities(entities: Flow<E>): Flow<E> {
        throw IllegalStateException("Bridge method not implemented")
    }

    fun deleteEntities(identifiers: Collection<String>): Flow<DocIdentifier> {
        throw IllegalStateException("Bridge method not implemented")
    }

    fun deleteEntities(identifiers: Flow<String>): Flow<DocIdentifier> {
        throw IllegalStateException("Bridge method not implemented")
    }

    suspend fun exists(id: String): Boolean {
        throw IllegalStateException("Bridge method not implemented")
    }

    fun getEntities(): Flow<E> {
        throw IllegalStateException("Bridge method not implemented")
    }

    fun getEntities(identifiers: Collection<String>): Flow<E> {
        throw IllegalStateException("Bridge method not implemented")
    }

    fun getEntities(identifiers: Flow<String>): Flow<E> {
        throw IllegalStateException("Bridge method not implemented")
    }

    suspend fun getEntity(id: String): E? {
        throw IllegalStateException("Bridge method not implemented")
    }

    fun getEntityIds(): Flow<String> {
        throw IllegalStateException("Bridge method not implemented")
    }

    suspend fun hasEntities(): Boolean {
        throw IllegalStateException("Bridge method not implemented")
    }

    open fun modifyEntities(entities: Collection<E>): Flow<E> {
        throw IllegalStateException("Bridge method not implemented")
    }

    open fun modifyEntities(entities: Flow<E>): Flow<E> {
        throw IllegalStateException("Bridge method not implemented")
    }

    fun undeleteByIds(identifiers: Collection<String>): Flow<DocIdentifier> {
        throw IllegalStateException("Bridge method not implemented")
    }

    fun listEntityIdsInCustomView(
        viewName: String,
        partitionName: String,
        startKey: ExternalFilterKey?,
        endKey: ExternalFilterKey?
    ): Flow<String> {
        throw BridgeException()
    }
}
