package milkucha.trmt.block;

import milkucha.trmt.TRMTBlocks;
import milkucha.trmt.TRMTConfig;
import milkucha.trmt.erosion.BlockThresholds;
import milkucha.trmt.erosion.ChunkErosionMap;
import milkucha.trmt.erosion.ErosionEntry;
import milkucha.trmt.erosion.ErosionMapManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Grass block produced by foot-traffic erosion.
 * Stores a FACING direction (established when grass first erodes) for UV rotation,
 * and a STAGE (0–4) matching eroded_grass_block_s0 through eroded_grass_block_s4 models.
 * Never placed by players or generated naturally — only set by the erosion system.
 */
public class ErodedGrassBlock extends Block {

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    /**
     * Visual erosion stage (0–4).
     * 0 = least eroded (grass_block_eroded_0 model), 4 = most eroded (grass_block_eroded_4 model).
     * Maps to old grass erosion stages 1–5: stage+1 is used for de-erosion timeout lookup.
     */
    public static final IntProperty STAGE = IntProperty.of("stage", 0, 4);

    public ErodedGrassBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.SOUTH).with(STAGE, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, STAGE);
    }

    /** Handles neighbor updates when this block is placed or updated next to another block. */
    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        if (world.isClient) return;
        // If the block above is opaque, revert to vanilla grass and clear tracking data.
        if (!world.getBlockState(pos.up()).isOpaque()) return;
        world.setBlockState(pos, Blocks.GRASS_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
        ErosionMapManager.getInstance().removeEntry(pos);
    }

    /** Handles random ticking for erosion simulation (De-erosion). */
    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Check if the block above is opaque before proceeding with erosion logic.
        if (!world.getBlockState(pos.up()).isOpaque()) return;

        if (!TRMTConfig.get().deErosion.grassEnabled) return;
        ChunkErosionMap chunkMap = ErosionMapManager.getInstance().getChunkMap(new ChunkPos(pos));
        ErosionEntry entry = chunkMap != null ? chunkMap.getEntry(pos) : null;

        int blockStage = state.get(STAGE);
        long currentTime = world.getTime();
        // Map block STAGE 0–4 to old grass stages 1–5 for the per-stage timeout config.
        long timeout = BlockThresholds.getGrassDeErosionTimeout(blockStage + 1);
        if (BlockThresholds.isIsolated(world, pos, ErosionMapManager.getInstance())) {
            timeout /= 2; // Halve timeout if isolated
        }
        // Check cooldown period: If current time is before the required time, do nothing.
        if (entry != null && currentTime - entry.getLastTouchedGameTime() <= timeout) return;

        long newCooldownTime = (entry != null) ? entry.getLastTouchedGameTime() + timeout : currentTime;

        if (blockStage > 0) {
            // De-erode: Step down one visual stage, preserving rotation.
            world.setBlockState(pos, state.with(STAGE, Math.max(0, blockStage - 1)), Block.NOTIFY_ALL);
            ErosionMapManager.getInstance().removeEntry(pos);
            // Write the new cooldown time for the next check.
            ErosionMapManager.getInstance().writeCooldownEntry(pos, TRMTBlocks.ERODED_GRASS_BLOCK, newCooldownTime);
        } else {
            // Stage 0 → revert to vanilla grass block.
            world.setBlockState(pos, Blocks.GRASS_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
            ErosionMapManager.getInstance().removeEntry(pos);
        }
    }
}