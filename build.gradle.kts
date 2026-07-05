plugins {
    kotlin("jvm") version "1.9.0"
}

group = "com.example.miruro"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    // CloudStream API (from JitPack)
    implementation("com.github.recloudstream:cloudstream3:master-SNAPSHOT")
    // HTML parser (for scraping)
    implementation("org.jsoup:jsoup:1.15.3")
}

// Configure Kotlin compilation
kotlin {
    jvmToolchain(17)
}

// Task to build the .cs3 file (CloudStream extension)
tasks.register<Jar>("make") {
    archiveBaseName.set("miruro")
    archiveExtension.set("cs3")
    from(sourceSets.main.get().output)
    from(configurations.runtimeClasspath.get().map { 
        if (it.isDirectory) it else zipTree(it) 
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Task to generate plugins.json (metadata)
tasks.register("makePluginsJson") {
    doLast {
        val json = """
            {
                "name": "Miruro",
                "description": "Watch anime from Miruro.tv",
                "language": "en",
                "plugins": [
                    {
                        "name": "Miruro",
                        "main": "com.example.miruro.MiruroProvider"
                    }
                ]
            }
        """.trimIndent()
        file("build/plugins.json").writeText(json)
    }
}

// Make 'make' and 'makePluginsJson' run together
tasks.named("make") {
    dependsOn("makePluginsJson")
}
