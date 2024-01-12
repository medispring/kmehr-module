package org.taktik.icure.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
open class BridgeConfig {

    @Value("\${icure.backend.url}")
    var iCureUrl: String = "http://127.0.0.1:16043"

    @Value("\${icure.bridge.kmehrLogin}")
    var kmehrUsername: String = ""

    @Value("\${icure.bridge.kmehrPwd}")
    var kmehrPwd: String = ""
}
