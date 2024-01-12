/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.be.kmehr

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class SoftwareMedicalFileExportDto(
    var secretForeignKeys: List<String>,
    var comment: String? = null,
    var exportAsPMF: Boolean,
    var softwareName: String? = null,
    var softwareVersion: String? = null,
) : Serializable
