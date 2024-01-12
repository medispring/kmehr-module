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
//         create<MavenPublication>("sam") {
//             from(components["java"])
//         }
//     }
// }
//
// tasks.withType<PublishToMavenLocal>().configureEach {
//     val predicate = provider {
//         publication == publishing.publications["sam"]
//     }
//     onlyIf("publishing to maven local") {
//         predicate.get()
//     }
// }

dependencies {

    implementation(project(":kraken-common:core"))
    implementation(project(":kraken-common:dao"))
    implementation(project(":kraken-common:domain"))
    implementation(project(":kraken-common:logic"))
    implementation(project(":kraken-common:mapper"))
    implementation(project(":kraken-common:dto"))
    implementation(project(":kraken-common:utils"))

    implementation(coreLibs.bundles.swaggerLibs)
    implementation(coreLibs.bundles.krouchLibs)
    implementation(coreLibs.bundles.kotlinxCoroutinesLibs)

    implementation(coreLibs.kotlinxCollectionsImmutableJvm)

    implementation(coreLibs.springBootWebflux)
    implementation(coreLibs.springBootSecurity)

    implementation(coreLibs.mapstruct)
    implementation(coreLibs.kmapKsp)

    implementation(coreLibs.javaxServlet)

    implementation(kmehrLibs.ajaltClikt)

    ksp(group = "io.icure", name = "kmap", version = coreLibs.versions.kmap.orNull)
}
