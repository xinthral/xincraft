package com.xincraft.xincraft.registry;

import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.item.TinSpyglass;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, XincraftMod.MODID);

    public static final RegistryObject<CreativeModeTab> XINCRAFT_TAB = TABS.register("main", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.xincraft.main"))
        .withTabsBefore(CreativeModeTabs.COMBAT)
        .icon(() -> ModItems.TIN_INGOT.get().getDefaultInstance())
        // Every item the mod registers (block items included), in registration order, so new
        // items show up here without having to be added by hand, plus the tin spyglass, which is
        // a vanilla spyglass with a component rather than an item of its own
        .displayItems((_, output) -> {
            ModItems.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
            output.accept(TinSpyglass.create());
        })
        .build());

    private ModCreativeModeTabs() {}
}
