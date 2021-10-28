plugins {
    application
    kotlin("jvm")                              version "1.5.31"
    kotlin("plugin.spring")                    version "1.5.31"
    id("org.springframework.boot")             version "2.5.6"
    id("io.spring.dependency-management")      version "1.0.11.RELEASE"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("io.projectreactor.kafka:reactor-kafka")

    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("io.cloudevents:cloudevents-kafka:2.2.0")

    implementation("org.webjars:bootstrap:4.5.3")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.3")

    testImplementation("org.testcontainers:kafka:1.16.2")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
}

application {
    mainClass.set("chatkafka.MainKt")
}

tasks.withType<org.springframework.boot.gradle.tasks.run.BootRun> {
    dependsOn("testClasses")
    classpath += sourceSets["test"].runtimeClasspath
}