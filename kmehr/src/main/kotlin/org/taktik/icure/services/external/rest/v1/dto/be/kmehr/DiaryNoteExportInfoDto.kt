/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.be.kmehr

import org.taktik.icure.services.external.rest.v1.dto.HealthcarePartyDto
import java.io.Serializable

data class DiaryNoteExportInfoDto(
    val encryptionDecryptionKeys: List<String> = emptyList(),
    val excludedIds: List<String> = emptyList(),
    val recipient: HealthcarePartyDto? = null,
    val softwareName: String? = null,
    val softwareVersion: String? = null,
    val tags: List<String> = emptyList(),
    val contexts: List<String> = emptyList(),
    val psy: Boolean? = null,
    val documentId: String? = null,
    val attachmentId: String? = null,
    val note: String? = null
) : Serializable
