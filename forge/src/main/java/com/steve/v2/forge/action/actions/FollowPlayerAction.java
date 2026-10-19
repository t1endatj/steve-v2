package com.steve.v2.forge.action.actions;

import com.steve.v2.core.action.ActionResult;
import com.steve.v2.core.task.Task;
import com.steve.v2.forge.action.FollowController;
import com.steve.v2.forge.entity.SteveEntity;
import net.minecraft.world.entity.player.Player;
import java.util.UUID;

public final class FollowPlayerAction extends BaseAction {
    private Player target;
    private String playerName;
    private UUID playerId;
    private final FollowController movement;
    public FollowPlayerAction(SteveEntity steve, Task task) {
        super(steve, task); movement = new FollowController(steve);
    }
    @Override protected void onStart() {
        playerName = task.getStringParameter("player");
        String id = task.getStringParameter("playerId");
        if (id != null) playerId = UUID.fromString(id);
        target = findPlayer();
        if (target == null) { result = ActionResult.failure("Player not found in this dimension: " + playerName, false); return; }
        playerId = target.getUUID();
        movement.reset();
    }
    @Override protected void onTick() {
        if (target == null || target.isRemoved() || !target.isAlive()) target = findPlayer();
        if (target == null) { result = ActionResult.failure("Follow target left this dimension", false); return; }
        movement.tick(target);
    }
    private Player findPlayer() {
        if (playerId != null) return steve.level().getPlayerByUUID(playerId);
        Player owner = steve.getOwnerPlayer();
        if (owner != null && (owner.getName().getString().equalsIgnoreCase(playerName)
            || "me".equalsIgnoreCase(playerName) || "you".equalsIgnoreCase(playerName))) return owner;
        return steve.level().players().stream().filter(p -> p.isAlive() && !p.isRemoved()
            && p.getName().getString().equalsIgnoreCase(playerName)).findFirst().orElse(null);
    }
    @Override protected void onCancel() { steve.getNavigation().stop(); steve.setFlying(false); }
    @Override public String getDescription() { return "Follow " + playerName + ": " + movement.status(); }
}


