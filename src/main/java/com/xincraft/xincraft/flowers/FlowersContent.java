package com.xincraft.xincraft.flowers;

public final class FlowersContent {
    // Java only runs a class's static fields when the class is first used, so a holder nothing has
    // touched yet would register nothing. Call this before the registers go on the mod bus.
    public static void init() {
        FlowerBlocks.init();
        FlowerItems.init();
        FlowerBlockEntities.init();
    }

    private FlowersContent() {}
}
