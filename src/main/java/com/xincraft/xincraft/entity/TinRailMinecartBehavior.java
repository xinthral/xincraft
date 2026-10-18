package com.xincraft.xincraft.entity;

import com.xincraft.xincraft.Config;
import com.xincraft.xincraft.registry.ModTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior;

// Vanilla's default minecart physics, except that the speed cap is raised while the cart is on a
// tin rail. Forge's per-rail speed hook (BaseRailBlock#getRailMaxSpeed) isn't consulted by the
// default physics in this version, so we raise the cap here instead. Every other kind of cart
// (furnace minecarts included) scales from vanilla's own cap, so their relative speeds hold.
public class TinRailMinecartBehavior extends OldMinecartBehavior {
    public TinRailMinecartBehavior(AbstractMinecart minecart) {
        super(minecart);
    }

    // Swaps this in for vanilla's default physics on every minecart, leaving the experimental
    // physics and any other mod's replacement physics alone
    public static void install() {
        var previous = AbstractMinecart.getBehaviorFactory();
        AbstractMinecart.setBehaviorFactory((cart, level) -> {
            var behavior = previous.apply(cart, level);
            return behavior.getClass() == OldMinecartBehavior.class ? new TinRailMinecartBehavior(cart) : behavior;
        });
    }

    @Override
    public double getMaxSpeed(ServerLevel level) {
        double max = super.getMaxSpeed(level);
        return level.getBlockState(minecart.getCurrentBlockPosOrRailBelow()).is(ModTags.TIN_RAILS)
            ? max * Config.tinRailSpeedMultiplier
            : max;
    }
}
