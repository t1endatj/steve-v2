package com.steve.v2.forge.action;

import com.steve.v2.forge.config.SteveConfig;
import com.steve.v2.forge.entity.SteveEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Shared follow movement. Recoveries only use supported, collision-free loaded ground. */
public final class FollowController {
    private final SteveEntity steve;
    private int ticks, stuckTicks, recoveries;
    private Vec3 checkpoint;
    private String status = "Starting";
    public FollowController(SteveEntity steve) { this.steve = steve; }
    public void reset() {
        ticks = stuckTicks = recoveries = 0;
        checkpoint = steve.position(); status = "Starting";
        steve.setFlying(false); steve.getNavigation().stop(); steve.setDeltaMovement(Vec3.ZERO);
    }
    public void tick(Player player) {
        if (player == null || !player.isAlive() || player.isRemoved() || player.isSpectator()) {
            steve.getNavigation().stop(); status = "Player unavailable"; return;
        }
        ticks++;
        boolean airborne = player.getAbilities().flying;
        double distance = airborne ? Math.sqrt(Math.pow(steve.getX() - player.getX(), 2) + Math.pow(steve.getZ() - player.getZ(), 2))
            : steve.distanceTo(player);
        if (distance <= 3.0 && !steve.isInWall()) {
            steve.getNavigation().stop(); checkpoint = steve.position(); stuckTicks = 0;
            status = airborne ? "Waiting below flying player" : "Waiting near player"; return;
        }
        if (ticks % 10 != 0) return;
        if (checkpoint == null) checkpoint = steve.position();
        double progress = steve.position().distanceToSqr(checkpoint);
        checkpoint = steve.position();
        stuckTicks = progress < 0.04 ? stuckTicks + 10 : 0;
        boolean moving = steve.getNavigation().moveTo(player, 1.15);
        status = moving ? "Walking" : "No reachable path";
        if (SteveConfig.ASSISTANT_MODE.get() && (steve.isInWall() || distance > 24 || stuckTicks >= 60)) {
            BlockPos safe = findSafePosition(steve, player.blockPosition());
            if (safe != null) {
                steve.getNavigation().stop(); steve.setFlying(false); steve.setDeltaMovement(Vec3.ZERO);
                steve.teleportTo(safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5);
                checkpoint = steve.position(); stuckTicks = 0; recoveries++;
                status = "Recovered near player";
            } else status = "Blocked: no safe ground near player";
        }
    }
    public String status() { return status + (recoveries == 0 ? "" : " (recoveries=" + recoveries + ")"); }
    public static BlockPos findSafePosition(SteveEntity steve, BlockPos target) {
        ServerLevel level = (ServerLevel) steve.level();
        for (int radius = 2; radius <= 4; radius++) {
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                for (int dy : new int[]{0, -1, 1, -2, 2, -3, 3}) {
                    BlockPos pos = target.offset(dx, dy, dz);
                    if (safe(steve, level, pos)) return pos;
                }
            }
        }
        // Follow a creative player on the surface below instead of teleporting into unsupported air.
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) < 2) continue;
            BlockPos column = target.offset(dx, 0, dz);
            if (!level.hasChunkAt(column)) continue;
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            if (safe(steve, level, surface)) return surface;
        }
        return null;
    }
    private static boolean safe(SteveEntity steve, ServerLevel level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos) || level.isOutsideBuildHeight(pos.above())
            || !level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)) return false;
        var floor = level.getBlockState(pos.below());
        if (!floor.isFaceSturdy(level, pos.below(), Direction.UP) || !floor.getFluidState().isEmpty()
            || floor.is(Blocks.MAGMA_BLOCK) || floor.is(Blocks.CAMPFIRE) || floor.is(Blocks.SOUL_CAMPFIRE)
            || floor.is(Blocks.CACTUS)) return false;
        var feet = level.getBlockState(pos); var head = level.getBlockState(pos.above());
        if (!feet.getFluidState().isEmpty() || !head.getFluidState().isEmpty()
            || feet.is(Blocks.FIRE) || feet.is(Blocks.SOUL_FIRE) || feet.is(Blocks.POWDER_SNOW)
            || feet.is(Blocks.SWEET_BERRY_BUSH) || head.is(Blocks.FIRE) || head.is(Blocks.SOUL_FIRE)) return false;
        var destination = steve.getBoundingBox().move(pos.getX() + 0.5 - steve.getX(), pos.getY() - steve.getY(), pos.getZ() + 0.5 - steve.getZ());
        return level.noCollision(steve, destination);
    }
}
