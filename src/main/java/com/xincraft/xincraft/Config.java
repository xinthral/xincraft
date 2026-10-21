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

    private static final ForgeConfigSpec.BooleanValue GENERATE_TIN_ORE;
    private static final ForgeConfigSpec.DoubleValue PADDED_RAIL_VOLUME;
    private static final ForgeConfigSpec.DoubleValue TIN_RAIL_SPEED_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue HONEY_RAIL_SPEED_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue BOUNCY_RAIL_MAX_HEIGHT;
    private static final ForgeConfigSpec.DoubleValue LOCOMOTIVE_FUEL_MULTIPLIER;
    private static final ForgeConfigSpec.IntValue LOCOMOTIVE_ENERGY_CAPACITY;
    private static final ForgeConfigSpec.IntValue LOCOMOTIVE_ENERGY_PER_TICK;
    private static final ForgeConfigSpec.IntValue HEAT_TURBINE_ENERGY_PER_TICK;
    private static final ForgeConfigSpec.IntValue STEAM_TURBINE_ENERGY_PER_TICK;
    private static final ForgeConfigSpec.IntValue STEAM_TURBINE_WATER_PER_TICK;
    private static final ForgeConfigSpec.DoubleValue MOTORBOAT_FUEL_MULTIPLIER;
    private static final ForgeConfigSpec.IntValue MOTORBOAT_ENERGY_CAPACITY;
    private static final ForgeConfigSpec.IntValue MOTORBOAT_ENERGY_PER_TICK;
    private static final ForgeConfigSpec.IntValue MENDING_LILAC_RANGE;
    private static final ForgeConfigSpec.IntValue MOONLIT_DANDELION_RANGE;
    private static final ForgeConfigSpec.IntValue SHIMMER_LILY_RANGE;
    private static final ForgeConfigSpec.IntValue ENDER_RAIL_MAX_DISTANCE;
    private static final ForgeConfigSpec.DoubleValue ENDER_RAIL_COOLDOWN;
    private static final ForgeConfigSpec.IntValue ENDER_RAIL_MAX_PER_SECOND;
    private static final ForgeConfigSpec.BooleanValue ENDER_RAIL_ALLOW_ANY_MINECART;

    static {
        BUILDER.comment("Tin").push("tin");
        GENERATE_TIN_ORE = BUILDER
                .comment("Whether tin ore generates in the Overworld. Turn off if another mod already generates tin.",
                        "Takes effect the next time a world is loaded, and only in chunks generated after that.")
                .define("generateOre", true);
        BUILDER.pop();

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

        BUILDER.comment("Honey rails").push("honeyRails");
        HONEY_RAIL_SPEED_MULTIPLIER = BUILDER
                .comment("Speed minecarts are held at on honey rails, as a fraction of their normal top speed (1 = full speed)")
                .defineInRange("speedMultiplier", 0.5, 0.05, 1.0);
        BUILDER.pop();

        BUILDER.comment("Bouncy rails").push("bouncyRails");
        BOUNCY_RAIL_MAX_HEIGHT = BUILDER
                .comment("Roughly how many blocks high a bouncy rail launches a minecart at full redstone power (15); weaker signals launch proportionally lower")
                .defineInRange("maxLaunchHeight", 8.0, 1.0, 64.0);
        BUILDER.pop();

        BUILDER.comment("Locomotive cart").push("locomotiveCart");
        LOCOMOTIVE_FUEL_MULTIPLIER = BUILDER
                .comment("How many times longer fuel burns in a locomotive cart than in a furnace")
                .defineInRange("fuelMultiplier", 8.0, 1.0, 64.0);
        LOCOMOTIVE_ENERGY_CAPACITY = BUILDER
                .comment("How much Forge Energy (FE) a locomotive cart with the energy upgrade can store")
                .defineInRange("energyCapacity", 100_000, 1_000, 100_000_000);
        LOCOMOTIVE_ENERGY_PER_TICK = BUILDER
                .comment("How much Forge Energy (FE) a locomotive cart with the energy upgrade uses each tick while driving")
                .defineInRange("energyPerTick", 20, 1, 100_000);
        BUILDER.pop();

        BUILDER.comment("Heat and steam turbines").push("turbines");
        HEAT_TURBINE_ENERGY_PER_TICK = BUILDER
                .comment("Forge Energy (FE) a heat turbine makes each tick while burning fuel (fuel burns at furnace speed)")
                .defineInRange("heatTurbineEnergyPerTick", 40, 1, 100_000);
        STEAM_TURBINE_ENERGY_PER_TICK = BUILDER
                .comment("Forge Energy (FE) a steam turbine makes each tick while burning fuel (fuel burns at twice furnace speed)")
                .defineInRange("steamTurbineEnergyPerTick", 160, 1, 100_000);
        STEAM_TURBINE_WATER_PER_TICK = BUILDER
                .comment("Water, in millibuckets (1000 = one bucket), a steam turbine uses each tick while running")
                .defineInRange("steamTurbineWaterPerTick", 5, 0, 1_000);
        BUILDER.pop();

        BUILDER.comment("Motorboats").push("motorboat");
        MOTORBOAT_FUEL_MULTIPLIER = BUILDER
                .comment("How many times longer fuel burns in a motorboat than in a furnace")
                .defineInRange("fuelMultiplier", 8.0, 1.0, 64.0);
        MOTORBOAT_ENERGY_CAPACITY = BUILDER
                .comment("How much Forge Energy (FE) a motorboat with the energy upgrade can store")
                .defineInRange("energyCapacity", 100_000, 1_000, 100_000_000);
        MOTORBOAT_ENERGY_PER_TICK = BUILDER
                .comment("How much Forge Energy (FE) a motorboat with the energy upgrade uses each tick while cruising (twice this with the speed upgrade)")
                .defineInRange("energyPerTick", 20, 1, 100_000);
        BUILDER.pop();

        BUILDER.comment("Exotic flowers").push("flowers");
        MENDING_LILAC_RANGE = BUILDER
                .comment("How far, in blocks, a mending lilac's regeneration reaches in each direction (8 = a 16-block-wide area)")
                .defineInRange("mendingLilacRange", 8, 1, 32);
        MOONLIT_DANDELION_RANGE = BUILDER
                .comment("How far, in blocks, a moonlit dandelion's night vision reaches in each direction (8 = a 16-block-wide area)")
                .defineInRange("moonlitDandelionRange", 8, 1, 32);
        SHIMMER_LILY_RANGE = BUILDER
                .comment("How far, in blocks, a shimmer lily's invisibility reaches in each direction (8 = a 16-block-wide area)")
                .defineInRange("shimmerLilyRange", 8, 1, 32);
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
    public static boolean generateTinOre = true;
    public static float paddedRailVolume = 0.2F;
    public static double tinRailSpeedMultiplier = 2.0;
    public static double honeyRailSpeedMultiplier = 0.5;
    public static double bouncyRailMaxHeight = 8.0;
    public static double locomotiveFuelMultiplier = 8.0;
    public static int locomotiveEnergyCapacity = 100_000;
    public static int locomotiveEnergyPerTick = 20;
    public static int heatTurbineEnergyPerTick = 40;
    public static int steamTurbineEnergyPerTick = 160;
    public static int steamTurbineWaterPerTick = 5;
    public static double motorboatFuelMultiplier = 8.0;
    public static int motorboatEnergyCapacity = 100_000;
    public static int motorboatEnergyPerTick = 20;
    public static int mendingLilacRange = 8;
    public static int moonlitDandelionRange = 8;
    public static int shimmerLilyRange = 8;
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
        generateTinOre = GENERATE_TIN_ORE.get();
        paddedRailVolume = PADDED_RAIL_VOLUME.get().floatValue();
        tinRailSpeedMultiplier = TIN_RAIL_SPEED_MULTIPLIER.get();
        honeyRailSpeedMultiplier = HONEY_RAIL_SPEED_MULTIPLIER.get();
        bouncyRailMaxHeight = BOUNCY_RAIL_MAX_HEIGHT.get();
        locomotiveFuelMultiplier = LOCOMOTIVE_FUEL_MULTIPLIER.get();
        locomotiveEnergyCapacity = LOCOMOTIVE_ENERGY_CAPACITY.get();
        locomotiveEnergyPerTick = LOCOMOTIVE_ENERGY_PER_TICK.get();
        heatTurbineEnergyPerTick = HEAT_TURBINE_ENERGY_PER_TICK.get();
        steamTurbineEnergyPerTick = STEAM_TURBINE_ENERGY_PER_TICK.get();
        steamTurbineWaterPerTick = STEAM_TURBINE_WATER_PER_TICK.get();
        motorboatFuelMultiplier = MOTORBOAT_FUEL_MULTIPLIER.get();
        motorboatEnergyCapacity = MOTORBOAT_ENERGY_CAPACITY.get();
        motorboatEnergyPerTick = MOTORBOAT_ENERGY_PER_TICK.get();
        mendingLilacRange = MENDING_LILAC_RANGE.get();
        moonlitDandelionRange = MOONLIT_DANDELION_RANGE.get();
        shimmerLilyRange = SHIMMER_LILY_RANGE.get();
        enderRailMaxDistance = ENDER_RAIL_MAX_DISTANCE.get();
        enderRailCooldownTicks = (int) Math.round(ENDER_RAIL_COOLDOWN.get() * 20);
        enderRailMaxTeleportsPerSecond = ENDER_RAIL_MAX_PER_SECOND.get();
        enderRailAllowAnyMinecart = ENDER_RAIL_ALLOW_ANY_MINECART.get();
    }
}
