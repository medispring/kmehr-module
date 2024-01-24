package org.taktik.icure.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.WebFluxConfigurer

@Configuration
@EnableWebFlux
class WebConfig : WebFluxConfigurer {

    override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
        configurer.defaultCodecs().maxInMemorySize(128 * 1024 * 1024)

        configurer.defaultCodecs().jackson2JsonEncoder(
            Jackson2JsonEncoder(
                ObjectMapper().registerModule(
                    KotlinModule.Builder()
                        .configure(KotlinFeature.NullIsSameAsDefault, true)
                        .build()
                ).apply { setSerializationInclusion(JsonInclude.Include.NON_NULL) }
            )
        )
        configurer.defaultCodecs().jackson2JsonDecoder(
            Jackson2JsonDecoder(
                ObjectMapper().registerModule(
                    KotlinModule.Builder()
                        .configure(KotlinFeature.NullIsSameAsDefault, true)
                        .build()
                )
            ).apply { maxInMemorySize = 128 * 1024 * 1024 }
        )
    }

    override fun addCorsMappings(registry: CorsRegistry) {
        registry
            .addMapping("/**")
            .allowCredentials(true)
            .allowedOriginPatterns("*")
            .allowedMethods("*")
            .allowedHeaders("*")
    }

}
