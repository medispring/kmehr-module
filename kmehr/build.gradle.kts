@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed (https://youtrack.jetbrains.com/issue/KTIJ-19369)
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

// publishing {
//     publications {
//         create<MavenPublication>("kmehr") {
//             from(components["java"])
//         }
//     }
// }
//
// tasks.withType<PublishToMavenLocal>().configureEach {
//     val predicate = provider {
//         publication == publishing.publications["kmehr"]
//     }
//     onlyIf("publishing to maven local") {
//         predicate.get()
//     }
// }

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

    implementation(coreLibs.commonsIO)

    implementation(kmehrLibs.mustacheJava)

    implementation(coreLibs.kotlinxCollectionsImmutableJvm)

    implementation(coreLibs.taktikCommons)

    implementation(coreLibs.guava)

    implementation(coreLibs.mapstruct)
    implementation(coreLibs.kmapKsp)

}
