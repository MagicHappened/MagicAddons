plugins {
    kotlin("jvm") version "2.4.10"
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
    id("maven-publish")
}

// the jar says which Minecraft it is for, and a CI build says which commit it came from:
// magicaddons-1.1.0-beta.9d66111+26.1.2.jar. A hash of nothing but digits would not be a valid
// semver prerelease, so those are prefixed
val buildId: String? = (findProperty("build_id") as String?)
    ?.takeIf { it.isNotBlank() }
    ?.let { if (it.all(Char::isDigit)) "g$it" else it }

version = buildString {
    append(project.property("mod_version") as String)
    buildId?.let { append(".$it") }
    append("+${project.property("minecraft_version")}")
}
group = project.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

val targetJavaVersion = 25


java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    withSourcesJar()
}

// only the version the source tree is currently shaped for can actually be run, so the other one
// generates no IDE run configuration and its run tasks do nothing: one game starts, not two
val activeVersion: Boolean = stonecutter.current.isActive

loom {
    runConfigs.all {
        isIdeConfigGenerated = activeVersion
    }

    accessWidenerPath.set(rootProject.file("src/main/resources/magicaddons.accesswidener"))
    mods {
        register("magicaddons") {
            sourceSet(sourceSets.main.get())
        }
    }
}

fabricApi {
    configureDataGeneration {
        client = true
    }
}

repositories {
    // Add repositories to retrieve artifacts from in here.
    // You should only use this when depending on other mods because
    // Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
    // See https://docs.gradle.org/current/userguide/declaring_repositories.html
    // for more information about repositories.
    mavenCentral()
    maven { url = uri("https://api.modrinth.com/maven") }
    maven { url = uri("https://maven.terraformersmc.com/releases/") }
    maven { url = uri("https://maven.teamresourceful.com/repository/maven-public/") }
    maven { url = uri("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")}
}



dependencies {

    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    implementation("net.fabricmc:fabric-language-kotlin:${project.property("kotlin_loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")

    implementation("com.github.tommyettinger:blazingchain:${project.property("blazing_chain_version")}")
    include("com.github.tommyettinger:blazingchain:${project.property("blazing_chain_version")}")
    
    api("com.terraformersmc:modmenu:${project.property("modmenu_version")}")

    include("tech.thatgravyboat:skyblock-api:${project.property("skyblock_api_version")}") {
        capabilities { requireCapability("tech.thatgravyboat:${project.property("skyblock_api_capability")}") }
    }

    api("tech.thatgravyboat:skyblock-api:${project.property("skyblock_api_version")}") {
        capabilities { requireCapability("tech.thatgravyboat:${project.property("skyblock_api_capability")}") }
    }
}


tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", project.property("minecraft_version"))
    inputs.property("loader_version", project.property("loader_version"))
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_version" to project.property("minecraft_version")!!,
            "loader_version" to project.property("loader_version")!!,
            "kotlin_loader_version" to project.property("kotlin_loader_version")!!
        )
    }
}


tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}

tasks.named<JavaExec>("runClient") {

    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
        vendor.set(JvmVendorSpec.JETBRAINS)
    })

    jvmArgs(
        "-Ddevauth.enabled=true",
        "-Ddevauth.account=main",
        "-XX:+AllowEnhancedClassRedefinition"
    )

    doFirst {
        val mixinJar = configurations.runtimeClasspath
            .get()
            .resolvedConfiguration
            .resolvedArtifacts
            .firstOrNull { it.moduleVersion.id.name.contains("sponge-mixin") }
            ?.file

        if (mixinJar != null) {
            jvmArgs("-javaagent:${mixinJar.absolutePath}")
        }
    }
}

// configure the maven publication
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.property("archives_base_name") as String
            from(components["java"])
        }
    }

    // See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
    repositories {
        // Add repositories to publish to here.
        // Notice: This block does NOT have the same function as the block in the top level.
        // The repositories here will be used for publishing your artifact, not for
        // retrieving dependencies.
    }
}
kotlin {
    jvmToolchain(targetJavaVersion)
}

if (!activeVersion) {
    tasks.matching { it.name.startsWith("run") }.configureEach {
        enabled = false
    }
}
