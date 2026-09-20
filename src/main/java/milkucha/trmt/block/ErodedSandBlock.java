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
import net.minecraft.block.ShapeContext;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class ErodedSandBlock extends Block {

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final IntProperty STAGE = IntProperty.of("stage", 0, 4);
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    private static final VoxelShape[] COLLISION_SHAPES = {
        Block.createCuboidShape(0, 0, 0, 16, 16, 16), // stage 0
        Block.createCuboidShape(0, 0, 0, 16, 10, 16), // stage 1
        Block.createCuboidShape(0, 0, 0, 16, 10, 16), // stage 2
        Block.createCuboidShape(0, 0, 0, 16, 10, 16), // stage 3
        Block.createCuboidShape(0, 0, 0, 16, 10, 16)  // stage 4
    };

    private static final VoxelShape[] OUTLINE_SHAPES = {
        Block.createCuboidShape(0, 0, 0, 16, 16, 16), // stage 0
        Block.createCuboidShape(0, 0, 0, 16, 14, 16), // stage 1 — matches model height
        Block.createCuboidShape(0, 0, 0, 16, 14, 16), // stage 2 — matches model height
        Block.createCuboidShape(0, 0, 0, 16, 12, 16), // stage 3 — matches model height
        Block.createCuboidShape(0, 0, 0, 16, 10, 16)  // stage 4 — matches model height (same as collision)
    };

    public ErodedSandBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                .with(FACING, Direction.SOUTH)
                .with(STAGE, 0)
                .with(WATERLOGGED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(DirectionProperty.field("facing"));
        // Using IntProperty directly as it's a common pattern for state tracking
        builder.add(IntProperty.field("erosionLevel", 0)); 
    }

    @Override
    public BlockState getBlockState(BlockPos pos, World world, BlockState state) {
        // Ensure the block state reflects the correct facing and stage based on erosion data.
        ChunkErosionMap chunkMap = ErosionMapManager.getInstance().getChunkMap(new ChunkPos(pos));
        ErosionEntry entry = chunkMap != null ? chunkMap.getEntry(pos) : null;

        if (entry != null) {
            return state.with(FACING, entry.getFacing()).with(STAGE, entry.getStage());
        }
        return state;
    }

    // --- Waterloggable Overrides (Modernized for 26.3) ---

    /** Returns the fluid state based on the block's waterlogged property. */
    @Override
    public net.minecraft.fluid.FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    /** Checks if the block can be filled with a fluid (only sunken stages). */
    @Override
    public boolean canFillWithFluid(BlockView world, BlockPos pos, BlockState state, Fluid fluid) {
        return state.get(STAGE) > 0 && !state.get(WATERLOGGED) && fluid == Fluids.WATER;
    }

    /** Attempts to fill the block with water and schedules a tick. */
    @Override
    public boolean tryFillWithFluid(WorldAccess world, BlockPos pos, BlockState state, net.minecraft.fluid.FluidState fluidState) {
        if (state.get(STAGE) > 0 && !state.get(WATERLOGGED)) {
            if (!world.isClient()) {
                // Set the block state and schedule a tick to propagate fluids correctly.
                world.setBlockState(pos, state.with(WATERLOGGED, true), Block.NOTIFY_ALL);
                world.scheduleFluidTick(pos, fluidState.getFluid(), fluidState.getFluid().getTickRate(world));
            }
            return true;
        }
        return false;
    }

    /** Attempts to drain water from the block. */
    @Override
    public ItemStack tryDrainFluid(WorldAccess world, BlockPos pos, BlockState state) {
        if (state.get(STAGE) > 0 && state.get(WATERLOGGED)) {
            world.setBlockState(pos, state.with(WATERLOGGED, false), Block.NOTIFY_ALL);
            return new ItemStack(net.minecraft.item.Items.WATER_BUCKET);
        }
        return ItemStack.EMPTY;
    }

    /** Schedules fluid ticking when the block is waterlogged. */
    @Override
    public void tickFluid(BlockState state, WorldAccess world, BlockPos pos, BlockView view) {
        if (state.get(WATERLOGGED)) {
            world.scheduleFluidTick(pos, Fluids.WATER, 1);
        }
    }

    /** Schedules fluid ticking when the block is waterlogged (Overload for compatibility). */
    @Override
    public void tickFluid(BlockState state, WorldAccess world, BlockPos pos, BlockView view) {
        if (state.get(WATERLOGGED)) {
            world.scheduleFluidTick(pos, Fluids.WATER, 1);
        }
    }

    /** Handles fluid ticking during neighbor updates. */
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    // --- Block Overrides (Lifecycle) ---

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, net.minecraft.block.Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        if (world.isClient) return;
        // Only react if the block above is opaque and we are not in a client context.
        if (!world.getBlockState(pos.up()).isOpaque()) return; 
        
        // Reset to base sand state upon neighbor interaction, simulating removal of erosion effect.
        world.setBlockState(pos, Blocks.SAND.getDefaultState(), Block.NOTIFY_ALL);
        ErosionMapManager.getInstance().removeEntry(pos);
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Check if the block above is opaque (i.e., not air/empty space).
        if (!world.getBlockState(pos.up()).isOpaque()) return;

        if (!TRMTConfig.get().deErosion.sandEnabled) return;
        
        ChunkErosionMap chunkMap = ErosionMapManager.getInstance().getChunkMap(new ChunkPos(pos));
        ErosionEntry entry = chunkMap != null ? chunkMap.getEntry(pos) : null;

        int stage = state.get(STAGE);
        long currentTime = world.getTime();
        // Determine the required cooldown time based on current stage.
        long timeout = BlockThresholds.getSandDeErosionTimeout(stage);
        if (BlockThresholds.isIsolated(world, pos, ErosionMapManager.getInstance())) {
            timeout /= 2; // Halve timeout if isolated
        }
        
        // Check if the cooldown period has passed since last touch/check.
        long requiredTime = (entry != null) ? entry.getLastTouchedGameTime() + timeout : currentTime;
        if (currentTime < requiredTime) return;

        // --- Erosion Logic ---
        if (stage > 0) {
            // De-erode: Move to the previous stage, preserving waterlogged state if it was sunken enough.
            boolean keepWaterlogged = stage > 1 && state.get(WATERLOGGED);
            world.setBlockState(pos, state.with(STAGE, Math.max(0, stage - 1)).with(WATERLOGGED, keepWaterlogged), Block.NOTIFY_ALL);
            ErosionMapManager.getInstance().removeEntry(pos);
            // Write the new cooldown time for the next check.
            ErosionMapManager.getInstance().writeCooldownEntry(pos, TRMTBlocks.ERODED_SAND, currentTime);
        } else {
            // If stage is 0 (full height), revert to vanilla sand and clear tracking data.
            world.setBlockState(pos, Blocks.SAND.getDefaultState(), Block.NOTIFY_ALL);
            ErosionMapManager.getInstance().removeEntry(pos);
        }
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return OUTLINE_SHAPES[state.get(STAGE)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return COLLISION_SHAPES[state.get(STAGE)];
    }
}