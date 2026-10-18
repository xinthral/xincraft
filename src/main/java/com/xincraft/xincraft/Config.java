package com.xincraft.xincraft;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

// Tunable mod values. Forge writes these, with their comments and allowed ranges, to
// config/xincraft-common.toml on first launch, and reloads them when that file is edited
// while the game is running. Add new values to the section for the feature they belong to.
@Mod.EventBusSubscriber(modid = XincraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.DoubleValue PADDED_RAIL_VOLUME;
    private static final ForgeConfigSpec.DoubleValue TIN_RAIL_SPEED_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue LOCOMOTIVE_FUEL_MULTIPLIER;
    private static final ForgeConfigSpec.IntValue ENDER_RAIL_MAX_DISTANCE;
    private static final ForgeConfigSpec.DoubleValue ENDER_RAIL_COOLDOWN;
    private static final ForgeConfigSpec.IntValue ENDER_RAIL_MAX_PER_SECOND;
    private static final ForgeConfigSpec.BooleanValue ENDER_RAIL_ALLOW_ANY_MINECART;

    static {
        BUILDER.comment("Padded rails and padded powered rails").push("paddedRails");
        PADDED_RAIL_VOLUME = BUILDER
                .comment("Volume multiplier applied to minecart sounds while the cart is on a padded rail (0 = silent, 1 = vanilla)")
                .defineInRange("volume", 0.2, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.comment("Tin rails and tin powered rails").push("tinRails");
        TIN_RAIL_SPEED_MULTIPLIER = BUILDER
                .comment("How many times faster minecarts can travel on tin rails than on other rails (1 = same as vanilla)")
                .defineInRange("speedMultiplier", 2.0, 1.0, 3.0);
        BUILDER.pop();

        BUILDER.comment("Locomotive cart").push("locomotiveCart");
        LOCOMOTIVE_FUEL_MULTIPLIER = BUILDER
                .comment("How many times longer fuel burns in a locomotive cart than in a furnace")
                .defineInRange("fuelMultiplier", 8.0, 1.0, 64.0);
        BUILDER.pop();

        BUILDER.comment("Ender rails").push("enderRails");
        ENDER_RAIL_MAX_DISTANCE = BUILDER
                .comment("Maximum distance in blocks between two linked ender rails")
                .defineInRange("maxDistance", 64, 1, 30_000_000);
        ENDER_RAIL_COOLDOWN = BUILDER
                .comment("Seconds a minecart must wait after teleporting before an ender rail will teleport it again")
                .defineInRange("cooldownSeconds", 3.0, 0.0, 3600.0);
        ENDER_RAIL_MAX_PER_SECOND = BUILDER
                .comment("Maximum ender rail teleports across the whole server in any one second (0 = no limit)")
                .defineInRange("maxTeleportsPerSecond", 20, 0, 1000);
        ENDER_RAIL_ALLOW_ANY_MINECART = BUILDER
                .comment("Whether minecarts other than the ender minecart (vanilla and other mods' carts) can teleport on ender rails")
                .define("allowOtherMinecarts", false);
        BUILDER.pop();
    }

    static final ForgeConfigSpec SPEC = BUILDER.build();

    // Cached copies of the values above, refreshed whenever the config loads or reloads.
    // Initialized to the same defaults so they're usable before the config has loaded.
    public static float paddedRailVolume = 0.2F;
    public static double tinRailSpeedMultiplier = 2.0;
    public static double locomotiveFuelMultiplier = 8.0;
    public static int enderRailMaxDistance = 64;
    public static int enderRailCooldownTicks = 60;
    public static int enderRailMaxTeleportsPerSecond = 20;
    public static boolean enderRailAllowAnyMinecart = false;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent.Loading event) {
        refresh();
    }

    // Fired when the file is edited while the game is running
    @SubscribeEvent
    static void onReload(final ModConfigEvent.Reloading event) {
        refresh();
    }

    private static void refresh() {
        paddedRailVolume = PADDED_RAIL_VOLUME.get().floatValue();
        tinRailSpeedMultiplier = TIN_RAIL_SPEED_MULTIPLIER.get();
        locomotiveFuelMultiplier = LOCOMOTIVE_FUEL_MULTIPLIER.get();
        enderRailMaxDistance = ENDER_RAIL_MAX_DISTANCE.get();
        enderRailCooldownTicks = (int) Math.round(ENDER_RAIL_COOLDOWN.get() * 20);
        enderRailMaxTeleportsPerSecond = ENDER_RAIL_MAX_PER_SECOND.get();
        enderRailAllowAnyMinecart = ENDER_RAIL_ALLOW_ANY_MINECART.get();
    }
}
