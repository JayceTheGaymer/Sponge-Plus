pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.kikugie.dev/snapshots")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev")
        maven("https://maven.neoforged.net/releases/")
    }
}

plugins {
    id("gg.meza.stonecraft") version "1.10.+"
    id("dev.kikugie.stonecutter") version "0.9.+"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    centralScript = "build.gradle.kts"
    kotlinController = true
    shared {
        fun mc(version: String, vararg loaders: String) {
            // Each loader gets its own version directory, e.g. "1.21.1-fabric".
            for (it in loaders) version("$version-$it", version)
        }

        mc("1.21.1", "fabric", "neoforge")
        mc("26.2", "fabric", "neoforge")

        vcsVersion = "1.21.1-neoforge"
    }
    create(rootProject)
}

rootProject.name = "Sponge-Plus"
