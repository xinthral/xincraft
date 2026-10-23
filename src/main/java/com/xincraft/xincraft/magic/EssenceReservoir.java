package com.xincraft.xincraft.magic;

// Something that stores essence of a single element, for rituals to draw from. The elemental flora
// (block.FloraBlockEntity) is the first; EssenceHelper finds and drains them.
public interface EssenceReservoir {
    Element getElement();

    int getEssence();

    int getCapacity();

    // Adds up to amount essence and returns how much went in. With simulate, nothing changes.
    int insert(int amount, boolean simulate);

    // Takes up to amount essence out and returns how much came out. With simulate, nothing changes.
    int extract(int amount, boolean simulate);
}
