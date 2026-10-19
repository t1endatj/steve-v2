package com.steve.v2.forge.event;

import com.steve.v2.forge.SteveMod;
import com.steve.v2.forge.action.CollaborativeBuildManager;
import com.steve.v2.forge.entity.SteveEntity;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SteveMod.MODID)
public final class ServerEvents {
    @SubscribeEvent public static void stop(ServerStoppingEvent event) {
        for (var level : event.getServer().getAllLevels()) {
            for (var entity : level.getAllEntities()) if (entity instanceof SteveEntity steve) steve.getActionExecutor().stopCurrentAction();
        }
        CollaborativeBuildManager.clear();
    }
}
