package com.steve.v2.forge.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.steve.v2.forge.SteveMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = SteveMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class KeyBindings {
    public static final KeyMapping OPEN = new KeyMapping("key.steve_v2.open", InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_K, "key.categories.steve_v2");
    @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) { event.register(OPEN); }

    @Mod.EventBusSubscriber(modid = SteveMod.MODID, value = Dist.CLIENT)
    public static final class InputEvents {
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent.Post event) {
            var mc = Minecraft.getInstance();
            while (OPEN.consumeClick()) if (mc.player != null && mc.screen == null) mc.setScreen(new SteveScreen());
        }
    }
}
