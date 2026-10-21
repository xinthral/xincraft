package com.xincraft.xincraft.entity;

import com.xincraft.xincraft.Config;
import com.xincraft.xincraft.block.BouncyRailBlock;
import com.xincraft.xincraft.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// Vanilla's default minecart physics, plus the mod's special rails:
// - tin rails raise the speed cap on straight track (slopes included), easing back to normal for
//   corners;
// - honey rails hold carts at a reduced speed;
// - powered bouncy rails launch carts into the air.
// It also runs the shrieker upgrade (see CartUpgrades).
// Forge's per-rail speed hook (BaseRailBlock#getRailMaxSpeed) isn't consulted by the default
// physics in this version, so the cap is changed here instead. Every kind of cart (furnace
// minecarts included) scales from vanilla's own cap, so their relative speeds hold.
public class XincraftMinecartBehavior extends OldMinecartBehavior {
    // Speed a honey rail adds each tick to a cart below its target speed (same as a powered rail)
    private static final double HONEY_RAIL_BOOST = 0.06;
    // Below this a cart counts as stopped, with no direction to push it in (same as a powered rail)
    private static final double STOPPED_SPEED = 0.01;
    // Longest single move a cart makes along the track: vanilla's normal speed cap, which its rail
    // following is built around (see moveMinecartOnRail)
    private static final double MAX_STEP = 0.4;
    // A flying minecart's gravity and the share of its speed it keeps each tick (vanilla's and
    // Forge's defaults), used to work out how hard to launch a cart to reach a given height
    private static final double GRAVITY = 0.04;
    private static final double AIR_DRAG = 0.95;

    // Horizontal speed the cart had when a bouncy rail launched it, so the air speed cap doesn't
    // cut it down mid-flight (e.g. off a tin rail); 0 when the cart isn't in a bounce
    private double launchSpeed = 0.0;

    public XincraftMinecartBehavior(AbstractMinecart minecart) {
        super(minecart);
    }

    // Swaps this in for vanilla's default physics on every minecart, leaving the experimental
    // physics and any other mod's replacement physics alone
    public static void install() {
        var previous = AbstractMinecart.getBehaviorFactory();
        AbstractMinecart.setBehaviorFactory((cart, level) -> {
            var behavior = previous.apply(cart, level);
            return behavior.getClass() == OldMinecartBehavior.class ? new XincraftMinecartBehavior(cart) : behavior;
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

    // Tin speed only applies on straight track. Even split into steps (see moveMinecartOnRail), a
    // fast cart moves in straight lines, and through a curve that can cut across into the diagonal
    // block, which has no rail, so the cart flies off. So carts move at the normal cap from one
    // block before a curve until they're past it. Their velocity isn't reduced, only how far they
    // move each tick, so they're back up to tin speed on the next straight.
    private boolean canGoTinSpeed(ServerLevel level) {
        BlockPos pos = minecart.getCurrentBlockPosOrRailBelow();
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModTags.TIN_RAILS) || isCurve(railShape(level, state, pos))) {
            return false;
        }
        BlockPos ahead = railAhead(level, pos);
        return ahead == null || !isCurve(railShape(level, level.getBlockState(ahead), ahead));
    }

    private @Nullable RailShape railShape(ServerLevel level, BlockState state, BlockPos pos) {
        return state.getBlock() instanceof BaseRailBlock rail ? rail.getRailDirection(state, level, pos, minecart) : null;
    }

    private static boolean isCurve(@Nullable RailShape shape) {
        return shape != null && shape != RailShape.NORTH_SOUTH && shape != RailShape.EAST_WEST && !shape.isSlope();
    }

    // Vanilla moves the cart in one straight step per tick, at the height of the rail it started the
    // tick on (for a slope, the top of the slope's block). Any longer than the normal cap and that
    // step can reach two blocks ahead on a climb, into the solid block holding up a later slope rail,
    // and the collision stops the cart dead. So longer moves are split into steps of at most
    // MAX_STEP, and whenever the cart crosses into the next block it's moved to the height vanilla
    // would have used had it started the tick on the rail there.
    @Override
    protected void moveMinecartOnRail(ServerLevel level) {
        if (getMaxSpeed(level) <= MAX_STEP) {
            super.moveMinecartOnRail(level);
            return;
        }
        // The same move vanilla makes. AbstractMinecart#getMaxSpeed, which some carts lower (the
        // furnace minecart halves it), is protected; Forge's air speed cap returns it unless a mod
        // has set its own.
        double cap = minecart.getMaxSpeedAirLateral();
        double scale = minecart.isVehicle() ? 0.75 : 1.0;
        Vec3 velocity = getDeltaMovement();
        Vec3 move = new Vec3(Mth.clamp(scale * velocity.x, -cap, cap), 0.0, Mth.clamp(scale * velocity.z, -cap, cap));
        int steps = Mth.ceil(move.horizontalDistance() / MAX_STEP);
        if (steps <= 1) {
            minecart.move(MoverType.SELF, move);
            return;
        }
        Vec3 step = move.scale(1.0 / steps);
        BlockPos start = minecart.getCurrentBlockPosOrRailBelow();
        BlockPos rail = start;
        for (int i = 0; i < steps; i++) {
            minecart.move(MoverType.SELF, step);
            if (minecart.horizontalCollision) {
                return;
            }
            int x = Mth.floor(minecart.getX());
            int z = Mth.floor(minecart.getZ());
            if (x == rail.getX() && z == rail.getZ()) {
                continue;
            }
            BlockPos next = findRail(level, x, Mth.floor(minecart.getY()), z);
            if (next == null) {
                // Off the end of the track: make the rest of the move in one go, as vanilla would
                minecart.move(MoverType.SELF, step.scale(steps - i - 1));
                return;
            }
            rail = next;
            RailShape shape = railShape(level, level.getBlockState(rail), rail);
            minecart.setPos(minecart.getX(), rail.getY() + (shape != null && shape.isSlope() ? 1 : 0), minecart.getZ());
        }
        if (!rail.equals(start)) {
            // Vanilla's adjustments after the move find the rail from one block above it
            minecart.setPos(minecart.getX(), rail.getY() + 1, minecart.getZ());
        }
    }

    // The rail in the given column at the cart's height or up to two blocks below (the next rail
    // down a descent is two below the height a cart on a slope moves at)
    private static @Nullable BlockPos findRail(ServerLevel level, int x, int y, int z) {
        for (int dy = 0; dy >= -2; dy--) {
            BlockPos pos = new BlockPos(x, y + dy, z);
            if (BaseRailBlock.isRail(level.getBlockState(pos))) {
                return pos;
            }
        }
        return null;
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

    // A cart with a shrieker upgrade clears the track ahead instead of picking up whatever it hits,
    // and is never slowed by anything but other minecarts, which it still bumps as vanilla does
    @Override
    public boolean pushAndPickupEntities() {
        if (!(minecart instanceof UpgradableCart cart) || !cart.upgrades().has(CartUpgrade.SHRIEKER)
                || !(level() instanceof ServerLevel level)) {
            return super.pushAndPickupEntities();
        }
        cart.upgrades().clearTrackAhead(level, minecart.getDeltaMovement());
        for (Entity entity : level.getEntities(minecart, minecart.getBoundingBox().inflate(0.2, 0.0, 0.2))) {
            if (entity instanceof AbstractMinecart && entity.isPushable() && !minecart.hasPassenger(entity)) {
                entity.push(minecart);
            }
        }
        return false;
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
    // height, keeping the forward speed it was moving at. A stopped cart isn't launched, or it
    // would bounce in place forever.
    private void launch(ServerLevel level, int power) {
        Vec3 movement = getDeltaMovement();
        if (movement.horizontalDistance() < STOPPED_SPEED) {
            return;
        }
        // A cart's velocity can build up well past the speed cap (to 2 blocks a tick on a long line
        // of powered rails) while the cap holds how far it actually moves. In the air nothing would
        // hold it back, so it's thrown forward at the speed it was really moving along the rail:
        // the same clamped move vanilla makes (see moveMinecartOnRail), with any earlier bounce's
        // speed cleared from the cap first.
        launchSpeed = 0.0;
        double cap = minecart.getMaxSpeedAirLateral();
        double scale = minecart.isVehicle() ? 0.75 : 1.0;
        double x = Mth.clamp(scale * movement.x, -cap, cap);
        double z = Mth.clamp(scale * movement.z, -cap, cap);
        setDeltaMovement(x, launchVelocity(Config.bouncyRailMaxHeight * power / 15.0), z);
        launchSpeed = Math.sqrt(x * x + z * z);
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
