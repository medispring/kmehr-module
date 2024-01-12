/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.net.URI

@Component
@Profile("sam")
@ConfigurationProperties("icure.couchdb")
open class SAMCouchDbProperties(
	override var url: String = "http://127.0.0.1:5984",
	override var altUrls: String = "",
	override var username: String? = null,
	override var password: String? = null
) : CouchDbProperties {
	override fun knownServerUrls() = if (altUrls.isBlank()) listOf(url) else altUrls.split(";").let { if (it.contains(url)) it else listOf(url) + it }
	override fun knownServerUris() = knownServerUrls().map { URI(it) }
	override fun preferredServerUrlForNewlyRegisteredDatabase() = knownServerUrls().last()
}
