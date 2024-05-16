pluginManagement {
    // Include 'plugins build' to define convention plugins.
    includeBuild("./kraken-common/build-logic")

    repositories {
        mavenLocal()
        google()
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://maven.taktik.be/content/groups/public") }
        maven { url = uri("https://plugins.gradle.org/m2/") }
        maven { url = uri("https://repo.spring.io/plugins-release") }
        maven { url = uri("https://jitpack.io") }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenLocal()
        mavenCentral()
        maven { url = uri("https://maven.taktik.be/content/groups/public") }
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://repo.spring.io/plugins-release") }
    }

    versionCatalogs {
        create("coreLibs") {
            from(files("./kraken-common/libs.versions.toml"))
        }
        create("kmehrLibs") {
            from(files("./libs.versions.toml"))
        }
    }
}

val sharedDeps = listOf(
    "utils",
    "domain",
    "dto",
    "dao",
    "logic",
    "jwt",
    "mapper",
    "core",
    "service"
)

val kmehrDeps = listOf(
    "standalone",
    "kmehr",
    "sam"
)

include(
    sharedDeps.map { ":kraken-common:$it" } + kmehrDeps.map { ":$it" }
)

rootProject.name = "kmehr-module"
