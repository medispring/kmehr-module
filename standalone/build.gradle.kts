import com.github.jk1.license.render.CsvReportRenderer
import com.github.jk1.license.render.ReportRenderer
import java.text.SimpleDateFormat
import java.util.*

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed (https://youtrack.jetbrains.com/issue/KTIJ-19369)
plugins {
    id("com.icure.kotlin-application-conventions")

    alias(coreLibs.plugins.springBootPlugin) apply(true)
    alias(coreLibs.plugins.mavenRepository)
    alias(coreLibs.plugins.gitVersion) apply(true)
    alias(coreLibs.plugins.helmRepository) apply(true)
    alias(coreLibs.plugins.licenceReport) apply(true)
    alias(coreLibs.plugins.springBootDependenciesManagement) apply(true)
    alias(coreLibs.plugins.kotlinAllOpen) apply(true)
    alias(coreLibs.plugins.kotlinSpring) apply(true)
    alias(coreLibs.plugins.kotlinxSerialization) apply(true)

    `maven-publish`
}

licenseReport {
    renderers = arrayOf<ReportRenderer>(CsvReportRenderer())
}

val gitVersion: String? by project

group = "org.taktik.icure"
version = gitVersion ?: "0.0.1-SNAPSHOT"

configure<com.taktik.gradle.plugins.HelmRepositoryPluginExtension> {
    this.chartNameOverride = "kmehr-module"
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
    mainClass.set("org.taktik.icure.ICureKmehrStandaloneModuleKt")
    manifest {
        attributes(mapOf(
            "Built-By" to System.getProperties()["user.name"],
            "Build-Timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(Date()),
            "Build-Revision" to gitVersion,
            "Created-By" to "Gradle ${gradle.gradleVersion}",
            "Build-Jdk" to "${System.getProperties()["java.version"]} (${System.getProperties()["java.vendor"]} ${System.getProperties()["java.vm.version"]})",
            "Build-OS" to "${System.getProperties()["os.name"]} ${System.getProperties()["os.arch"]} ${System.getProperties()["os.version"]}"
        ))
    }
}

tasks.withType<org.springframework.boot.gradle.tasks.run.BootRun> {
    if ( project.hasProperty("jvmArgs") ) {
        jvmArgs = (project.getProperties()["jvmArgs"] as String).split(Regex("\\s+"))
    }
}

configurations {
    all {
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
        exclude(group = "log4j", module = "log4j")
    }
    listOf(apiElements, runtimeElements).forEach {
        it.get().outgoing.artifacts.removeIf {
            it.buildDependencies.getDependencies(null).any { it is Jar }
        }
        it.get().outgoing.artifact(tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar>().first())
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs = (jvmArgs ?: emptyList()) + "-Dnet.bytebuddy.experimental=true"
}

tasks.withType<JavaCompile>().configureEach {
    options.isIncremental = true
}

tasks.withType<JavaCompile> {
    options.isFork = true
    options.fork("memoryMaximumSize" to "16192m")
}

dependencies {
    implementation(project(":kmehr"))
    implementation(project(":sam"))
    implementation(project(":kraken-common:core"))
    implementation(project(":kraken-common:dao"))
    implementation(project(":kraken-common:logic"))
    implementation(project(":kraken-common:mapper"))
    implementation(project(":kraken-common:dto"))
    implementation(project(":kraken-common:domain"))
    implementation(project(":kraken-common:jwt"))
    implementation(project(":kraken-common:utils"))

    implementation(coreLibs.bundles.krouchLibs)
    implementation(coreLibs.bundles.jsonWebTokenLibs)
    implementation(coreLibs.bundles.bouncyCastleLibs)
    implementation(coreLibs.bundles.kotlinxCoroutinesLibs)
    implementation(coreLibs.bundles.ktorClientLibs)
    implementation(coreLibs.bundles.jacksonLibs)

    implementation(kmehrLibs.icureKotlinSDK)

    implementation(coreLibs.springBootWebflux)
    implementation(coreLibs.springBootSecurity)

    implementation(coreLibs.springAspects)
    implementation(coreLibs.springWebsocket)

    implementation(coreLibs.ktorSerialization)

    implementation(coreLibs.asyncJacksonHttpClient)

    implementation(coreLibs.caffeine)
    implementation(coreLibs.springBootCache)

    implementation(kmehrLibs.jaxbApi)
    implementation(kmehrLibs.jaxbRuntime)

    implementation(coreLibs.taktikBoot)

    implementation(kmehrLibs.credentialsRotation)

    testImplementation(coreLibs.bundles.kotestLibs)
    testImplementation(coreLibs.springBootTest)
    testImplementation(coreLibs.jupiter)
    testImplementation(coreLibs.mockk)
    testImplementation(kmehrLibs.icureTestStack)
}
