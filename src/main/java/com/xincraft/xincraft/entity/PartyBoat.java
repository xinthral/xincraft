package com.xincraft.xincraft.entity;

import java.util.function.Supplier;
import com.xincraft.xincraft.client.JukeboxVehicleSound;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

// A bronze-lined motorboat carrying a jukebox where the second seat would be. It's a motorboat in
// every other way (engine, fuel, storage, upgrades, buoy paths), and plays its disc while it's on
// the move, like the jingle cart (see JukeboxPlayback); with a sculk sensor upgrade, only while a
// player is within earshot. Right-click with a music disc to put it in (swapping out any disc
// already there); shift-right-click with an empty hand takes the disc out, or opens the boat's
// slots when there's no disc.
public class PartyBoat extends Motorboat implements JukeboxVehicle {
    private static final EntityDataAccessor<ItemStack> DATA_DISC = SynchedEntityData.defineId(PartyBoat.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> DATA_SESSION = SynchedEntityData.defineId(PartyBoat.class, EntityDataSerializers.INT);

    private final JukeboxPlayback jukebox = new JukeboxPlayback(this, DATA_DISC, DATA_SESSION);

    public PartyBoat(EntityType<? extends PartyBoat> type, Level level, Supplier<Item> dropItem) {
        super(type, level, dropItem, false);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DISC, ItemStack.EMPTY);
        builder.define(DATA_SESSION, 0);
    }

    @Override
    public JukeboxPlayback jukebox() {
        return jukebox;
    }

    // The jukebox takes the back seat
    @Override
    protected int getMaxPassengers() {
        return 1;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            // Only reached on the client, so the client-only class is never loaded on a server
            JukeboxVehicleSound.update(this);
            if (jukebox.getSession() != 0 && tickCount % 10 == 0) {
                level().addParticle(ParticleTypes.NOTE, getX(), getY() + 1.2, getZ(), random.nextInt(25) / 24.0, 0.0, 0.0);
            }
        }
    }

    @Override
    protected void onServerTick(ServerLevel level, Vec3 motion) {
        boolean heard = !upgrades().has(CartUpgrade.SCULK_SENSOR)
            || level.hasNearbyAlivePlayer(getX(), getY(), getZ(), JukeboxPlayback.SENSOR_RANGE);
        jukebox.serverTick(motion.horizontalDistanceSqr(), heard);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        ItemStack stack = player.getItemInHand(hand);
        if (JukeboxSong.fromStack(stack).isPresent() && !player.isSecondaryUseActive()) {
            if (level() instanceof ServerLevel level) {
                giveBackDisc(level, player);
                jukebox.setDisc(stack.copyWithCount(1));
                stack.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive() && stack.isEmpty() && !jukebox.getDisc().isEmpty()) {
            if (level() instanceof ServerLevel level) {
                giveBackDisc(level, player);
            }
            return InteractionResult.SUCCESS;
        }
        return super.interact(player, hand, location);
    }

    private void giveBackDisc(ServerLevel level, Player player) {
        ItemStack disc = jukebox.getDisc();
        if (disc.isEmpty()) {
            return;
        }
        jukebox.setDisc(ItemStack.EMPTY);
        if (!player.getInventory().add(disc)) {
            spawnAtLocation(level, disc);
        }
    }

    @Override
    public void destroy(ServerLevel level, DamageSource source) {
        super.destroy(level, source);
        if (!jukebox.getDisc().isEmpty()) {
            spawnAtLocation(level, jukebox.getDisc());
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (!jukebox.getDisc().isEmpty()) {
            output.store("Disc", ItemStack.CODEC, jukebox.getDisc());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        jukebox.setDisc(input.read("Disc", ItemStack.CODEC).orElse(ItemStack.EMPTY));
    }
}
