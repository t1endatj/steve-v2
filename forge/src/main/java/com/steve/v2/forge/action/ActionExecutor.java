package com.steve.v2.forge.action;

import com.steve.v2.core.agent.AgentRuntime;
import com.steve.v2.core.llm.HttpPlanner;
import com.steve.v2.core.perception.WorldSnapshot;
import com.steve.v2.core.task.ActionPlan;
import com.steve.v2.forge.action.actions.*;
import com.steve.v2.forge.config.SteveConfig;
import com.steve.v2.forge.entity.SteveEntity;
import com.steve.v2.forge.memory.WorldKnowledge;

/** Minecraft adapter for the independent core. Called only on the server thread. */
public final class ActionExecutor {
    private static final HttpPlanner PLANNER = new HttpPlanner(SteveConfig::llm);
    private final SteveEntity steve;
    private final AgentRuntime runtime;
    private final FollowController idleFollow;
    public ActionExecutor(SteveEntity steve) {
        this.steve = steve;
        idleFollow = new FollowController(steve);
        runtime = new AgentRuntime(PLANNER, task -> switch (task.action()) {
            case "pathfind" -> new PathfindAction(steve, task);
            case "mine" -> new MineBlockAction(steve, task);
            case "place" -> new PlaceBlockAction(steve, task);
            case "follow" -> new FollowPlayerAction(steve, task);
            case "attack" -> new CombatAction(steve, task);
            case "build" -> new BuildStructureAction(steve, task);
            default -> throw new IllegalArgumentException("Unsupported action: " + task.action());
        }, message -> { steve.getMemory().record(message); steve.sendChatMessage(message); });
    }
    public void processNaturalLanguageCommand(String command) {
        steve.getNavigation().stop();
        WorldKnowledge world = new WorldKnowledge(steve);
        var owner = steve.getOwnerPlayer();
        String situation = "Position: " + steve.blockPosition().toShortString()
            + "\nOwner: " + (owner == null ? "offline" : owner.getName().getString())
            + "\nNearby players: " + world.getNearbyPlayerNames()
            + "\nNearby entities: " + world.getNearbyEntitiesSummary()
            + "\nNearby blocks: " + world.getNearbyBlocksSummary()
            + "\nBiome: " + world.getBiomeName()
            + "\nRecent activity: " + steve.getMemory().history();
        runtime.command(new WorldSnapshot(steve.getSteveName(), steve.level().dimension().location().toString(), situation), command);
    }
    public void submit(ActionPlan plan) { idleFollow.reset(); runtime.submit(plan); }
    public void tick() {
        runtime.tick();
        if (runtime.state() == AgentRuntime.State.IDLE && SteveConfig.IDLE_FOLLOW.get()) {
            var owner = steve.getOwnerPlayer();
            if (owner != null && owner.isAlive() && !owner.isSpectator()) {
                idleFollow.tick(owner);
            }
        }
    }
    public void stopCurrentAction() { runtime.stop(); idleFollow.reset(); }
    public void pause() { runtime.pause(); steve.getNavigation().stop(); }
    public void resume() { runtime.resume(); }
    public AgentRuntime runtime() { return runtime; }
}
