package com.xincraft.xincraft.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.xincraft.xincraft.item.CartUpgradeItem;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// The upgrades fitted to one minecart, and what they do. Upgrades are fitted by using the upgrade
// item on the cart and snipped off one at a time with shears; each can be fitted once, and the
// cart drops them when broken. The installed set is synced to clients (as flags in an int the cart
// defines) so they can show the upgrades on the cart.
public final class CartUpgrades {
    // How far ahead of the cart a shrieker clears the track, plus a little more the faster it goes
    private static final double SHRIEKER_REACH = 3.0;
    private static final double SHRIEKER_REACH_PER_SPEED = 4.0;
    // Sideways shove and hop given to anything in the way: enough to land it clear of the cart
    private static final double SHOVE = 0.5;
    private static final double HOP = 0.3;
    private static final int SHRIEK_COOLDOWN_TICKS = 40;
    // Below this horizontal speed the cart has no direction to clear in
    private static final double MOVING_SPEED = 0.05;

    private final AbstractMinecart cart;
    private final EntityDataAccessor<Integer> data;
    private int shriekCooldown;

    public CartUpgrades(AbstractMinecart cart, EntityDataAccessor<Integer> data) {
        this.cart = cart;
        this.data = data;
    }

    public boolean has(CartUpgrade upgrade) {
        return (bits() & upgrade.bit()) != 0;
    }

    public List<CartUpgrade> installed() {
        return Arrays.stream(CartUpgrade.values()).filter(this::has).toList();
    }

    private int bits() {
        return cart.getEntityData().get(data);
    }

    private void set(int bits) {
        cart.getEntityData().set(data, bits);
    }

    // Fits an upgrade item, or snips the last fitted upgrade off with shears. PASS for anything
    // else, so the cart can carry on with its own interaction.
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
            if (server) {
                set(bits() | upgrade.bit());
                stack.consume(1, player);
                cart.playSound(SoundEvents.SMITHING_TABLE_USE, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.SHEARS) && bits() != 0) {
            if (server && cart.level() instanceof ServerLevel level) {
                List<CartUpgrade> installed = installed();
                CartUpgrade last = installed.get(installed.size() - 1);
                set(bits() & ~last.bit());
                cart.spawnAtLocation(level, new ItemStack(last.item()));
                stack.hurtAndBreak(1, player, hand);
                cart.playSound(SoundEvents.SHEEP_SHEAR, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // Called when the cart is broken
    public void dropAll(ServerLevel level) {
        for (CartUpgrade upgrade : installed()) {
            cart.spawnAtLocation(level, new ItemStack(upgrade.item()));
        }
    }

    public void save(ValueOutput output) {
        output.putString("Upgrades", installed().stream().map(CartUpgrade::id).collect(Collectors.joining(",")));
    }

    public void load(ValueInput input) {
        List<String> ids = new ArrayList<>(Arrays.asList(input.getStringOr("Upgrades", "").split(",")));
        int bits = 0;
        for (CartUpgrade upgrade : CartUpgrade.values()) {
            if (ids.contains(upgrade.id())) {
                bits |= upgrade.bit();
            }
        }
        set(bits);
    }

    // With a shrieker, only other minecarts can push the cart: anything else bumping into it is
    // shoved aside instead, so it never slows the cart down. Returns whether it handled the push.
    public boolean pushAside(Entity entity) {
        if (!has(CartUpgrade.SHRIEKER) || entity instanceof AbstractMinecart || cart.hasPassenger(entity)) {
            return false;
        }
        Vec3 away = entity.position().subtract(cart.position()).horizontal();
        if (!cart.level().isClientSide() && !entity.noPhysics && away.lengthSqr() > 1.0E-4) {
            away = away.normalize().scale(0.05);
            entity.push(away.x, 0.0, away.z);
        }
        return true;
    }

    // The shrieker's job, run each tick in place of vanilla's pick-up-and-push (see
    // XincraftMinecartBehavior#pushAndPickupEntities): anything living on the track ahead is shoved
    // off to whichever side it's already nearer, with a hop, before the cart reaches it. Only
    // shoves something not already moving clear, so each is shoved once.
    public void clearTrackAhead(ServerLevel level) {
        if (shriekCooldown > 0) {
            shriekCooldown--;
        }
        Vec3 motion = cart.getDeltaMovement().horizontal();
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
