package com.xincraft.xincraft.transport.train;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.xincraft.xincraft.common.MobFilterItem;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * @brief The upgrades fitted to one vehicle, and what they do.
 *
 * Upgrades are fitted by using the upgrade item on the vehicle and snipped off one at a time with
 * shears, most recently fitted first. Each can be fitted once, and the vehicle drops them when
 * broken. The installed set is synced to clients so they can show the upgrades.
 *
 * A carriage or ferry also takes a mob filter, kept apart from the upgrades since it carries the
 * mobs inscribed on it. Shears snip the filter off before any upgrade.
 *
 * @see common.MobFilterItem
 * @see MobCell
 */
public final class CartUpgrades {
    /** @brief How far ahead of the cart a shrieker clears the track. */
    private static final double SHRIEKER_REACH = 3.0;
    /** @brief How much further a shrieker reaches, the faster the cart goes. */
    private static final double SHRIEKER_REACH_PER_SPEED = 4.0;
    /**
     * @brief Sideways shove given to anything in the way: enough, with HOP, to land it clear of the
     *        cart.
     */
    private static final double SHOVE = 0.5;
    /** @brief Hop given to anything in the way. */
    private static final double HOP = 0.3;
    private static final int SHRIEK_COOLDOWN_TICKS = 40;
    /** @brief How often the shimmer upgrade refreshes its riders' invisibility. */
    private static final int SHIMMER_INTERVAL_TICKS = 20;
    /** @brief How long each dose lasts, outlasting the gap. */
    private static final int SHIMMER_EFFECT_TICKS = 60;
    /**
     * @brief One tick in this many gives riders away with a pink glint: on average, every few
     *        seconds.
     */
    private static final int GLINT_CHANCE = 80;
    private static final DustParticleOptions GLINT = new DustParticleOptions(0xFF7FC8, 1.0F);
    /** @brief Below this horizontal speed the cart has no direction to clear in. */
    private static final double MOVING_SPEED = 0.05;

    private final VehicleEntity cart;
    private final EntityDataAccessor<Integer> data;
    /**
     * @brief Server side: the fitted upgrades in the order they went on, so shears take off the
     *        newest.
     */
    private final List<CartUpgrade> fitOrder = new ArrayList<>();
    private int shriekCooldown;
    private ItemStack filter = ItemStack.EMPTY;

    public CartUpgrades(VehicleEntity cart, EntityDataAccessor<Integer> data) {
        this.cart = cart;
        this.data = data;
    }

    public boolean has(CartUpgrade upgrade) {
        return (bits() & upgrade.bit()) != 0;
    }

    public List<CartUpgrade> installed() {
        return Arrays.stream(CartUpgrade.values()).filter(this::has).toList();
    }

    /**
     * @brief The fitted mob filter, if any.
     *
     * @return The filter, or null if there's none.
     */
    public @Nullable ItemStack filter() {
        return filter.isEmpty() ? null : filter;
    }

    private int bits() {
        return cart.getEntityData().get(data);
    }

    private void set(int bits) {
        cart.getEntityData().set(data, bits);
    }

    /**
     * @brief Fits an upgrade item, or snips the most recently fitted upgrade off with shears,
     *        handing it back.
     *
     * @param owner The vehicle these upgrades are on.
     * @param player The player.
     * @param hand The hand being used.
     * @return PASS for anything else, so the vehicle can carry on with its own interaction.
     */
    public InteractionResult interact(UpgradableCart owner, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean server = !cart.level().isClientSide();
        if (stack.getItem() instanceof CartUpgradeItem item) {
            CartUpgrade upgrade = item.upgrade();
            if (!owner.accepts(upgrade) || has(upgrade)) {
                if (server) {
                    player.sendOverlayMessage(Component.translatable(has(upgrade)
                        ? "message.xincraft.upgrade_already_fitted" : "message.xincraft.upgrade_not_accepted"));
                }
                return InteractionResult.FAIL;
            }
            CartUpgrade required = upgrade.requires();
            if (required != null && !has(required)) {
                if (server) {
                    player.sendOverlayMessage(Component.translatable("message.xincraft.upgrade_needs",
                        new ItemStack(required.item()).getHoverName()));
                }
                return InteractionResult.FAIL;
            }
            if (server) {
                set(bits() | upgrade.bit());
                fitOrder.add(upgrade);
                stack.consume(1, player);
                cart.playSound(SoundEvents.SMITHING_TABLE_USE, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.getItem() instanceof MobFilterItem && owner.acceptsFilter()) {
            if (server) {
                ItemStack old = filter;
                filter = stack.split(1);
                if (!old.isEmpty()) {
                    giveBack(player, old);
                }
                cart.playSound(SoundEvents.SMITHING_TABLE_USE, 1.0F, 1.2F);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.SHEARS) && !filter.isEmpty()) {
            if (server) {
                ItemStack removed = filter;
                filter = ItemStack.EMPTY;
                player.sendOverlayMessage(Component.translatable("message.xincraft.upgrade_removed", removed.getHoverName()));
                giveBack(player, removed);
                stack.hurtAndBreak(1, player, hand);
                cart.playSound(SoundEvents.SHEEP_SHEAR, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.SHEARS) && bits() != 0) {
            if (server) {
                CartUpgrade last = newest();
                set(bits() & ~last.bit());
                fitOrder.remove(last);
                ItemStack removed = new ItemStack(last.item());
                player.sendOverlayMessage(Component.translatable("message.xincraft.upgrade_removed", removed.getHoverName()));
                giveBack(player, removed);
                stack.hurtAndBreak(1, player, hand);
                cart.playSound(SoundEvents.SHEEP_SHEAR, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /**
     * @brief Into the player's inventory, or dropped at the cart if there's no room.
     *
     * @param player The player.
     * @param stack The item stack.
     */
    private void giveBack(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack) && cart.level() instanceof ServerLevel level) {
            cart.spawnAtLocation(level, stack);
        }
    }

    /**
     * @brief The most recently fitted upgrade still on the cart.
     *
     * @return The upgrade.
     */
    private CartUpgrade newest() {
        fitOrder.removeIf(upgrade -> !has(upgrade));
        for (CartUpgrade upgrade : installed()) {
            if (!fitOrder.contains(upgrade)) {
                fitOrder.add(upgrade);
            }
        }
        return fitOrder.get(fitOrder.size() - 1);
    }

    /**
     * @brief The shimmer upgrade's job, run each tick.
     *
     * Keeps whoever's riding invisible, the effect wearing off a few seconds after they get out.
     *
     * @param level The level to work in.
     */
    public void tickShimmer(ServerLevel level) {
        if (!has(CartUpgrade.SHIMMER) || !cart.isVehicle()) {
            return;
        }
        for (Entity passenger : cart.getPassengers()) {
            if (!(passenger instanceof LivingEntity rider)) {
                continue;
            }
            if (cart.tickCount % SHIMMER_INTERVAL_TICKS == 0) {
                // No swirling effect particles, which would give the rider away
                rider.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, SHIMMER_EFFECT_TICKS, 0, true, false, true));
            }
            if (cart.getRandom().nextInt(GLINT_CHANCE) == 0) {
                level.sendParticles(GLINT, rider.getX(), rider.getY() + rider.getBbHeight() * 0.6, rider.getZ(), 1, 0.25, 0.3, 0.25, 0.0);
            }
        }
    }

    /**
     * @brief Called when the cart is broken.
     *
     * @param level The level to work in.
     */
    public void dropAll(ServerLevel level) {
        for (CartUpgrade upgrade : installed()) {
            cart.spawnAtLocation(level, new ItemStack(upgrade.item()));
        }
        if (!filter.isEmpty()) {
            cart.spawnAtLocation(level, filter);
            filter = ItemStack.EMPTY;
        }
    }

    public void save(ValueOutput output) {
        // In the order they were fitted
        newest();
        output.putString("Upgrades", fitOrder.stream().map(CartUpgrade::id).collect(Collectors.joining(",")));
        if (!filter.isEmpty()) {
            output.store("Filter", ItemStack.CODEC, filter);
        }
    }

    public void load(ValueInput input) {
        List<String> ids = new ArrayList<>(Arrays.asList(input.getStringOr("Upgrades", "").split(",")));
        int bits = 0;
        fitOrder.clear();
        for (String id : ids) {
            for (CartUpgrade upgrade : CartUpgrade.values()) {
                if (upgrade.id().equals(id) && !fitOrder.contains(upgrade)) {
                    fitOrder.add(upgrade);
                    bits |= upgrade.bit();
                }
            }
        }
        set(bits);
        filter = input.read("Filter", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    /**
     * @brief With a shrieker, only other vehicles can push this one, and anything else bumping into
     *        it is shoved aside.
     *
     * @param entity The entity bumping into it.
     * @return True if it handled the push.
     */
    public boolean pushAside(Entity entity) {
        if (!has(CartUpgrade.SHRIEKER) || entity instanceof VehicleEntity || cart.hasPassenger(entity)) {
            return false;
        }
        Vec3 away = entity.position().subtract(cart.position()).horizontal();
        if (!cart.level().isClientSide() && !entity.noPhysics && away.lengthSqr() > 1.0E-4) {
            away = away.normalize().scale(0.05);
            entity.push(away.x, 0.0, away.z);
        }
        return true;
    }

    /**
     * @brief The shrieker's job, run each tick: shoves anything living in the vehicle's path off to
     *        the nearer side, with a hop.
     *
     * Only something not already moving clear gets shoved, so each is shoved once. For carts, this
     * runs in place of vanilla's pick-up-and-push.
     *
     * @param level The level to work in.
     * @param vehicleMotion The vehicle's motion, since a boat steered by a player only moves on
     *     that player's client.
     * @see transport.rail.XincraftMinecartBehavior#pushAndPickupEntities
     */
    public void clearTrackAhead(ServerLevel level, Vec3 vehicleMotion) {
        if (shriekCooldown > 0) {
            shriekCooldown--;
        }
        Vec3 motion = vehicleMotion.horizontal();
        double speed = motion.length();
        if (speed < MOVING_SPEED) {
            return;
        }
        Vec3 ahead = motion.scale(1.0 / speed);
        Vec3 side = new Vec3(-ahead.z, 0.0, ahead.x);
        AABB path = cart.getBoundingBox().inflate(0.2, 0.0, 0.2)
            .expandTowards(ahead.scale(SHRIEKER_REACH + speed * SHRIEKER_REACH_PER_SPEED));
        boolean shoved = false;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, path,
                e -> e.isAlive() && !e.isSpectator() && !e.isPassenger() && !cart.hasPassenger(e))) {
            double offset = entity.position().subtract(cart.position()).dot(side);
            Vec3 away = side.scale(offset > 0.0 || (offset == 0.0 && cart.getRandom().nextBoolean()) ? 1.0 : -1.0);
            double clearing = entity.getDeltaMovement().dot(away);
            if (clearing >= SHOVE * 0.5) {
                continue;
            }
            entity.setDeltaMovement(entity.getDeltaMovement()
                .add(away.scale(SHOVE - Math.max(0.0, clearing)))
                .add(0.0, entity.onGround() ? HOP : 0.0, 0.0));
            // Sends the new motion to clients straight away (players move themselves)
            entity.hurtMarked = true;
            shoved = true;
        }
        if (shoved && shriekCooldown == 0) {
            level.playSound(null, cart.getX(), cart.getY(), cart.getZ(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.NEUTRAL, 1.0F, 1.0F);
            shriekCooldown = SHRIEK_COOLDOWN_TICKS;
        }
    }
}
