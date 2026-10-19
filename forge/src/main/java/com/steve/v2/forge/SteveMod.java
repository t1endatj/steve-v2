package com.steve.v2.forge;

import com.mojang.logging.LogUtils;
import com.steve.v2.forge.command.SteveCommands;
import com.steve.v2.forge.config.SteveConfig;
import com.steve.v2.forge.entity.SteveEntity;
import net.minecraft.world.entity.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;
import org.slf4j.Logger;

@Mod(SteveMod.MODID)
public final class SteveMod {
    public static final String MODID = "steve_v2";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final RegistryObject<EntityType<SteveEntity>> STEVE_ENTITY = ENTITIES.register("steve", () ->
        EntityType.Builder.of(SteveEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).clientTrackingRange(10).build("steve"));

    public SteveMod(FMLJavaModLoadingContext context) {
        var bus = context.getModEventBus();
        ENTITIES.register(bus);
        bus.addListener(this::attributes);
        context.registerConfig(ModConfig.Type.COMMON, SteveConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(this);
    }
    private void attributes(EntityAttributeCreationEvent event) {
        event.put(STEVE_ENTITY.get(), SteveEntity.createAttributes().build());
    }
    @SubscribeEvent public void commands(RegisterCommandsEvent event) { SteveCommands.register(event.getDispatcher()); }
}
