package com.xincraft.xincraft.entity;

import com.xincraft.xincraft.Config;
import com.xincraft.xincraft.block.BouncyRailBlock;
import com.xincraft.xincraft.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// Vanilla's default minecart physics, plus the mod's special rails:
// - tin rails raise the speed cap on straight track, easing back to normal for corners;
// - honey rails hold carts at a reduced speed;
// - powered bouncy rails launch carts into the air.
// Forge's per-rail speed hook (BaseRailBlock#getRailMaxSpeed) isn't consulted by the default
// physics in this version, so the cap is changed here instead. Every kind of cart (furnace
// minecarts included) scales from vanilla's own cap, so their relative speeds hold. Despite the
// name, which predates the honey and bouncy rails, this is the mod's one minecart physics class.
public class TinRailMinecartBehavior extends OldMinecartBehavior {
    // Speed a honey rail adds each tick to a cart below its target speed (same as a powered rail)
    private static final double HONEY_RAIL_BOOST = 0.06;
    // Below this a cart counts as stopped, with no direction to push it in (same as a powered rail)
    private static final double STOPPED_SPEED = 0.01;
    // A flying minecart's gravity and the share of its speed it keeps each tick (vanilla's and
    // Forge's defaults), used to work out how hard to launch a cart to reach a given height
    private static final double GRAVITY = 0.04;
    private static final double AIR_DRAG = 0.95;

    // Horizontal speed the cart had when a bouncy rail launched it, so the air speed cap doesn't
    // cut it down mid-flight (e.g. off a tin rail); 0 when the cart isn't in a bounce
    private double launchSpeed = 0.0;

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
        if (canGoTinSpeed(level)) {
            max *= Config.tinRailSpeedMultiplier;
        }
        return Math.max(max, launchSpeed);
    }

    // Tin speed only applies on straight track. A tick's movement is one straight step that's then
    // snapped back onto the rail, and at tin speed a step through a curve can cut across into the
    // diagonal block, which has no rail, so the cart flies off. Carts move at the normal cap from
    // one block before a curve until they're past it. Their velocity isn't reduced, only how far
    // they move each tick, so they're back up to tin speed on the next straight.
    private boolean canGoTinSpeed(ServerLevel level) {
        BlockPos pos = minecart.getCurrentBlockPosOrRailBelow();
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModTags.TIN_RAILS) || isCurve(level, state, pos)) {
            return false;
        }
        BlockPos ahead = railAhead(level, pos);
        return ahead == null || !isCurve(level, level.getBlockState(ahead), ahead);
    }

    private boolean isCurve(ServerLevel level, BlockState state, BlockPos pos) {
        if (!(state.getBlock() instanceof BaseRailBlock rail)) {
            return false;
        }
        RailShape shape = rail.getRailDirection(state, level, pos, minecart);
        return shape != RailShape.NORTH_SOUTH && shape != RailShape.EAST_WEST && !shape.isSlope();
    }

    // The next block in the direction of travel, or the block below it when that's where the rail
    // is (as AbstractMinecart#getCurrentBlockPosOrRailBelow does); null if the cart isn't moving
    private @Nullable BlockPos railAhead(ServerLevel level, BlockPos pos) {
        Vec3 movement = getDeltaMovement();
        if (movement.horizontalDistance() < STOPPED_SPEED) {
            return null;
        }
        BlockPos ahead = Math.abs(movement.x) >= Math.abs(movement.z)
            ? pos.east((int) Math.signum(movement.x))
            : pos.south((int) Math.signum(movement.z));
        return level.getBlockState(ahead.below()).is(BlockTags.RAILS) ? ahead.below() : ahead;
    }

    @Override
    public void tick() {
        super.tick();
        if (launchSpeed > 0.0) {
            // Rails are ignored on the way up so the one the cart left doesn't snap it straight back
            // down; from the top of the arc (or on hitting a ceiling) they can catch it again
            if (!minecart.canUseRail() && getDeltaMovement().y <= 0.0) {
                minecart.setCanUseRail(true);
            }
            if (minecart.canUseRail() && (minecart.isOnRails() || minecart.onGround())) {
                launchSpeed = 0.0;
            }
        }
    }

    @Override
    public void moveAlongTrack(ServerLevel level) {
        // Checked before moving, since the cart may roll onto the next rail during the tick
        BlockState rail = level.getBlockState(minecart.getCurrentBlockPosOrRailBelow());
        super.moveAlongTrack(level);
        if (!shouldDoRailFunctions()) {
            return;
        }
        if (rail.is(ModTags.HONEY_RAILS)) {
            applyHoneyRail(level);
        } else if (rail.is(ModTags.BOUNCY_RAILS)) {
            int power = rail.getValueOrElse(BouncyRailBlock.POWER, 0);
            if (power > 0) {
                launch(level, power);
            }
        }
    }

    // Brings the cart to the honey rail's speed: a faster cart is cut straight down to it and a
    // slower one is pushed up toward it. A stopped cart is left where it is.
    private void applyHoneyRail(ServerLevel level) {
        double target = super.getMaxSpeed(level) * Config.honeyRailSpeedMultiplier;
        Vec3 movement = getDeltaMovement();
        double speed = movement.horizontalDistance();
        if (speed < STOPPED_SPEED) {
            return;
        }
        double newSpeed = speed > target ? target : Math.min(target, speed + HONEY_RAIL_BOOST);
        setDeltaMovement(movement.multiply(newSpeed / speed, 1.0, newSpeed / speed));
    }

    // Throws the cart upward hard enough to rise about (power / 15) of the configured maximum
    // height, keeping its forward velocity. A stopped cart isn't launched, or it would bounce in
    // place forever.
    private void launch(ServerLevel level, int power) {
        Vec3 movement = getDeltaMovement();
        double speed = movement.horizontalDistance();
        if (speed < STOPPED_SPEED) {
            return;
        }
        setDeltaMovement(movement.x, launchVelocity(Config.bouncyRailMaxHeight * power / 15.0), movement.z);
        launchSpeed = speed;
        minecart.setCanUseRail(false);
        // Otherwise its first tick in the air would treat it as rolling along the ground and halve its speed
        minecart.setOnGround(false);
        level.playSound(null, minecart.getX(), minecart.getY(), minecart.getZ(),
            SoundEvents.SLIME_JUMP, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    // The upward speed that carries a flying cart to the given height, found by bisection over
    // the same gravity-then-drag steps the cart takes each tick in the air
    private static double launchVelocity(double height) {
        double low = 0.0;
        double high = 4.0;
        for (int i = 0; i < 30; i++) {
            double mid = (low + high) / 2.0;
            if (riseHeight(mid) < height) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return high;
    }

    private static double riseHeight(double velocity) {
        double height = 0.0;
        for (double y = velocity - GRAVITY; y > 0.0; y = y * AIR_DRAG - GRAVITY) {
            height += y;
        }
        return height;
    }
}
