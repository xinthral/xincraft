package com.xincraft.xincraft.golems;

public final class GolemsContent {
    // Java only runs a class's static fields when the class is first used, so a holder nothing has
    // touched yet would register nothing. Call this before the registers go on the mod bus.
    public static void init() {
        GolemBlocks.init();
        GolemItems.init();
        GolemBlockEntities.init();
        GolemEntityTypes.init();
        GolemMenus.init();
    }

    private GolemsContent() {}
}
