package com.xincraft.xincraft.client;

import com.xincraft.xincraft.entity.JingleCart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

// A jingle cart's music: the disc's song, played like a jukebox (same volume, falloff and volume
// slider) but following the cart as it moves. It stops when that play-through ends on the server
// (the cart stopped, the disc came out, or the song finished) or the cart goes away.
public class JingleCartSound extends AbstractTickableSoundInstance {
    private final JingleCart cart;
    private final int session;

    private JingleCartSound(JingleCart cart, SoundEvent sound, int session) {
        super(sound, SoundSource.RECORDS, SoundInstance.createUnseededRandom());
        this.cart = cart;
        this.session = session;
        this.volume = 4.0F;
        this.attenuation = Attenuation.LINEAR;
        moveToCart();
    }

    // Run on each client tick of a jingle cart: starts the song when the server starts a new
    // play-through. A play-through already under way when this client first sees the cart is left
    // alone, since a song can't start partway; the cart is heard from its next one.
    public static void update(JingleCart cart) {
        int session = cart.getSession();
        if (cart.getClientSession() == -1) {
            cart.setClientSession(session);
            return;
        }
        if (session == cart.getClientSession()) {
            return;
        }
        cart.setClientSession(session);
        if (session != 0) {
            cart.getSong().ifPresent(song ->
                Minecraft.getInstance().getSoundManager().play(new JingleCartSound(cart, song.value().soundEvent().value(), session)));
        }
    }

    @Override
    public void tick() {
        if (cart.isRemoved() || cart.getSession() != session) {
            stop();
            return;
        }
        moveToCart();
    }

    private void moveToCart() {
        x = cart.getX();
        y = cart.getY();
        z = cart.getZ();
    }
}
