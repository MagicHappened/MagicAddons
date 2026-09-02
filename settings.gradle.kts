pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
        maven("https://maven.kikugie.dev/releases") {
            name = "KikuGie Releases"
        }
        gradlePluginPortal()
    }
    plugins {
        kotlin("jvm") version "2.4.10"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.kikugie.stonecutter") version "0.9.8"
}

// one source tree, built once per Minecraft version. The tree is stored in the shape 26.1.2 wants,
// which is what vcsVersion says: everything 26.2 needs sits behind a comment in git and is
// uncommented on the way into that version's build
stonecutter {
    create(rootProject) {
        versions("26.1.2", "26.2")
        vcsVersion = "26.1.2"
    }
}
