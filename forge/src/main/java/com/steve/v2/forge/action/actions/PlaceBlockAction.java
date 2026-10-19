package com.steve.v2.forge.action.actions;

import com.steve.v2.core.action.ActionResult;
import com.steve.v2.core.task.Task;
import com.steve.v2.forge.entity.SteveEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class PlaceBlockAction extends BaseAction {
    private Block blockToPlace;
    private BlockPos targetPos;
    private int ticksRunning;
    private static final int MAX_TICKS = 200;

    public PlaceBlockAction(SteveEntity steve, Task task) {
        super(steve, task);
    }

    @Override
    protected void onStart() {
        if (!com.steve.v2.forge.config.SteveConfig.ASSISTANT_MODE.get()) {
            result = ActionResult.failure("Placing blocks currently requires assistantMode; inventory is not implemented", false);
            return;
        }
        String blockName = task.getStringParameter("block");
        int x = task.getIntParameter("x", 0);
        int y = task.getIntParameter("y", 0);
        int z = task.getIntParameter("z", 0);
        
        targetPos = new BlockPos(x, y, z);
        ticksRunning = 0;
        
        blockToPlace = parseBlock(blockName);
        
        if (blockToPlace == null || blockToPlace == Blocks.AIR) {
            result = ActionResult.failure("Invalid block type: " + blockName);
            return;
        }
        
    }

    @Override
    protected void onTick() {
        ticksRunning++;
        
        if (ticksRunning > MAX_TICKS) {
            result = ActionResult.failure("Place block timeout");
            return;
        }
        
        if (!steve.blockPosition().closerThan(targetPos, 5.0)) {
            steve.getNavigation().moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 1.0);
            return;
        }
        
        BlockState currentState = steve.level().getBlockState(targetPos);
        if (!currentState.isAir() && !currentState.liquid()) {
            result = ActionResult.failure("Position is not empty");
            return;
        }
        
        if (!steve.level().hasChunkAt(targetPos) || !steve.level().getWorldBorder().isWithinBounds(targetPos)) {
            result = ActionResult.failure("Placement is outside loaded chunks or world border", false);
            return;
        }
        result = steve.level().setBlock(targetPos, blockToPlace.defaultBlockState(), 3)
            ? ActionResult.success("Placed " + blockToPlace.getName().getString())
            : ActionResult.failure("Block placement failed");
    }

    @Override
    protected void onCancel() {
        steve.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Place " + blockToPlace.getName().getString() + " at " + targetPos;
    }

    private Block parseBlock(String blockName) {
        blockName = blockName.toLowerCase().replace(" ", "_");
        if (!blockName.contains(":")) {
            blockName = "minecraft:" + blockName;
        }
        ResourceLocation resourceLocation = ResourceLocation.parse(blockName);
        return BuiltInRegistries.BLOCK.get(resourceLocation);
    }
}


