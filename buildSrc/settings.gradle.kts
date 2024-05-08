dependencyResolutionManagement {
	versionCatalogs {
		create("coreLibs") {
			from(files("../kraken-common/libs.versions.toml"))
		}
	}
}
