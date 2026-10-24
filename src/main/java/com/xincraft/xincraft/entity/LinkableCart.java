package com.xincraft.xincraft.entity;

// A minecart a linking bar can hitch to another (see CartCoupling). Each one holds a CartCoupling and
// hands it its ticks, interactions, pushes, removal and saving.
public interface LinkableCart {
    CartCoupling coupling();

    // Whether this cart can tow another hitched behind it: only locomotives can
    default boolean canTow() {
        return false;
    }
}
