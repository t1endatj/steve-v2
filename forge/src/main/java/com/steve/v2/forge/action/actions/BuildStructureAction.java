package com.steve.v2.forge.action.actions;

import com.steve.v2.core.action.ActionResult;
import com.steve.v2.core.task.Task;
import com.steve.v2.forge.action.CollaborativeBuildManager;
import com.steve.v2.forge.config.SteveConfig;
import com.steve.v2.forge.entity.SteveEntity;
import com.steve.v2.forge.structure.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.*;

public final class BuildStructureAction extends BaseAction {
    private CollaborativeBuildManager.Build build;
    private final String worker;
    private int ticks;
    public BuildStructureAction(SteveEntity steve, Task task) {
        super(steve, task); worker = steve.getUUID().toString();
    }
    @Override protected void onStart() {
        if (!SteveConfig.ASSISTANT_MODE.get()) {
            result = ActionResult.failure("Building currently requires assistantMode; survival inventory is not implemented", false); return;
        }
        var level = (ServerLevel) steve.level();
        var player = steve.getOwnerPlayer();
        BlockPos target = steve.blockPosition().offset(3, 0, 3);
        if (player != null) {
            var p = player.position().add(player.getLookAngle().multiply(12, 0, 12));
            target = BlockPos.containing(p);
        }
        if (!level.hasChunkAt(target)) { result = ActionResult.failure("Build area is not loaded"); return; }
        BlockPos anchor = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target);
        if (task.getParameter("x") != null) anchor = new BlockPos(task.getIntParameter("x", 0), task.getIntParameter("y", 0), task.getIntParameter("z", 0));
        String type = task.getStringParameter("structure").toLowerCase(Locale.ROOT);
        build = CollaborativeBuildManager.find(level, steve.getOwnerId(), type, anchor);
        if (build == null) {
            @SuppressWarnings("unchecked") var dims = (List<Number>) task.getParameter("dimensions");
            List<Block> materials = new ArrayList<>();
            for (Object name : (List<?>) task.getParameter("blocks")) {
                var id = ResourceLocation.tryParse(name.toString());
                if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
                    result = ActionResult.failure("Unknown block: " + name, false); return;
                }
                materials.add(BuiltInRegistries.BLOCK.get(id));
            }
            final BlockPos origin = anchor;
            var plan = StructureTemplateLoader.load(level, type, origin).orElseGet(() ->
                StructureGenerators.generate(type, origin, dims.get(0).intValue(), dims.get(1).intValue(), dims.get(2).intValue(), materials));
            // A coordinate can occur multiple times in generators (walls/windows/roof). Last write wins.
            Map<BlockPos, BlockPlacement> unique = new LinkedHashMap<>();
            for (var placement : plan) unique.put(placement.pos, placement);
            for (var entry : new ArrayList<>(unique.entrySet())) {
                var placement = entry.getValue();
                if (placement.block instanceof DoorBlock && !placement.state.getValue(DoorBlock.HALF).equals(net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER)) {
                    var below = unique.get(placement.pos.below());
                    if (below != null && below.block == placement.block && below.state.getValue(DoorBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) {
                        unique.put(placement.pos, new BlockPlacement(placement.pos, placement.state.setValue(DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER)));
                    }
                }
            }
            plan = new ArrayList<>(unique.values());
            plan.sort(Comparator.comparingInt(p -> p.pos.getY()));
            if (plan.isEmpty() || plan.size() > 65536) { result = ActionResult.failure("Invalid build size", false); return; }
            for (var placement : plan) {
                if (level.isOutsideBuildHeight(placement.pos) || !level.getWorldBorder().isWithinBounds(placement.pos)
                    || !level.hasChunkAt(placement.pos)) {
                    result = ActionResult.failure("Build extends outside loaded chunks, world height or border", false); return;
                }
            }
            build = CollaborativeBuildManager.create(level, steve.getOwnerId(), type, anchor, plan);
        }
        steve.setFlying(true);
    }
    @Override protected void onTick() {
        ticks++;
        build.lastActive = steve.level().getGameTime();
        if (ticks > 120000) { result = ActionResult.failure("Build timeout"); return; }
        if (build.work.isComplete()) { result = ActionResult.success("Built " + build.type); return; }
        var placement = build.work.claim(worker);
        if (placement == null) return; // Other workers still own the remaining placements.
        var level = steve.level();
        if (!level.hasChunkAt(placement.pos)) { result = ActionResult.failure("Build chunk unloaded"); return; }
        if (level.getBlockState(placement.pos).getDestroySpeed(level, placement.pos) < 0) {
            result = ActionResult.failure("Cannot replace unbreakable block at " + placement.pos, false); return;
        }
        if (steve.blockPosition().distSqr(placement.pos) > 25) steve.teleportTo(placement.pos.getX() + 2.5, placement.pos.getY(), placement.pos.getZ() + 2.5);
        var state = placement.state;
        if (!level.getBlockState(placement.pos).equals(state) && !level.setBlock(placement.pos, state, 3)) {
            result = ActionResult.failure("Failed to place block at " + placement.pos); return;
        }
        steve.swing(InteractionHand.MAIN_HAND, true);
        build.work.commit(worker);
        if (ticks % 100 == 0) steve.sendChatMessage("Building " + build.type + ": " + build.work.completed() + "/" + build.work.total());
        if (build.work.isComplete()) result = ActionResult.success("Built " + build.type);
    }
    @Override protected void onCancel() {
        if (build != null) build.work.release(worker);
        steve.setFlying(false); steve.getNavigation().stop();
    }
    @Override public String getDescription() {
        return build == null ? "Preparing build" : "Build " + build.type + " " + build.work.completed() + "/" + build.work.total();
    }
}
