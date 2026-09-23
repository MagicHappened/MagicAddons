plugins {
    java
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
    archivesName.set("${project.property("archives_base_name")}-extension")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
}

dependencies {
    compileOnly("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    compileOnly("org.slf4j:slf4j-api:2.0.16")
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("loader_version", project.property("loader_version"))
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "loader_version" to project.property("loader_version")!!
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.jar {
    from(rootProject.file("LICENSE")) {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}
