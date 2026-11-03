package dev.vessel.api.block;

import java.util.Arrays;
import java.util.Objects;

/**
 * Evaluates block harvesting prerequisites, required tool classes, and mining tier levels
 * across Forge {@code IForgeBlock.getHarvestTool} and Vanilla/Fabric mining tags.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class BlockHarvestGovernor {

    public static final int DEFAULT_CAPACITY = 4096;

    public static final String TOOL_NONE = "none";
    public static final String TOOL_PICKAXE = "pickaxe";
    public static final String TOOL_AXE = "axe";
    public static final String TOOL_SHOVEL = "shovel";
    public static final String TOOL_HOE = "hoe";
    public static final String TOOL_SWORD = "sword";

    public static final int LEVEL_WOOD = 0;
    public static final int LEVEL_STONE = 1;
    public static final int LEVEL_IRON = 2;
    public static final int LEVEL_DIAMOND = 3;
    public static final int LEVEL_NETHERITE = 4;

    private final String[] harvestTools;
    private final int[] harvestLevels;

    /**
     * Constructs a harvest governor with default capacity and standard block profiles.
     */
    public BlockHarvestGovernor() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Constructs a harvest governor with custom block capacity.
     *
     * @param capacity maximum block ID range
     */
    public BlockHarvestGovernor(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive: " + capacity);
        }
        this.harvestTools = new String[capacity];
        this.harvestLevels = new int[capacity];
        Arrays.fill(this.harvestTools, TOOL_NONE);
        Arrays.fill(this.harvestLevels, -1);
        initializeStandardHarvestProfiles();
    }

    private void initializeStandardHarvestProfiles() {
        // Stone & Cobblestone (Wood pickaxe)
        registerHarvestTool(1, TOOL_PICKAXE, LEVEL_WOOD);
        registerHarvestTool(4, TOOL_PICKAXE, LEVEL_WOOD);

        // Iron Ore & Lapis (Stone pickaxe)
        registerHarvestTool(15, TOOL_PICKAXE, LEVEL_STONE);
        registerHarvestTool(21, TOOL_PICKAXE, LEVEL_STONE);

        // Gold & Diamond Ore (Iron pickaxe)
        registerHarvestTool(14, TOOL_PICKAXE, LEVEL_IRON);
        registerHarvestTool(56, TOOL_PICKAXE, LEVEL_IRON);

        // Obsidian (Diamond pickaxe)
        registerHarvestTool(49, TOOL_PICKAXE, LEVEL_DIAMOND);

        // Oak Log & Wood Planks (Axe)
        registerHarvestTool(17, TOOL_AXE, LEVEL_WOOD);
        registerHarvestTool(5, TOOL_AXE, LEVEL_WOOD);

        // Dirt & Sand & Gravel (Shovel)
        registerHarvestTool(3, TOOL_SHOVEL, LEVEL_WOOD);
        registerHarvestTool(12, TOOL_SHOVEL, LEVEL_WOOD);
        registerHarvestTool(13, TOOL_SHOVEL, LEVEL_WOOD);
    }

    /**
     * Registers harvest tool and mining level requirements for a specific block ID.
     *
     * @param blockId target block numerical ID
     * @param toolType tool category string
     * @param toolLevel minimum mining tier level
     */
    public void registerHarvestTool(int blockId, String toolType, int toolLevel) {
        if (blockId >= 0 && blockId < harvestTools.length) {
            harvestTools[blockId] = (toolType != null) ? toolType.toLowerCase() : TOOL_NONE;
            harvestLevels[blockId] = toolLevel;
        }
    }

    /**
     * Retrieves the tool type required to harvest this block.
     *
     * @param blockId numerical block ID
     * @return tool type string (or "none" if no tool is required)
     */
    public String getHarvestTool(int blockId) {
        if (blockId >= 0 && blockId < harvestTools.length) {
            return harvestTools[blockId];
        }
        return TOOL_NONE;
    }

    /**
     * Retrieves the minimum harvest tier level for this block.
     *
     * @param blockId numerical block ID
     * @return minimum tool level (or -1 if any tool or hand is sufficient)
     */
    public int getHarvestLevel(int blockId) {
        if (blockId >= 0 && blockId < harvestLevels.length) {
            return harvestLevels[blockId];
        }
        return -1;
    }

    /**
     * Determines whether the specified tool type and level can effectively harvest the block.
     *
     * @param blockId numerical block ID
     * @param toolType tool category of the equipped item
     * @param toolLevel mining level of the equipped item
     * @return true if the block can be harvested with drops
     */
    public boolean canHarvestBlock(int blockId, String toolType, int toolLevel) {
        if (blockId < 0 || blockId >= harvestTools.length) {
            return true;
        }

        String requiredTool = harvestTools[blockId];
        int requiredLevel = harvestLevels[blockId];

        // If no specific tool is required, block can always be harvested
        if (TOOL_NONE.equals(requiredTool) || requiredLevel < 0) {
            return true;
        }

        if (toolType == null) {
            return false;
        }

        return requiredTool.equalsIgnoreCase(toolType.trim()) && toolLevel >= requiredLevel;
    }
}
