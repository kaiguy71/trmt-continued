package milkucha.trmt.block;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;

/**
 * Container class for all custom, eroded block types in the mod.
 * These blocks should ideally be registered via data components/JSON files 
 * rather than programmatically, but this class maintains the necessary structure 
 * and properties for the Java side to reference them correctly during runtime checks.
 */
public final class TRMTBlocks {

    // --- Block Definitions (Must match JSON definitions) ---

    /** The base block state used when a block is first placed or fully restored. */
    public static final ErodedGrassBlock ERODED_GRASS_BLOCK = new ErodedGrassBlock(Settings.copy(Blocks.GRASS_BLOCK));
    public static final ErodedDirtBlock ERODED_DIRT = new ErodedDirtBlock(Settings.copy(Blocks.DIRT));
    public static final ErodedCoarseDirtBlock ERODED_COARSE_DIRT = new ErodedCoarseDirtBlock(Settings.copy(Blocks.COARSE_DIRT));
    public static final ErodedSandBlock ERODED_SAND = new ErodedSandBlock(Settings.copy(Blocks.SAND));

    // Placeholder for any other eroded blocks (e.g., Snow, Netherrack)
    // public static final ErodedSnowBlock ERODED_SNOW = new ErodedSnowBlock(Settings.copy(Blocks.SNOW_BLOCK));


    private TRMTBlocks() {}

    /** Helper method to create a block instance with copied settings. */
    public static Block copyBlock(Block original) {
        return (Block) original.getDefaultState().getBlock();
    }
}