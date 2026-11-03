package dev.vessel.api.command;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive JUnit 5 test suite validating the Universal Command Substrate:
 * VesselCommandHub, BukkitToBrigadierTranspiler, CommandPermissionFilter,
 * TabCompletionCache, BukkitCommandSymmetryAdapter, and Rule 17 compliance.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class UniversalCommandSymmetryTest {

    private VesselCommandHub commandHub;
    private CommandPermissionFilter permissionFilter;
    private TabCompletionCache tabCache;
    private BukkitToBrigadierTranspiler transpiler;

    @BeforeEach
    void setUp() {
        permissionFilter = new CommandPermissionFilter();
        tabCache = new TabCompletionCache();
        transpiler = new BukkitToBrigadierTranspiler();
        commandHub = new VesselCommandHub(permissionFilter, tabCache, transpiler);
    }

    @AfterEach
    void tearDown() {
        if (commandHub != null) {
            commandHub.clear();
        }
        BukkitCommandSymmetryAdapter.clear();
    }

    @Test
    @DisplayName("VesselCommandHub registers and executes command with arguments")
    void testCommandRegistrationAndExecution() {
        MockBukkitCommandExecutor executor = new MockBukkitCommandExecutor(
                "vessel",
                "VESSEL Sovereign Core Management",
                "",
                "v"
        );
        MockCommandSender sender = new MockCommandSender("Shiroz", false);

        commandHub.registerCommand("vessel", executor);

        assertThat(commandHub.hasCommand("vessel")).isTrue();
        assertThat(commandHub.hasCommand("v")).isTrue();

        boolean executed = commandHub.executeCommand(sender, "/vessel reload world");
        assertThat(executed).isTrue();
        assertThat(executor.getExecutionCount()).isEqualTo(1);
        assertThat(executor.getLastLabel()).isEqualTo("vessel");
        assertThat(executor.getLastArgs()).containsExactly("reload", "world");

        // Alias execution
        boolean aliasExecuted = commandHub.executeCommand(sender, "v status");
        assertThat(aliasExecuted).isTrue();
        assertThat(executor.getExecutionCount()).isEqualTo(2);
        assertThat(executor.getLastLabel()).isEqualTo("v");
        assertThat(executor.getLastArgs()).containsExactly("status");
    }

    @Test
    @DisplayName("VesselCommandHub unregisters command cleanly")
    void testCommandUnregistration() {
        MockBukkitCommandExecutor executor = new MockBukkitCommandExecutor("spawn", "Teleport", "");
        MockCommandSender sender = new MockCommandSender("Player1", false);

        commandHub.registerCommand("spawn", executor);
        assertThat(commandHub.hasCommand("spawn")).isTrue();

        boolean executedBefore = commandHub.executeCommand(sender, "spawn");
        assertThat(executedBefore).isTrue();

        boolean removed = commandHub.unregisterCommand("spawn");
        assertThat(removed).isTrue();
        assertThat(commandHub.hasCommand("spawn")).isFalse();

        boolean executedAfter = commandHub.executeCommand(sender, "spawn");
        assertThat(executedAfter).isFalse();
    }

    @Test
    @DisplayName("CommandPermissionFilter evaluates direct, wildcard, and operator rights")
    void testPermissionFilterDirectAndWildcard() {
        MockCommandSender regular = new MockCommandSender("Player", false);
        MockCommandSender admin = new MockCommandSender("Admin", false);
        MockCommandSender op = new MockCommandSender("Owner", true);

        admin.addPermission("vessel.admin.*");

        // Exact match
        permissionFilter.assignPermission(regular, "vessel.player.home");
        assertThat(permissionFilter.hasPermission(regular, "vessel.player.home")).isTrue();
        assertThat(permissionFilter.hasPermission(regular, "vessel.player.warp")).isFalse();

        // Hierarchical wildcard match
        assertThat(permissionFilter.hasPermission(admin, "vessel.admin.reload")).isTrue();
        assertThat(permissionFilter.hasPermission(admin, "vessel.admin.stop")).isTrue();
        assertThat(permissionFilter.hasPermission(admin, "vessel.player.home")).isFalse();

        // Operator bypass
        assertThat(permissionFilter.hasPermission(op, "vessel.secret.anything")).isTrue();
    }

    @Test
    @DisplayName("VesselCommandHub blocks execution if permission is denied")
    void testPermissionFilterEnforcement() {
        MockBukkitCommandExecutor restrictedExecutor = new MockBukkitCommandExecutor(
                "ban",
                "Ban a player",
                "vessel.admin.ban"
        );
        MockCommandSender unauthorizedSender = new MockCommandSender("Guest", false);
        MockCommandSender authorizedSender = new MockCommandSender("Moderator", false);
        authorizedSender.addPermission("vessel.admin.ban");

        commandHub.registerCommand("ban", restrictedExecutor);

        boolean denied = commandHub.executeCommand(unauthorizedSender, "ban Steve");
        assertThat(denied).isFalse();
        assertThat(restrictedExecutor.getExecutionCount()).isEqualTo(0);

        boolean allowed = commandHub.executeCommand(authorizedSender, "ban Steve");
        assertThat(allowed).isTrue();
        assertThat(restrictedExecutor.getExecutionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("TabCompletionCache prefix matching and deletion")
    void testTabCompletionCachePrefixQuery() {
        tabCache.insert("vessel");
        tabCache.insert("version");
        tabCache.insert("vfs");
        tabCache.insert("spawn");
        tabCache.insert("sethome");

        assertThat(tabCache.size()).isEqualTo(5);
        assertThat(tabCache.contains("vessel")).isTrue();

        List<String> vMatches = tabCache.complete("ve");
        assertThat(vMatches).containsExactly("version", "vessel");

        List<String> sMatches = tabCache.complete("s");
        assertThat(sMatches).containsExactly("sethome", "spawn");

        // Removal
        boolean removed = tabCache.remove("vfs");
        assertThat(removed).isTrue();
        assertThat(tabCache.size()).isEqualTo(4);
        assertThat(tabCache.contains("vfs")).isFalse();
    }

    @Test
    @DisplayName("VesselCommandHub resolves tab completions for roots and sub-arguments")
    void testVesselCommandHubTabCompletions() {
        MockBukkitCommandExecutor executor = new MockBukkitCommandExecutor("vessel", "Core", "");
        MockCommandSender sender = new MockCommandSender("Shiroz", true);

        commandHub.registerCommand("vessel", executor);
        commandHub.registerCommand("version", executor);
        commandHub.registerCommand("vanilla", executor);

        // Root completions
        List<String> rootCompletions = commandHub.getTabCompletions(sender, "ve");
        assertThat(rootCompletions).contains("version", "vessel");

        // Sub-arguments delegating to executor onTabComplete
        List<String> subCompletions = commandHub.getTabCompletions(sender, "vessel ");
        assertThat(subCompletions).containsExactly("sub1", "sub2", "reload");
        assertThat(executor.getTabCompleteCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("BukkitToBrigadierTranspiler converts Bukkit command metadata into node descriptor")
    void testBukkitToBrigadierTranspiler() {
        MockBukkitCommandExecutor executor = new MockBukkitCommandExecutor(
                "home",
                "Teleport home",
                "essentials.home",
                "h", "homies"
        );

        BrigadierTranspiledNode node = transpiler.transpile("home", executor);

        assertThat(node).isNotNull();
        assertThat(node.commandName()).isEqualTo("home");
        assertThat(node.description()).isEqualTo("Teleport home");
        assertThat(node.permission()).isEqualTo("essentials.home");
        assertThat(node.aliases()).containsExactly("h", "homies");
    }

    @Test
    @DisplayName("BukkitCommandSymmetryAdapter opcode receptors for setExecutor and onCommand")
    void testBukkitCommandSymmetryAdapterOpcodeReceptors() {
        MockBukkitCommandExecutor executor = new MockBukkitCommandExecutor("warp", "Warps", "");
        MockCommandSender sender = new MockCommandSender("Shiroz", true);
        Object fakePluginCommand = new Object();

        BukkitCommandSymmetryAdapter.setExecutor(fakePluginCommand, executor);
        assertThat(BukkitCommandSymmetryAdapter.getExecutor(fakePluginCommand)).isSameAs(executor);

        boolean directResult = BukkitCommandSymmetryAdapter.onCommand(
                executor,
                sender,
                null,
                "warp",
                new String[]{"lobby"}
        );
        assertThat(directResult).isTrue();
        assertThat(executor.getExecutionCount()).isEqualTo(1);
        assertThat(executor.getLastLabel()).isEqualTo("warp");
        assertThat(executor.getLastArgs()).containsExactly("lobby");

        List<String> tabResults = BukkitCommandSymmetryAdapter.onTabComplete(
                executor,
                sender,
                null,
                "warp",
                new String[]{""}
        );
        assertThat(tabResults).contains("sub1", "sub2", "reload");
    }

    @Test
    @DisplayName("Strict Rule 17 Verification: All command package classes must have zero inner classes")
    void testRule17ZeroInnerClassesCompliance() {
        Class<?>[] commandClasses = new Class<?>[]{
                TabCompletionTrieNode.class,
                TabCompletionCache.class,
                CommandPermissionFilter.class,
                BrigadierTranspiledNode.class,
                BukkitToBrigadierTranspiler.class,
                VesselCommandHub.class,
                BukkitCommandSymmetryAdapter.class
        };

        for (Class<?> clazz : commandClasses) {
            assertThat(clazz.getDeclaredClasses())
                    .as("Class %s must have 0 inner/nested classes per Rule 17", clazz.getSimpleName())
                    .isEmpty();
        }
    }
}
