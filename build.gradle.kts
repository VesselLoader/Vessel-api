// ==============================================================================
// VESSEL-API — Build Gradle (Kotlin DSL)
// Version: 1.0.3
// Description: Public Developer Contracts & Universal Super-API (VUSA) for VESSEL.
//              Interconnects bidirectionally with VESSEL Sovereign Core locally
//              via Gradle Composite Build (includeBuild) and at runtime.
// ==============================================================================

plugins {
    `java-library`
    `maven-publish`
}

group = "dev.vessel"
version = "1.1.0"
description = "VESSEL Universal Super-API (VUSA) — Public Developer & Cross-Loader Symmetry Contracts"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    // ASM 9.7 — Bytecode Taxonomy & Symmetry Transmutation Contracts
    api("org.ow2.asm:asm:9.7")
    api("org.ow2.asm:asm-tree:9.7")

    // Testing Framework (JUnit 5 + AssertJ)
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.26.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all", "-Xlint:-processing", "--enable-preview"))
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).apply {
        addBooleanOption("-enable-preview", true)
        addStringOption("source", "21")
        addStringOption("Xdoclint:none", "-quiet")
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs("--enable-preview")
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

tasks.withType<Jar>().configureEach {
    from(listOf("CREDITS.md", "NOTICE")) {
        into("META-INF")
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = "vessel-api"
            version = project.version.toString()
        }
    }
}
