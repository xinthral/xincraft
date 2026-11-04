package com.xincraft.xincraft.magic;

/**
 * @brief Something that stores essence of a single element, for rituals to draw from.
 *
 * The elemental flora is the first; EssenceHelper finds and drains them.
 *
 * @see FloraBlockEntity
 */
public interface EssenceReservoir {
    Element getElement();

    int getEssence();

    int getCapacity();

    /**
     * @brief Adds up to amount essence.
     *
     * @param amount The most to add.
     * @param simulate If true, nothing changes.
     * @return How much went in.
     */
    int insert(int amount, boolean simulate);

    /**
     * @brief Takes up to amount essence out.
     *
     * @param amount The most to take.
     * @param simulate If true, nothing changes.
     * @return How much came out.
     */
    int extract(int amount, boolean simulate);
}
