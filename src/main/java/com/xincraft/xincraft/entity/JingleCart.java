package com.xincraft.xincraft.entity;

import com.xincraft.xincraft.client.JukeboxVehicleSound;
import com.xincraft.xincraft.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.ContainerSingleItem;

// A minecart carrying a jukebox. Right-click with a music disc to put it in, and right-click again
// to take it out. It plays the disc while it's rolling, the music following the cart along the
// track (see JukeboxPlayback); with a sculk sensor upgrade, only while a player is within earshot.
// Like a jukebox, it holds its disc as a one-slot container, so
// hoppers (and hopper minecarts) can load and unload it, and a detector rail's comparator signal
// shows whether it has a disc. Can be hitched into a locomotive's train with a linking bar (see CartCoupling).
public class JingleCart extends AbstractMinecart implements UpgradableCart, LinkableCart, JukeboxVehicle, ContainerSingleItem {
    private static final EntityDataAccessor<Integer> DATA_UPGRADES = SynchedEntityData.defineId(JingleCart.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TOWED = SynchedEntityData.defineId(JingleCart.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> DATA_DISC = SynchedEntityData.defineId(JingleCart.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> DATA_SESSION = SynchedEntityData.defineId(JingleCart.class, EntityDataSerializers.INT);

    private final CartUpgrades upgrades = new CartUpgrades(this, DATA_UPGRADES);
    private final JukeboxPlayback jukebox = new JukeboxPlayback(this, DATA_DISC, DATA_SESSION);
    private final CartCoupling coupling = new CartCoupling(this, DATA_TOWED);

    public JingleCart(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_UPGRADES, 0);
        builder.define(DATA_TOWED, CartCoupling.NONE);
        builder.define(DATA_DISC, ItemStack.EMPTY);
        builder.define(DATA_SESSION, 0);
    }

    @Override
    public CartUpgrades upgrades() {
        return upgrades;
    }

    @Override
    public CartCoupling coupling() {
        return coupling;
    }

    @Override
    public JukeboxPlayback jukebox() {
        return jukebox;
    }

    public ItemStack getDisc() {
        return jukebox.getDisc();
    }

    private void setDisc(ItemStack disc) {
        jukebox.setDisc(disc);
    }

    // The container: one slot holding the disc

    @Override
    public ItemStack getTheItem() {
        return getDisc();
    }

    // Hands over the disc itself rather than splitting the synced stack in place
    @Override
    public ItemStack splitTheItem(int count) {
        ItemStack disc = getDisc();
        setDisc(ItemStack.EMPTY);
        return disc;
    }

    @Override
    public void setTheItem(ItemStack stack) {
        setDisc(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return JukeboxSong.fromStack(stack).isPresent() && getDisc().isEmpty();
    }

    // Only lets a hopper take the disc when it has room for it, so a full hopper doesn't keep pulling
    // the disc out and putting it back
    @Override
    public boolean canTakeItem(Container into, int slot, ItemStack stack) {
        return into.hasAnyMatching(ItemStack::isEmpty);
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return !isRemoved() && player.isWithinEntityInteractionRange(getBoundingBox(), 4.0);
    }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel serverLevel) {
            coupling.tick(serverLevel);
        }
        super.tick();
        if (level() instanceof ServerLevel serverLevel) {
            boolean heard = !upgrades.has(CartUpgrade.SCULK_SENSOR)
                || serverLevel.hasNearbyAlivePlayer(getX(), getY(), getZ(), JukeboxPlayback.SENSOR_RANGE);
            jukebox.serverTick(getDeltaMovement().horizontalDistanceSqr(), heard);
        } else {
            // Only reached on the client, so the client-only class is never loaded on a server
            JukeboxVehicleSound.update(this);
            if (jukebox.getSession() != 0 && tickCount % 10 == 0) {
                level().addParticle(ParticleTypes.NOTE, getX(), getY() + 1.2, getZ(), random.nextInt(25) / 24.0, 0.0, 0.0);
            }
        }
    }

    // The linking bar and upgrades first; then a disc goes in, or the disc comes out
    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        InteractionResult result = coupling.interact(player, hand);
        if (result == InteractionResult.PASS) {
            result = upgrades.interact(this, player, hand);
        }
        if (result != InteractionResult.PASS) {
            return result;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!getDisc().isEmpty()) {
            if (level() instanceof ServerLevel level) {
                ItemStack disc = getDisc();
                setDisc(ItemStack.EMPTY);
                if (!player.getInventory().add(disc)) {
                    spawnAtLocation(level, disc);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (JukeboxSong.fromStack(stack).isPresent()) {
            if (!level().isClientSide()) {
                setDisc(stack.copyWithCount(1));
                stack.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
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
    public void remove(RemovalReason reason) {
        coupling.onRemove(reason);
        super.remove(reason);
    }

    @Override
    protected void destroy(ServerLevel level, DamageSource source) {
        super.destroy(level, source);
        if (!getDisc().isEmpty()) {
            spawnAtLocation(level, getDisc());
        }
        upgrades.dropAll(level);
    }

    // A jukebox, showing the disc when there's one in
    @Override
    public BlockState getDefaultDisplayBlockState() {
        return Blocks.JUKEBOX.defaultBlockState().setValue(JukeboxBlock.HAS_RECORD, !getDisc().isEmpty());
    }

    @Override
    protected Item getDropItem() {
        return ModItems.JINGLE_CART.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.JINGLE_CART.get());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (!getDisc().isEmpty()) {
            output.store("Disc", ItemStack.CODEC, getDisc());
        }
        upgrades.save(output);
        coupling.save(output);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setDisc(input.read("Disc", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        upgrades.load(input);
        coupling.load(input);
    }
}
