package com.xincraft.xincraft.entity;

import com.xincraft.xincraft.registry.ModItems;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

// A rideable passenger minecart, lacquered and trimmed in bronze, for hitching behind a locomotive
// with a linking bar (see CartCoupling). Otherwise an ordinary minecart. Takes upgrades (see
// CartUpgrades).
public class Carriage extends Minecart implements UpgradableCart, LinkableCart {
    private static final EntityDataAccessor<Integer> DATA_UPGRADES = SynchedEntityData.defineId(Carriage.class, EntityDataSerializers.INT);

    private final CartUpgrades upgrades = new CartUpgrades(this, DATA_UPGRADES);
    private final CartCoupling coupling = new CartCoupling(this, null);

    public Carriage(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_UPGRADES, 0);
    }

    @Override
    public CartUpgrades upgrades() {
        return upgrades;
    }

    @Override
    public CartCoupling coupling() {
        return coupling;
    }

    // The linking bar and upgrades first; then right-click to ride, as any minecart
    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        InteractionResult result = coupling.interact(player, hand);
        if (result == InteractionResult.PASS) {
            result = upgrades.interact(this, player, hand);
        }
        return result != InteractionResult.PASS ? result : super.interact(player, hand, location);
    }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel serverLevel) {
            coupling.tick(serverLevel);
            upgrades.tickShimmer(serverLevel);
        }
        super.tick();
    }

    @Override
    public void push(Entity entity) {
        if (!coupling.isLinkedTo(entity) && !upgrades.pushAside(entity)) {
            super.push(entity);
        }
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return !coupling.isLinkedTo(entity) && super.canCollideWith(entity);
    }

    @Override
    protected void destroy(ServerLevel level, DamageSource source) {
        super.destroy(level, source);
        upgrades.dropAll(level);
    }

    @Override
    public void remove(RemovalReason reason) {
        coupling.onRemove(reason);
        super.remove(reason);
    }

    @Override
    protected Item getDropItem() {
        return ModItems.CARRIAGE.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.CARRIAGE.get());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        upgrades.save(output);
        coupling.save(output);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        upgrades.load(input);
        coupling.load(input);
    }
}
