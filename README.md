# Vessel Universal Super-API (VUSA)

Official public API and cross-ecosystem bytecode transmutation contracts for the **VESSEL Sovereign Standalone Server Microkernel**.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.vesselloader/vessel-api.svg)](https://central.sonatype.com/artifact/io.github.vesselloader/vessel-api)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

---

## Overview

The **VESSEL Universal Super-API (VUSA)** provides unified interfaces, cross-loader events, zero-allocation storage transfers, and binary matrix contracts (`.vstmb`) bridging Fabric, Quilt, Forge, NeoForge, Bukkit, Paper, Folia, Velocity, and BungeeCord ecosystems.

## Installation

### Gradle (Kotlin DSL)

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    compileOnly("io.github.vesselloader:vessel-api:1.0.0")
}
```

### Gradle (Groovy DSL)

```groovy
repositories {
    mavenCentral()
}

dependencies {
    compileOnly 'io.github.vesselloader:vessel-api:1.0.0'
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.vesselloader</groupId>
    <artifactId>vessel-api</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
```

---

## License & Attribution

- Licensed under the Apache License, Version 2.0. See [NOTICE](NOTICE) and [CREDITS.md](CREDITS.md) for complete upstream credits.
