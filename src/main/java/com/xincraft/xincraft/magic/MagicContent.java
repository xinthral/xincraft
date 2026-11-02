package com.xincraft.xincraft.magic;

public final class MagicContent {
    // Java only runs a class's static fields when the class is first used, so a holder nothing has
    // touched yet would register nothing. Call this before the registers go on the mod bus.
    public static void init() {
        MagicBlocks.init();
        MagicItems.init();
        MagicBlockEntities.init();
        MagicMenus.init();
    }

    private MagicContent() {}
}
