plugins {
    id("java")
}

group = "com.example"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.recloudstream:cloudstream3:master-SNAPSHOT")
    implementation("org.jsoup:jsoup:1.15.3")
}

tasks.register<Jar>("make") {
    archiveExtension.set("cs3")
    from(sourceSets.main.get().output)
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "com.example.miruro.MiruroProvider"
    }
}

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
