plugins {
    kotlin("jvm") version "1.2.0"
    application
}

repositories {
    maven {
        setUrl("https://repo.gradle.org/gradle/repo")
    }
    jcenter()
}

dependencies {
    compile(kotlin("stdlib"))
    compile("org.gradle:gradle-tooling-api:4.4.1")
    compile("jline:jline:2.14.2")
    compile("commons-io:commons-io:2.5")
    runtime("org.slf4j:slf4j-simple:1.7.10")
}

application {
    mainClassName = "org.gradle.launcher.MainKt"
}
