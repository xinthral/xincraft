package com.xincraft.xincraft.client;

import com.xincraft.xincraft.entity.JukeboxPlayback;
import com.xincraft.xincraft.entity.JukeboxVehicle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

// The music of a vehicle carrying a jukebox (the jingle cart and the party boat): the disc's song,
// played like a jukebox (same volume, falloff and volume slider) but following the vehicle as it
// moves. It stops when that play-through ends on the server (the vehicle stopped, the disc came out,
// or the song finished) or the vehicle goes away.
public class JukeboxVehicleSound extends AbstractTickableSoundInstance {
    private final Entity vehicle;
    private final JukeboxPlayback jukebox;
    private final int session;

    private JukeboxVehicleSound(Entity vehicle, JukeboxPlayback jukebox, SoundEvent sound, int session) {
        super(sound, SoundSource.RECORDS, SoundInstance.createUnseededRandom());
        this.vehicle = vehicle;
        this.jukebox = jukebox;
        this.session = session;
        this.volume = 4.0F;
        this.attenuation = Attenuation.LINEAR;
        moveToVehicle();
    }

    // Run on each client tick of the vehicle: starts the song when the server starts a new
    // play-through. A play-through already under way when this client first sees the vehicle is
    // left alone, since a song can't start partway; it's heard from its next one.
    public static <T extends Entity & JukeboxVehicle> void update(T vehicle) {
        JukeboxPlayback jukebox = vehicle.jukebox();
        int session = jukebox.getSession();
        if (jukebox.getClientSession() == -1) {
            jukebox.setClientSession(session);
            return;
        }
        if (session == jukebox.getClientSession()) {
            return;
        }
        jukebox.setClientSession(session);
        if (session != 0) {
            jukebox.getSong().ifPresent(song -> Minecraft.getInstance().getSoundManager().play(
                new JukeboxVehicleSound(vehicle, jukebox, song.value().soundEvent().value(), session)));
        }
    }

    @Override
    public void tick() {
        if (vehicle.isRemoved() || jukebox.getSession() != session) {
            stop();
            return;
        }
        moveToVehicle();
    }

    private void moveToVehicle() {
        x = vehicle.getX();
        y = vehicle.getY();
        z = vehicle.getZ();
    }
}
