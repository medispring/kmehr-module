package org.taktik.icure.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.*
import java.util.jar.JarFile
import java.util.jar.Manifest

@Configuration
open class KmehrConfiguration {

    @Value("\${icure.version}")
    private var icureVersion = ""

    private val manifestVersion = getManifest()?.mainAttributes?.getValue("Build-revision")?.trim { it <= ' ' }

    val kmehrVersion: String
        get() = manifestVersion ?: icureVersion

    private fun getJarPath(): String? =
        try {
            URI(
                KmehrConfiguration::class.java.protectionDomain.codeSource.location.path.replace("jar\\!/.+".toRegex(), "jar")
            ).path
            .takeIf { it != null && it.lowercase(Locale.getDefault()).endsWith(".jar") }

        } catch (ignored: URISyntaxException) { null }

    private fun getManifest(): Manifest? =
        getJarPath()?.let{ jarPath ->
            try {
                JarFile(jarPath).use {
                    it.manifest
                }
            } catch (ignored: IOException) {
                null
            }
        }

}
