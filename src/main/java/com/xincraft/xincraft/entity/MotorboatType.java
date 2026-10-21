package com.xincraft.xincraft.entity;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

// The kinds of motorboat: one for each vanilla boat (and the bamboo raft), made by crafting that
// boat with a propeller. Each is its own entity type and item, and looks like the boat it came from.
public enum MotorboatType {
    OAK("oak", Items.OAK_BOAT, false),
    SPRUCE("spruce", Items.SPRUCE_BOAT, false),
    BIRCH("birch", Items.BIRCH_BOAT, false),
    JUNGLE("jungle", Items.JUNGLE_BOAT, false),
    ACACIA("acacia", Items.ACACIA_BOAT, false),
    CHERRY("cherry", Items.CHERRY_BOAT, false),
    DARK_OAK("dark_oak", Items.DARK_OAK_BOAT, false),
    PALE_OAK("pale_oak", Items.PALE_OAK_BOAT, false),
    MANGROVE("mangrove", Items.MANGROVE_BOAT, false),
    BAMBOO("bamboo", Items.BAMBOO_RAFT, true);

    private final String wood;
    private final Item boat;
    private final boolean raft;

    MotorboatType(String wood, Item boat, boolean raft) {
        this.wood = wood;
        this.boat = boat;
        this.raft = raft;
    }

    public String wood() {
        return wood;
    }

    // The registry name of the motorboat's item and entity type, e.g. oak_motorboat, bamboo_motor_raft
    public String id() {
        return wood + (raft ? "_motor_raft" : "_motorboat");
    }

    // The vanilla boat it's made from
    public Item boat() {
        return boat;
    }

    public boolean raft() {
        return raft;
    }
}
