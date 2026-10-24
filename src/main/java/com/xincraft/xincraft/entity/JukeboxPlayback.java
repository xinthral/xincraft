package com.xincraft.xincraft.entity;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;

// The jukebox carried by a vehicle (the jingle cart and the party boat): which disc is in, and when
// its song plays. It plays while the vehicle is moving, starting the song over each time it sets off
// again (a song can't be resumed partway) and looping it for as long as it keeps moving; with a
// sculk sensor upgrade, only while a player is within earshot. Clients play the music themselves
// (client.JukeboxVehicleSound), starting it when the synced play-through number changes.
public final class JukeboxPlayback {
    // Below this horizontal speed (squared) the vehicle counts as stopped
    public static final double STOPPED_SPEED_SQR = 1.0E-4;
    // Earshot for the sculk sensor upgrade: plays while a player is within this many blocks
    public static final double SENSOR_RANGE = 32.0;
    // How long it can sit still before the music stops, so a brief stop (e.g. topping a hill)
    // doesn't cut it off
    private static final int STOP_DELAY_TICKS = 10;
    // Pause before the song plays again from the top
    private static final int LOOP_PAUSE_TICKS = 20;

    private final Entity vehicle;
    private final EntityDataAccessor<ItemStack> discData;
    // Which play-through of the song is under way, so clients can tell a new one from the one
    // they're already playing; 0 while silent
    private final EntityDataAccessor<Integer> sessionData;
    private int stillTicks = STOP_DELAY_TICKS;
    private int songTicks;
    private int pauseTicks;
    private int lastSession;
    // Client only: the play-through this client has heard start, or -1 before its first tick
    private int clientSession = -1;

    public JukeboxPlayback(Entity vehicle, EntityDataAccessor<ItemStack> discData, EntityDataAccessor<Integer> sessionData) {
        this.vehicle = vehicle;
        this.discData = discData;
        this.sessionData = sessionData;
    }

    public ItemStack getDisc() {
        return vehicle.getEntityData().get(discData);
    }

    // Putting the same disc back (as a hopper does when it can't take it after all) leaves the song
    // playing; a different disc stops it
    public void setDisc(ItemStack disc) {
        boolean changed = !ItemStack.matches(getDisc(), disc);
        vehicle.getEntityData().set(discData, disc);
        if (changed) {
            vehicle.getEntityData().set(sessionData, 0);
        }
    }

    public Optional<Holder<JukeboxSong>> getSong() {
        return JukeboxSong.fromStack(getDisc());
    }

    public int getSession() {
        return vehicle.getEntityData().get(sessionData);
    }

    public int getClientSession() {
        return clientSession;
    }

    public void setClientSession(int session) {
        clientSession = session;
    }

    // Run each server tick with how far the vehicle moved and whether a player can hear it
    public void serverTick(double horizontalSpeedSqr, boolean heard) {
        stillTicks = horizontalSpeedSqr < STOPPED_SPEED_SQR ? stillTicks + 1 : 0;
        if (pauseTicks > 0) {
            pauseTicks--;
        }
        Optional<Holder<JukeboxSong>> song = getSong();
        boolean shouldPlay = song.isPresent() && stillTicks < STOP_DELAY_TICKS && heard;
        if (getSession() != 0) {
            songTicks++;
            boolean finished = song.isPresent() && songTicks >= song.get().value().lengthInTicks();
            if (!shouldPlay || finished) {
                vehicle.getEntityData().set(sessionData, 0);
                pauseTicks = finished ? LOOP_PAUSE_TICKS : 0;
            }
        } else if (shouldPlay && pauseTicks == 0) {
            lastSession = lastSession % 1000000 + 1;
            vehicle.getEntityData().set(sessionData, lastSession);
            songTicks = 0;
        }
    }
}
