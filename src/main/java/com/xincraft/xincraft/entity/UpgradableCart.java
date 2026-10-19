package com.xincraft.xincraft.entity;

// A minecart that upgrades can be fitted to. Each one holds a CartUpgrades and hands it the cart's
// interactions, pushes, saving and drops.
public interface UpgradableCart {
    CartUpgrades upgrades();

    // Upgrades for one kind of cart (e.g. the sculk sensor's music cue) only go on that cart
    default boolean accepts(CartUpgrade upgrade) {
        return upgrade.fitsOnly() == null || upgrade.fitsOnly().isInstance(this);
    }
}
