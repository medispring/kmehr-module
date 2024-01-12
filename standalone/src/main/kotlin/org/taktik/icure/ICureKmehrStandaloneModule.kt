package org.taktik.icure

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.scheduling.annotation.EnableScheduling
import org.taktik.icure.asyncdao.InternalDAO
import org.taktik.icure.properties.CouchDbProperties
import javax.xml.bind.JAXBContext

@SpringBootApplication(
    scanBasePackages = [
        "org.springframework.boot.autoconfigure.aop",
        "org.springframework.boot.autoconfigure.context",
        "org.springframework.boot.autoconfigure.validation",
        "org.springframework.boot.autoconfigure.websocket",
        "org.taktik.icure.config",
        "org.taktik.icure.asynclogic",
        "org.taktik.icure.asyncdao",
        "org.taktik.icure.security",
        "org.taktik.icure.be.ehealth.logic",
        "org.taktik.icure.be.format.logic",
        "org.taktik.icure.db",
        "org.taktik.icure.errors",
        "org.taktik.icure.properties",
        "org.taktik.icure.services.external.http",
        "org.taktik.icure.services.external.rest",
        "org.taktik.icure.services.external.rest.v1.controllers",
        "org.taktik.icure.services.external.rest.v1.controllers.support",
        "org.taktik.icure.services.external.rest.v1.mapper",
        "org.taktik.icure.services.external.rest.v2.mapper"
    ]
)
@PropertySource("classpath:kmehr-standalone.properties")
@EnableScheduling
open class ICureEhrStandaloneModule {
    private val log = LoggerFactory.getLogger(this.javaClass)

    @Bean
    @Profile("kmehr")
    open fun performKmehrStartupTasks() = ApplicationRunner {
        log.info("Standalone kmehr module is starting")
        log.info("Standalone kmehr module is started")
    }

    @Bean
    @Profile("sam")
    open fun performStartupTasks(
        couchDbProperties: CouchDbProperties,
        internalDaos: List<InternalDAO<*>>
    ) = ApplicationRunner {
        log.info("Standalone sam module is starting")
        runBlocking {
            couchDbProperties.knownServerUrls().forEach { url ->
                internalDaos.forEach {
                    try {
                        it.forceInitStandardDesignDocument(true)
                    } catch (ex: Exception) {
                        log.error("Design doc initialization failed during startup for DAO : ${it::class.java.simpleName} on server $url", ex)
                    }
                }
            }
        }
        log.info("Standalone sam module is started")
    }

//    @Component
//    @Profile("cmd")
//    class Commander(val applicationContext: ConfigurableApplicationContext) : CommandLineRunner {
//        private val log = LoggerFactory.getLogger(this.javaClass)
//        @ExperimentalCoroutinesApi
//        override fun run(vararg args: String) {
//            if (args.firstOrNull() != "cmd") {
//                throw IllegalStateException("first argument should be profile cmd")
//            }
//            val tailArgs = args.drop(1)
//            log.info("icure commander started. Executing ${tailArgs.firstOrNull()}")
//
//            when (tailArgs.firstOrNull()) {
//                "samv2" -> Samv2v5Import().parse(tailArgs.drop(1))
//                "samv2check" -> Samv2v5Validate().parse(tailArgs.drop(1))
//                "inami" -> InamiImport().parse(tailArgs.drop(1))
//            }
//            applicationContext.close()
//
//            runBlocking {
//                //Give time for the application to close
//                delay(30000)
//                exitProcess(0)
//            }
//        }
//    }

}

fun main(args: Array<String>) {
    System.setProperty(JAXBContext.JAXB_CONTEXT_FACTORY, "com.sun.xml.bind.v2.ContextFactory")
    SpringApplicationBuilder(ICureEhrStandaloneModule::class.java)
        .web(WebApplicationType.REACTIVE)
        .run(*args)
}
