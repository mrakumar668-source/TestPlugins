plugins {
    id("com.github.recloudstream.gradle") version "1.0.0"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/recloudstream/gradle")
    }
}

cloudstream {
    language = "en"
    description = "Watch anime from Miruro.tv"
    authors = listOf("YourName")
    repo = "https://raw.githubusercontent.com/YourUsername/YourRepo/main/repo.json"
}
