package com.xincraft.xincraft.common;

public final class CommonContent {
    // Java only runs a class's static fields when the class is first used, so a holder nothing has
    // touched yet would register nothing. Call this before the registers go on the mod bus.
    public static void init() {
        CommonItems.init();
        CommonComponents.init();
    }

    private CommonContent() {}
}
