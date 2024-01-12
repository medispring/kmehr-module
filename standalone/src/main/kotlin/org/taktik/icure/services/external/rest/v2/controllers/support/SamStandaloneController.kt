package org.taktik.icure.services.external.rest.v2.controllers.support

import kotlinx.coroutines.reactor.mono
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.taktik.icure.config.KmehrConfiguration
import org.taktik.icure.security.credentialsrotation.CouchDbCredentialsManager
import org.taktik.icure.security.credentialsrotation.RotationNotification
import org.taktik.icure.security.credentialsrotation.RotationResponse
import reactor.core.publisher.Mono

@RestController("samStandaloneController")
@Profile("sam")
@RequestMapping("/rest/v2/be_samv2")
class SamStandaloneController(
    private val kmehrConfiguration: KmehrConfiguration,
    private val couchDbCredentialsManager: CouchDbCredentialsManager
) {
    @GetMapping("/module/v")
    fun getVersion(): String = kmehrConfiguration.kmehrVersion

    @GetMapping("/jwt/isValid")
    fun isValid(): Boolean = true

    @PutMapping("/couchdb/notifyrotation")
    fun couchDbCredentialsUpdate(
        @RequestBody(required = true) notification: RotationNotification
    ): Mono<RotationResponse> = mono {
        couchDbCredentialsManager.notifyNewCredentials(notification)
    }
}
