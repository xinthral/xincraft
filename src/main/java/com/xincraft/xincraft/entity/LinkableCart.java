package com.xincraft.xincraft.entity;

// A minecart a linking bar can hitch into a train (see CartCoupling). Each one holds a CartCoupling and
// hands it its ticks, interactions, pushes, removal and saving. The ender minecart deliberately isn't
// one: there's no good way to teleport a whole linked train through an ender rail.
public interface LinkableCart {
    CartCoupling coupling();

    // How many carts this one can tow behind it when it leads a train, counting every cart down the
    // line: only locomotives can lead
    default int maxTowed() {
        return 0;
    }
}
