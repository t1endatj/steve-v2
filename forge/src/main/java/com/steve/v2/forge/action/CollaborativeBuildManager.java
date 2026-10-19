package com.steve.v2.forge.action;

import com.steve.v2.core.agent.WorkQueue;
import com.steve.v2.forge.structure.BlockPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import java.util.*;

/** Per-world, per-owner work groups. Accessed only from the server thread. */
public final class CollaborativeBuildManager {
    public static final class Build {
        public final String type;
        public final UUID owner;
        public final BlockPos anchor;
        public final WorkQueue<BlockPlacement> work;
        public long lastActive;
        Build(String type, UUID owner, BlockPos anchor, List<BlockPlacement> plan, long time) {
            this.type = type; this.owner = owner; this.anchor = anchor.immutable();
            work = new WorkQueue<>(plan); lastActive = time;
        }
    }
    private static final Map<ServerLevel, List<Build>> WORLDS = new WeakHashMap<>();
    public static Build find(ServerLevel level, UUID owner, String type, BlockPos anchor) {
        var builds = WORLDS.computeIfAbsent(level, l -> new ArrayList<>());
        builds.removeIf(b -> b.work.isComplete() || level.getGameTime() - b.lastActive > 1200);
        return builds.stream().filter(b -> Objects.equals(b.owner, owner) && b.type.equals(type)
            && b.anchor.distSqr(anchor) <= 256).findFirst().orElse(null);
    }
    public static Build create(ServerLevel level, UUID owner, String type, BlockPos anchor, List<BlockPlacement> plan) {
        Build build = new Build(type, owner, anchor, plan, level.getGameTime());
        WORLDS.computeIfAbsent(level, l -> new ArrayList<>()).add(build);
        return build;
    }
    public static void clear() { WORLDS.clear(); }
    private CollaborativeBuildManager() {}
}
