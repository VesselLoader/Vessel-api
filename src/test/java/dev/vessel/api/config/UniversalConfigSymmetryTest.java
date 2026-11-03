package dev.vessel.api.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Enterprise test suite verifying Waves 3 & 4 Universal Configuration Substrate,
 * bridging Bukkit YamlConfiguration and Forge/NeoForge NightConfig ModConfigSpec.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner or nested classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public class UniversalConfigSymmetryTest {

    @Test
    @DisplayName("VesselConfigurationTree: Flat O(1) reads, typed fallbacks, hierarchy keys, and comments")
    void testVesselConfigurationTreeFlatOperationsAndTypedReads() {
        VesselConfigurationTree tree = new VesselConfigurationTree();

        tree.set("server.port", 25565);
        tree.set("server.ip", "127.0.0.1");
        tree.set("server.online-mode", true);
        tree.set("physics.gravity", -9.81);
        tree.set("limits.max-players", 500L);
        tree.set("features.enabled", List.of("folia", "starlight", "vstm"));
        tree.setComment("server.port", "Primary TCP socket port");

        // O(1) typed reads
        assertThat(tree.getInt("server.port")).isEqualTo(25565);
        assertThat(tree.getString("server.ip")).isEqualTo("127.0.0.1");
        assertThat(tree.getBoolean("server.online-mode")).isTrue();
        assertThat(tree.getDouble("physics.gravity")).isEqualTo(-9.81);
        assertThat(tree.getLong("limits.max-players")).isEqualTo(500L);
        assertThat(tree.getStringList("features.enabled")).containsExactly("folia", "starlight", "vstm");

        // Comments
        assertThat(tree.getComment("server.port")).isEqualTo("Primary TCP socket port");

        // Fallbacks
        assertThat(tree.getInt("non.existent", 8080)).isEqualTo(8080);
        assertThat(tree.getString("missing.key", "fallback")).isEqualTo("fallback");
        assertThat(tree.getBoolean("missing.flag", true)).isTrue();
        assertThat(tree.getDouble("missing.rate", 1.5)).isEqualTo(1.5);

        // Key hierarchy
        Set<String> deepKeys = tree.getKeys(true);
        assertThat(deepKeys).contains("server.port", "server.ip", "server.online-mode", "physics.gravity");

        Set<String> rootKeys = tree.getKeys(false);
        assertThat(rootKeys).containsExactlyInAnyOrder("server", "physics", "limits", "features");

        Set<String> serverChildren = tree.getSectionChildKeys("server");
        assertThat(serverChildren).containsExactlyInAnyOrder("port", "ip", "online-mode");
    }

    @Test
    @DisplayName("YamlToVesselParser: Parses indented YAML with comments, lists, and roundtrip serialization")
    void testYamlToVesselParserStreamingAndComments() {
        YamlToVesselParser parser = new YamlToVesselParser();

        String rawYaml = """
                # Server network configuration
                server:
                  port: 25565
                  host: "0.0.0.0"
                  motd: "VESSEL Standalone"
                  compression-threshold: 256
                  pvp: true
                plugins:
                  security:
                    enforce-whitelist: false
                  blacklist:
                    - "grief_item_1"
                    - "banned_block_2"
                """;

        VesselConfigurationTree tree = parser.parse(rawYaml);

        assertThat(tree.getInt("server.port")).isEqualTo(25565);
        assertThat(tree.getString("server.host")).isEqualTo("0.0.0.0");
        assertThat(tree.getString("server.motd")).isEqualTo("VESSEL Standalone");
        assertThat(tree.getBoolean("server.pvp")).isTrue();
        assertThat(tree.getBoolean("plugins.security.enforce-whitelist")).isFalse();
        assertThat(tree.getStringList("plugins.blacklist")).containsExactly("grief_item_1", "banned_block_2");

        // Roundtrip serialization
        String serialized = parser.toYaml(tree);
        assertThat(serialized).contains("port: 25565");
        assertThat(serialized).contains("motd: \"VESSEL Standalone\"");

        // Re-parse serialized output
        VesselConfigurationTree reParsed = parser.parse(serialized);
        assertThat(reParsed.getInt("server.port")).isEqualTo(25565);
        assertThat(reParsed.getStringList("plugins.blacklist")).containsExactly("grief_item_1", "banned_block_2");
    }

    @Test
    @DisplayName("NightConfigToVesselParser: Parses TOML section tables, inline arrays, and serializes back")
    void testNightConfigToVesselParserTomlTables() {
        NightConfigToVesselParser parser = new NightConfigToVesselParser();

        String rawToml = """
                # General mod configuration
                [general]
                enabled = true
                max_actors = 128
                name = "Create Contraptions"

                [general.physics]
                tick_rate = 20.0
                tags = ["kinetics", "rotational_stress"]
                """;

        VesselConfigurationTree tree = parser.parse(rawToml);

        assertThat(tree.getBoolean("general.enabled")).isTrue();
        assertThat(tree.getInt("general.max_actors")).isEqualTo(128);
        assertThat(tree.getString("general.name")).isEqualTo("Create Contraptions");
        assertThat(tree.getDouble("general.physics.tick_rate")).isEqualTo(20.0);
        assertThat(tree.getStringList("general.physics.tags")).containsExactly("kinetics", "rotational_stress");

        // Roundtrip serialization
        String tomlOut = parser.toToml(tree);
        assertThat(tomlOut).contains("[general]");
        assertThat(tomlOut).contains("max_actors = 128");

        VesselConfigurationTree reParsed = parser.parse(tomlOut);
        assertThat(reParsed.getInt("general.max_actors")).isEqualTo(128);
        assertThat(reParsed.getBoolean("general.enabled")).isTrue();
    }

    @Test
    @DisplayName("BukkitYamlSymmetryAdapter: Emulates Bukkit YamlConfiguration save/load and typed access")
    void testBukkitYamlSymmetryAdapterCompatibility(@TempDir Path tempDir) throws IOException {
        BukkitYamlSymmetryAdapter adapter = new BukkitYamlSymmetryAdapter();

        String yaml = """
                database:
                  host: "localhost"
                  port: 3306
                  pool-size: 10
                  ssl: false
                """;
        adapter.loadFromString(yaml);

        assertThat(adapter.getString("database.host")).isEqualTo("localhost");
        assertThat(adapter.getInt("database.port")).isEqualTo(3306);
        assertThat(adapter.getBoolean("database.ssl")).isFalse();

        // Mutate
        adapter.set("database.port", 3307);
        assertThat(adapter.getInt("database.port")).isEqualTo(3307);

        // File persistence
        File configFile = tempDir.resolve("config.yml").toFile();
        adapter.save(configFile);
        assertThat(configFile).exists();

        // Load via static factory
        BukkitYamlSymmetryAdapter loaded = BukkitYamlSymmetryAdapter.loadConfiguration(configFile);
        assertThat(loaded.getInt("database.port")).isEqualTo(3307);
        assertThat(loaded.getString("database.host")).isEqualTo("localhost");
    }

    @Test
    @DisplayName("ForgeConfigSpecSymmetryAdapter: Defines ModConfigSpec specs, path stacks, and bound ConfigValues")
    void testForgeConfigSpecSymmetryAdapterBuilderAndConfigValues(@TempDir Path tempDir) throws IOException {
        ForgeConfigSpecSymmetryAdapter adapter = new ForgeConfigSpecSymmetryAdapter();

        adapter.push("kinetics");
        adapter.comment("Maximum rotational RPM allowed before overload");
        ForgeConfigValueHolder<Integer> maxRpm = adapter.defineInRange("max_rpm", 256, 0, 1024);
        adapter.comment("Enables kinetic stress calculation");
        ForgeConfigValueHolder<Boolean> stressEnabled = adapter.define("stress_enabled", true);
        adapter.pop();

        // Assert defaults
        assertThat(maxRpm.get()).isEqualTo(256);
        assertThat(maxRpm.getPath()).containsExactly("kinetics", "max_rpm");
        assertThat(maxRpm.getDotPath()).isEqualTo("kinetics.max_rpm");
        assertThat(stressEnabled.get()).isTrue();

        // Mutate programmatically
        maxRpm.set(512);
        assertThat(maxRpm.get()).isEqualTo(512);

        // Save TOML to file
        File tomlFile = tempDir.resolve("create-common.toml").toFile();
        adapter.save(tomlFile);
        assertThat(tomlFile).exists();

        // Load overrides from TOML text
        String overrideToml = """
                [kinetics]
                max_rpm = 1000
                stress_enabled = false
                """;
        adapter.loadFromToml(overrideToml);

        assertThat(maxRpm.get()).isEqualTo(1000);
        assertThat(stressEnabled.get()).isFalse();
    }

    @Test
    @DisplayName("ConfigurationSyncPayload: Binary frame serialization and deserialization roundtrip")
    void testConfigurationSyncPayloadSerializationAndDeserialization() {
        byte[] rawConfig = "server.port = 25565\nserver.name = \"Test\"".getBytes(StandardCharsets.UTF_8);
        ConfigurationSyncPayload original = new ConfigurationSyncPayload(
                "vessel_core",
                "vessel.toml",
                ConfigFormatType.TOML,
                rawConfig,
                1700000000000L
        );

        byte[] serialized = original.serialize();
        assertThat(serialized.length).isGreaterThan(rawConfig.length);

        ConfigurationSyncPayload deserialized = ConfigurationSyncPayload.deserialize(serialized);

        assertThat(deserialized.modId()).isEqualTo("vessel_core");
        assertThat(deserialized.configName()).isEqualTo("vessel.toml");
        assertThat(deserialized.format()).isEqualTo(ConfigFormatType.TOML);
        assertThat(deserialized.timestampEpochMillis()).isEqualTo(1700000000000L);
        assertThat(deserialized.payloadBytes()).isEqualTo(rawConfig);
        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    @DisplayName("Rule 17 Compliance: Asserts zero inner, nested, or anonymous classes across api.config package")
    void testRule17ZeroInnerClassesVerification() {
        Class<?>[] classes = new Class<?>[]{
                ConfigFormatType.class,
                ConfigTokenEntry.class,
                ConfigurationSyncPayload.class,
                VesselConfigurationTree.class,
                YamlToVesselParser.class,
                NightConfigToVesselParser.class,
                BukkitYamlSymmetryAdapter.class,
                ForgeConfigValueHolder.class,
                ForgeConfigSpecSymmetryAdapter.class
        };

        for (Class<?> clazz : classes) {
            assertThat(clazz.getDeclaredClasses())
                    .as("Class " + clazz.getSimpleName() + " must have zero inner/nested classes (Rule 17)")
                    .isEmpty();
            assertThat(clazz.isAnonymousClass())
                    .as("Class " + clazz.getSimpleName() + " must not be anonymous")
                    .isFalse();
            assertThat(clazz.isMemberClass())
                    .as("Class " + clazz.getSimpleName() + " must be a top-level class")
                    .isFalse();
        }
    }
}
