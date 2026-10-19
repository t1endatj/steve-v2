package com.steve.v2.forge.client;

import com.steve.v2.forge.SteveMod;
import com.steve.v2.forge.entity.SteveEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SteveMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SteveMod.STEVE_ENTITY.get(), context ->
            new HumanoidMobRenderer<SteveEntity, PlayerModel<SteveEntity>>(context,
                new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F) {
                @Override public ResourceLocation getTextureLocation(SteveEntity entity) {
                    return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");
                }
            });
    }
}
