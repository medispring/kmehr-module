package org.taktik.icure.be.ehealth.logic

import org.taktik.icure.asynclogic.DocumentLogic
import org.taktik.icure.security.CryptoUtils
import org.taktik.icure.security.CryptoUtils.isValidAesKey
import org.taktik.icure.security.CryptoUtils.keyFromHexString
import org.taktik.icure.utils.toByteArray
import java.security.GeneralSecurityException
import java.security.KeyException

suspend fun DocumentLogic.getAndDecryptMainAttachment(documentId: String, encKeys: List<String>) =
    getMainAttachment(documentId)
        .toByteArray(true)
        .let { content ->
            encKeys.asSequence()
                .filter { sfk -> sfk.keyFromHexString().isValidAesKey() }
                .mapNotNull { sfk ->
                    try {
                        CryptoUtils.decryptAES(content, sfk.keyFromHexString())
                    } catch (_: GeneralSecurityException) {
                        null
                    } catch (_: KeyException) {
                        null
                    } catch (_: IllegalArgumentException) {
                        null
                    }
                }.firstOrNull() ?: content
        }

suspend fun DocumentLogic.getAndDecryptMainAttachment(documentId: String, encKey: String? = null) =
    getAndDecryptMainAttachment(documentId, encKey?.let{ listOf(it) } ?: emptyList())
