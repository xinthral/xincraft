package com.xincraft.xincraft.transport.train;

import com.xincraft.xincraft.common.MobHolder;

/**
 * @brief A minecart or boat that upgrades can be fitted to.
 *
 * Each one holds a CartUpgrades and hands it its interactions, pushes, saving and drops.
 */
public interface UpgradableCart extends MobHolder {
    CartUpgrades upgrades();

    @Override
    default boolean holdsMobs() {
        return upgrades().has(CartUpgrade.CELL);
    }

    /**
     * @brief Whether an upgrade fits, as upgrades for certain vehicles only go on those.
     *
     * @param upgrade The upgrade to check.
     * @return True if An upgrade fits, as upgrades for certain vehicles only go on those.
     */
    default boolean accepts(CartUpgrade upgrade) {
        return upgrade.fits().isEmpty() || upgrade.fits().stream().anyMatch(type -> type.isInstance(this));
    }

    /**
     * @brief Whether a mob filter can be fitted, to choose which mobs come aboard.
     *
     * @return True if A mob filter can be fitted, to choose which mobs come aboard.
     * @see common.MobFilterItem
     */
    default boolean acceptsFilter() {
        return false;
    }
}
