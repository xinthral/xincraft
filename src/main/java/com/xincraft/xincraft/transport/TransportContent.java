package com.xincraft.xincraft.transport;

public final class TransportContent {
    // Java only runs a class's static fields when the class is first used, so a holder nothing has
    // touched yet would register nothing. Call this before the registers go on the mod bus.
    public static void init() {
        TransportBlocks.init();
        TransportItems.init();
        TransportBlockEntities.init();
        TransportEntityTypes.init();
        TransportMenus.init();
        TransportComponents.init();
    }

    private TransportContent() {}
}
