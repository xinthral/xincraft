package com.xincraft.xincraft.network;

import com.xincraft.xincraft.XincraftMod;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

// The mod's network channel. So far it only carries the compendium's recipes to clients
// (CompendiumRecipesPacket), sent whenever the server's data packs sync: on login and on /reload.
@Mod.EventBusSubscriber(modid = XincraftMod.MODID)
public final class ModNetwork {
    private static final int PROTOCOL_VERSION = 1;

    @SuppressWarnings("deprecation")
    public static final SimpleChannel CHANNEL = ChannelBuilder
        .named(Identifier.fromNamespaceAndPath(XincraftMod.MODID, "main"))
        .networkProtocolVersion(PROTOCOL_VERSION)
        .simpleChannel()
        .messageBuilder(CompendiumRecipesPacket.class, NetworkDirection.PLAY_TO_CLIENT)
        .codec(CompendiumRecipesPacket.STREAM_CODEC)
        .consumerMainThread(CompendiumRecipesPacket::handle)
        .add()
        .build();

    private ModNetwork() {}

    // Loads the class, creating the channel; called from common setup
    public static void register() {}

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        for (ServerPlayer player : event.getPlayers()) {
            CHANNEL.send(CompendiumRecipesPacket.collect(player.level()), PacketDistributor.PLAYER.with(player));
        }
    }
}
