package com.xincraft.xincraft.entity;

import java.util.Optional;
import com.xincraft.xincraft.client.JingleCartSound;
import com.xincraft.xincraft.registry.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
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

// A minecart carrying a jukebox. Right-click with a music disc to put it in, and right-click again
// to take it out. It plays the disc while it's rolling, the music following the cart along the
// track, and starts the song over each time it sets off again (a song can't be resumed partway).
// It loops the song for as long as it keeps moving. With a sculk sensor upgrade, it only plays
// while a player is within earshot.
public class JingleCart extends AbstractMinecart implements UpgradableCart {
    private static final EntityDataAccessor<Integer> DATA_UPGRADES = SynchedEntityData.defineId(JingleCart.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> DATA_DISC = SynchedEntityData.defineId(JingleCart.class, EntityDataSerializers.ITEM_STACK);
    // Which play-through of the song is under way, so clients can tell a new one from the one
    // they're already playing; 0 while silent
    private static final EntityDataAccessor<Integer> DATA_SESSION = SynchedEntityData.defineId(JingleCart.class, EntityDataSerializers.INT);

    // Below this horizontal speed (squared) the cart counts as stopped
    private static final double STOPPED_SPEED_SQR = 1.0E-4;
    // How long it can sit still before the music stops, so a brief stop (e.g. topping a hill)
    // doesn't cut it off
    private static final int STOP_DELAY_TICKS = 10;
    // Pause before the song plays again from the top
    private static final int LOOP_PAUSE_TICKS = 20;
    // Earshot for the sculk sensor upgrade: plays while a player is within this many blocks
    private static final double SENSOR_RANGE = 16.0;

    private final CartUpgrades upgrades = new CartUpgrades(this, DATA_UPGRADES);
    private int stillTicks = STOP_DELAY_TICKS;
    private int songTicks;
    private int pauseTicks;
    private int lastSession;
    // Client only: the play-through this client has heard start, or -1 before its first tick
    private int clientSession = -1;

    public JingleCart(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_UPGRADES, 0);
        builder.define(DATA_DISC, ItemStack.EMPTY);
        builder.define(DATA_SESSION, 0);
    }

    @Override
    public CartUpgrades upgrades() {
        return upgrades;
    }

    // Every upgrade fits the jingle cart, including the ones only it can use
    @Override
    public boolean accepts(CartUpgrade upgrade) {
        return true;
    }

    public ItemStack getDisc() {
        return entityData.get(DATA_DISC);
    }

    private void setDisc(ItemStack disc) {
        entityData.set(DATA_DISC, disc);
        entityData.set(DATA_SESSION, 0);
    }

    public Optional<Holder<JukeboxSong>> getSong() {
        return JukeboxSong.fromStack(getDisc());
    }

    public int getSession() {
        return entityData.get(DATA_SESSION);
    }

    public int getClientSession() {
        return clientSession;
    }

    public void setClientSession(int session) {
        clientSession = session;
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel serverLevel) {
            updatePlayback(serverLevel);
        } else {
            // Only reached on the client, so the client-only class is never loaded on a server
            JingleCartSound.update(this);
            if (getSession() != 0 && tickCount % 10 == 0) {
                level().addParticle(ParticleTypes.NOTE, getX(), getY() + 1.2, getZ(), random.nextInt(25) / 24.0, 0.0, 0.0);
            }
        }
    }

    private void updatePlayback(ServerLevel level) {
        stillTicks = getDeltaMovement().horizontalDistanceSqr() < STOPPED_SPEED_SQR ? stillTicks + 1 : 0;
        if (pauseTicks > 0) {
            pauseTicks--;
        }
        Optional<Holder<JukeboxSong>> song = getSong();
        boolean heard = !upgrades.has(CartUpgrade.SCULK_SENSOR) || level.hasNearbyAlivePlayer(getX(), getY(), getZ(), SENSOR_RANGE);
        boolean shouldPlay = song.isPresent() && stillTicks < STOP_DELAY_TICKS && heard;
        if (getSession() != 0) {
            songTicks++;
            boolean finished = song.isPresent() && songTicks >= song.get().value().lengthInTicks();
            if (!shouldPlay || finished) {
                entityData.set(DATA_SESSION, 0);
                pauseTicks = finished ? LOOP_PAUSE_TICKS : 0;
            }
        } else if (shouldPlay && pauseTicks == 0) {
            lastSession = lastSession % 1000000 + 1;
            entityData.set(DATA_SESSION, lastSession);
            songTicks = 0;
        }
    }

    // Upgrades first; then a disc goes in, or the disc comes out
    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        InteractionResult result = upgrades.interact(this, player, hand);
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
        if (!upgrades.pushAside(entity)) {
            super.push(entity);
        }
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
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setDisc(input.read("Disc", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        upgrades.load(input);
    }
}
