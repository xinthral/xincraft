package com.xincraft.xincraft;

import com.mojang.logging.LogUtils;
import com.xincraft.xincraft.entity.TinRailMinecartBehavior;
import com.xincraft.xincraft.registry.ModBlockEntities;
import com.xincraft.xincraft.registry.ModBlocks;
import com.xincraft.xincraft.registry.ModCreativeModeTabs;
import com.xincraft.xincraft.registry.ModDataComponents;
import com.xincraft.xincraft.registry.ModEntityTypes;
import com.xincraft.xincraft.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(XincraftMod.MODID)
public final class XincraftMod {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "xincraft";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public XincraftMod(FMLJavaModLoadingContext context) {
        var modBusGroup = context.getModBusGroup();

        // Register the commonSetup method for modloading
        FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::commonSetup);

        // Register each content module's Deferred Register to the mod event bus
        ModBlocks.BLOCKS.register(modBusGroup);
        ModItems.ITEMS.register(modBusGroup);
        ModBlockEntities.BLOCK_ENTITIES.register(modBusGroup);
        ModEntityTypes.ENTITY_TYPES.register(modBusGroup);
        ModDataComponents.COMPONENTS.register(modBusGroup);
        ModCreativeModeTabs.TABS.register(modBusGroup);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        // Common setup runs in parallel with other mods; swap the minecart physics on the main thread
        event.enqueueWork(TinRailMinecartBehavior::install);
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
