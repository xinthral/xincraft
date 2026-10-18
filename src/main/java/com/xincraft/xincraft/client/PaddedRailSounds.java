package com.xincraft.xincraft.client;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import com.xincraft.xincraft.Config;
import com.xincraft.xincraft.XincraftMod;
import com.xincraft.xincraft.registry.ModTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.MinecartSoundInstance;
import net.minecraft.client.resources.sounds.RidingMinecartSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jspecify.annotations.Nullable;

// Muffles minecart rolling sounds while the cart is on a padded rail. Vanilla's minecart sound
// instances compute their own volume each tick, so we wrap them as they start playing and scale
// whatever volume they produce.
@Mod.EventBusSubscriber(modid = XincraftMod.MODID, value = Dist.CLIENT)
public final class PaddedRailSounds {
    // Fraction of the way the volume moves toward its target each tick, so entering or leaving
    // a padded section fades instead of cutting abruptly
    private static final float FADE_RATE = 0.25F;

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound instanceof MinecartSoundInstance minecartSound) {
            // Positional sound heard by everyone nearby; it tracks the cart's position each tick
            event.setSound(new Muffled(minecartSound, () -> new Vec3(minecartSound.getX(), minecartSound.getY(), minecartSound.getZ())));
        } else if (sound instanceof RidingMinecartSoundInstance ridingSound) {
            // Non-positional sound heard by the local rider; probe the cart they're sitting in
            event.setSound(new Muffled(ridingSound, () -> {
                var player = Minecraft.getInstance().player;
                Entity vehicle = player == null ? null : player.getVehicle();
                return vehicle == null ? null : vehicle.position();
            }));
        }
    }

    // Mirrors AbstractMinecart#getCurrentBlockPosOrRailBelow: the rail is either in the cart's
    // block or, on slopes and with experimental minecart movement, the block just below it
    private static boolean isOnPaddedRail(Level level, Vec3 pos) {
        BlockPos blockPos = BlockPos.containing(pos);
        return level.getBlockState(blockPos).is(ModTags.PADDED_RAILS)
            || level.getBlockState(blockPos.below()).is(ModTags.PADDED_RAILS);
    }

    private static final class Muffled implements TickableSoundInstance {
        private final TickableSoundInstance delegate;
        private final Supplier<@Nullable Vec3> cartPosition;
        private float multiplier = 1.0F;

        private Muffled(TickableSoundInstance delegate, Supplier<@Nullable Vec3> cartPosition) {
            this.delegate = delegate;
            this.cartPosition = cartPosition;
        }

        @Override
        public void tick() {
            delegate.tick();
            Level level = Minecraft.getInstance().level;
            Vec3 pos = cartPosition.get();
            float target = level != null && pos != null && isOnPaddedRail(level, pos) ? Config.paddedRailVolume : 1.0F;
            multiplier += (target - multiplier) * FADE_RATE;
        }

        @Override
        public float getVolume() {
            return delegate.getVolume() * multiplier;
        }

        @Override public boolean isStopped() { return delegate.isStopped(); }
        @Override public Identifier getIdentifier() { return delegate.getIdentifier(); }
        @Override public @Nullable WeighedSoundEvents resolve(SoundManager soundManager) { return delegate.resolve(soundManager); }
        @Override public @Nullable Sound getSound() { return delegate.getSound(); }
        @Override public SoundSource getSource() { return delegate.getSource(); }
        @Override public boolean isLooping() { return delegate.isLooping(); }
        @Override public boolean isRelative() { return delegate.isRelative(); }
        @Override public int getDelay() { return delegate.getDelay(); }
        @Override public float getPitch() { return delegate.getPitch(); }
        @Override public double getX() { return delegate.getX(); }
        @Override public double getY() { return delegate.getY(); }
        @Override public double getZ() { return delegate.getZ(); }
        @Override public Attenuation getAttenuation() { return delegate.getAttenuation(); }
        @Override public boolean canStartSilent() { return delegate.canStartSilent(); }
        @Override public boolean canPlaySound() { return delegate.canPlaySound(); }

        @Override
        public CompletableFuture<AudioStream> getStream(SoundBufferLibrary soundBuffers, Sound sound, boolean looping) {
            return delegate.getStream(soundBuffers, sound, looping);
        }
    }

    private PaddedRailSounds() {}
}
