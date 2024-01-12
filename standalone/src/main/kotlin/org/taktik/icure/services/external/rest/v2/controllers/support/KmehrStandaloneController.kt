package org.taktik.icure.services.external.rest.v2.controllers.support

import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.taktik.icure.config.KmehrConfiguration

@RestController("kmehrStandaloneController")
@Profile("kmehr")
@RequestMapping("/rest/v2/be_kmehr")
class KmehrStandaloneController(
    private val kmehrConfiguration: KmehrConfiguration
) {
    @GetMapping("/module/v")
    fun getVersion(): String = kmehrConfiguration.kmehrVersion

    @GetMapping("/jwt/isValid")
    fun isValid(): Boolean = true
}
