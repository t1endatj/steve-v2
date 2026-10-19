package com.steve.v2.forge.entity;

import com.steve.v2.core.memory.AgentMemory;
import com.steve.v2.forge.action.ActionExecutor;
import com.steve.v2.forge.config.SteveConfig;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import java.util.UUID;

public class SteveEntity extends PathfinderMob {
    private final ActionExecutor executor;
    private final AgentMemory memory = new AgentMemory();
    private UUID owner;
    private boolean flying;
    public SteveEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        executor = new ActionExecutor(this);
        setCustomNameVisible(true);
        setPersistenceRequired();
        if (getNavigation() instanceof GroundPathNavigation ground) {
            ground.setCanOpenDoors(true);
            ground.setCanPassDoors(true);
            ground.setCanFloat(true);
        }
    }
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, 4).add(Attributes.FOLLOW_RANGE, 48);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }
    @Override public void tick() {
        super.tick();
        if (!level().isClientSide) {
            setInvulnerable(SteveConfig.ASSISTANT_MODE.get());
            executor.tick();
        }
    }
    public String getSteveName() { return getCustomName() == null ? "Steve" : getCustomName().getString(); }
    public void setSteveName(String name) { setCustomName(Component.literal(name)); }
    public ActionExecutor getActionExecutor() { return executor; }
    public AgentMemory getMemory() { return memory; }
    public UUID getOwnerId() { return owner; }
    public void setOwner(UUID owner) { this.owner = owner; }
    public Player getOwnerPlayer() { return owner == null ? null : level().getPlayerByUUID(owner); }
    public void sendChatMessage(String message) {
        if (getOwnerPlayer() != null) getOwnerPlayer().sendSystemMessage(Component.literal("<" + getSteveName() + "> " + message));
    }
    public void setFlying(boolean enabled) {
        flying = enabled && SteveConfig.ASSISTANT_MODE.get();
        setNoGravity(flying);
    }
    public boolean isFlying() { return flying; }
    public void setInvulnerableBuilding(boolean enabled) { setInvulnerable(SteveConfig.ASSISTANT_MODE.get()); }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) tag.putUUID("SteveOwner", owner);
        ListTag history = new ListTag();
        memory.history().forEach(s -> history.add(StringTag.valueOf(s)));
        tag.put("SteveHistory", history);
        // Active work is intentionally not resumed implicitly after a world reload.
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("SteveOwner")) owner = tag.getUUID("SteveOwner");
        ListTag history = tag.getList("SteveHistory", Tag.TAG_STRING);
        for (int i = Math.max(0, history.size() - 20); i < history.size(); i++) memory.record(history.getString(i));
        setFlying(false);
    }
    @Override public void remove(RemovalReason reason) {
        if (!level().isClientSide) executor.stopCurrentAction();
        super.remove(reason);
    }
}
