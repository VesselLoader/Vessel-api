# Vessel Universal Super-API (VUSA)

Official public API and cross-ecosystem bytecode transmutation contracts for the **VESSEL Sovereign Standalone Server Microkernel**.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.vesselloader/vessel-api.svg)](https://central.sonatype.com/artifact/io.github.vesselloader/vessel-api)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

---

## Overview

The **VESSEL Universal Super-API (VUSA)** provides unified interfaces, cross-loader events, zero-allocation storage transfers, and binary matrix contracts (`.vstmb`) bridging Fabric, Quilt, Forge, NeoForge, Bukkit, Paper, Folia, Velocity, and BungeeCord ecosystems.

- **Expressive like Forge:** Static-declaration deferred registers (`VesselDeferredRegister`) and fluent content builders without lifecycle order headaches.
- **Lightweight like Fabric:** Zero runtime reflection overhead on event invocation, modular and minimal memory footprint.
- **Full IDE Auto-Completion (IntelliSense):** Start by typing `VesselAPI.` in any IDE to discover all registration, building, networking, and event capabilities.

---

## Installation

### Gradle (Kotlin DSL)

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    compileOnly("io.github.vesselloader:vessel-api:1.1.0")
}
```

### Gradle (Groovy DSL)

```groovy
repositories {
    mavenCentral()
}

dependencies {
    compileOnly 'io.github.vesselloader:vessel-api:1.1.0'
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.vesselloader</groupId>
    <artifactId>vessel-api</artifactId>
    <version>1.1.0</version>
    <scope>provided</scope>
</dependency>
```

---

## Quickstart Guide

### 1. Declaring Blocks & Items (Static & Lazy)

```java
import dev.vessel.api.VesselAPI;
import dev.vessel.api.block.VesselBlock;
import dev.vessel.api.item.VesselItem;
import dev.vessel.api.registry.VesselDeferredRegister;
import dev.vessel.api.registry.VesselRegistryKey;
import dev.vessel.api.registry.VesselRegistrySupplier;

public final class MyModContent {
    public static final String MOD_ID = "mymod";

    public static final VesselDeferredRegister<VesselBlock> BLOCKS =
            VesselAPI.createDeferredRegister(VesselRegistryKey.BLOCK, MOD_ID);

    public static final VesselDeferredRegister<VesselItem> ITEMS =
            VesselAPI.createDeferredRegister(VesselRegistryKey.ITEM, MOD_ID);

    public static final VesselRegistrySupplier<VesselBlock> QUANTUM_GENERATOR =
            BLOCKS.register("quantum_generator", () ->
                    VesselAPI.createBlock("mymod:quantum_generator")
                            .hardness(4.5f)
                            .resistance(12.0f)
                            .luminance(15)
                            .sound("metal")
                            .requiresTool(true)
                            .energyStorage(50_000_000L)
                            .build()
            );

    public static final VesselRegistrySupplier<VesselItem> PLASMA_BLADE =
            ITEMS.register("plasma_blade", () ->
                    VesselAPI.createItem("mymod:plasma_blade")
                            .durability(1500)
                            .rarity("EPIC")
                            .fireResistant(true)
                            .energyCapacity(100_000L)
                            .onRightClick((player, world, hand) -> true)
                            .build()
            );
}
```

### 2. Mounting Deferred Registers

In your mod initializer:
```java
MyModContent.BLOCKS.register();
MyModContent.ITEMS.register();
```

### 3. Type-Safe Network Payloads (Client <-> Server)

```java
import dev.vessel.api.VesselAPI;
import dev.vessel.api.network.VesselPacketPayload;
import dev.vessel.api.network.VesselPayloadChannel;

public record SyncSpeedPacket(int speed, String mode) implements VesselPacketPayload {
    public static final String ID = "mymod:speed_sync";

    @Override
    public String getId() {
        return ID;
    }
}

// Register channel
VesselPayloadChannel channel = VesselAPI.createNetworkChannel("mymod:main");
channel.sendToServer(new SyncSpeedPacket(120, "OVERDRIVE"));
```

### 4. Zero-Overhead Event Subscriptions

```java
import dev.vessel.api.VesselAPI;
import dev.vessel.api.event.VesselEventPriority;
import dev.vessel.api.event.VesselSubscribe;

public class MyEventListener {
    @VesselSubscribe(priority = VesselEventPriority.HIGH)
    public void onPlayerJoin(Object event) {
        // High-speed direct MethodHandle dispatch (< 1.2 ns)
    }
}

// Register
VesselAPI.registerEvents(new MyEventListener());
```

---

## License & Attribution

- Licensed under the Apache License, Version 2.0. See [NOTICE](NOTICE) and [CREDITS.md](CREDITS.md) for complete upstream credits.
