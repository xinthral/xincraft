package com.xincraft.xincraft.compendium;

public final class CompendiumContent {
    /**
     * @brief Java only runs a class's static fields when the class is first used, so a holder
     *        nothing has touched yet would register nothing.
     *
     * Call this before the registers go on the mod bus.
     */
    public static void init() {
        CompendiumItems.init();
    }

    private CompendiumContent() {}
}
