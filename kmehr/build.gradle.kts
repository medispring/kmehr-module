plugins {
    id("com.icure.kotlin-library-conventions")

    alias(coreLibs.plugins.ksp)
    alias(coreLibs.plugins.mavenRepository)
    alias(coreLibs.plugins.gitVersion) apply(true)

    `maven-publish`
}

val gitVersion: String? by project

group = "org.taktik.icure"
version = gitVersion ?: "0.0.1-SNAPSHOT"

dependencies {
    ksp(group = "io.icure", name = "kmap", version = coreLibs.versions.kmap.orNull)

    implementation(project(":kraken-common:core"))
    implementation(project(":kraken-common:domain"))
    implementation(project(":kraken-common:dto"))
    implementation(project(":kraken-common:logic"))
    implementation(project(":kraken-common:mapper"))
    implementation(project(":kraken-common:utils"))

    implementation(files("../libs/ehvalidator-service-core-2.1.1.jar"))

    implementation(coreLibs.bundles.krouchLibs)
    implementation(coreLibs.springdocKotlin)
    implementation(coreLibs.springdocWebflux)
    implementation(coreLibs.kotlinxCoroutinesCore)
    implementation(coreLibs.kotlinxCoroutinesReactive)
    implementation(coreLibs.kotlinxCoroutinesReactor)

    implementation(coreLibs.springBootWebflux)
    implementation(coreLibs.springBootSecurity)

    implementation(kmehrLibs.commonsCodec)

    implementation(kmehrLibs.jaxbApi)
    implementation(kmehrLibs.jaxbRuntime)

    implementation(kmehrLibs.kotlinSerialization)
    implementation(kmehrLibs.kotlinSerializationJvm)
    implementation(kmehrLibs.kotlinSerializationCore)
    implementation(kmehrLibs.kotlinSerializationCoreJvm)
    implementation(kmehrLibs.kotlinSerializationBom)
    implementation(kmehrLibs.kotlinSerializationProtobuf)

    implementation(coreLibs.commonsIO)

    implementation(kmehrLibs.mustacheJava)

    implementation(coreLibs.kotlinxCollectionsImmutableJvm)

    implementation(coreLibs.taktikCommons)

    implementation(coreLibs.guava)

    implementation(coreLibs.kmapKsp)

    implementation(coreLibs.websocketCommons)

    testImplementation(coreLibs.bundles.kotestLibs)
    testImplementation(coreLibs.jupiter)
    testImplementation(coreLibs.mockk)
    testImplementation(coreLibs.kotlinxCoroutinesCore)
}

configurations.testRuntimeClasspath {
    resolutionStrategy {
        force("org.bouncycastle:bcprov-jdk18on:1.81.1")
        force("net.bytebuddy:byte-buddy-agent:1.14.6")
    }
}
