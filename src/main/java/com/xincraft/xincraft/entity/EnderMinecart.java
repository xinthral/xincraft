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

// A rideable minecart on ender wheels. Unless the config allows other minecarts, it's the only
// cart that ender rails will teleport (see block.EnderRailBlock). Takes upgrades (see CartUpgrades).
public class EnderMinecart extends Minecart implements UpgradableCart {
    private static final EntityDataAccessor<Integer> DATA_UPGRADES = SynchedEntityData.defineId(EnderMinecart.class, EntityDataSerializers.INT);

    private final CartUpgrades upgrades = new CartUpgrades(this, DATA_UPGRADES);

    public EnderMinecart(EntityType<?> type, Level level) {
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
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        InteractionResult result = upgrades.interact(this, player, hand);
        return result != InteractionResult.PASS ? result : super.interact(player, hand, location);
    }

    @Override
    public void push(Entity entity) {
        if (!upgrades.pushAside(entity)) {
            super.push(entity);
        }
    }

    @Override
    protected void destroy(ServerLevel level, DamageSource source) {
        super.destroy(level, source);
        upgrades.dropAll(level);
    }

    @Override
    protected Item getDropItem() {
        return ModItems.ENDER_MINECART.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.ENDER_MINECART.get());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        upgrades.save(output);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        upgrades.load(input);
    }
}
