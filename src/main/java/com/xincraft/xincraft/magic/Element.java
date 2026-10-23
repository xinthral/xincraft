package com.xincraft.xincraft.magic;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.StringRepresentable;

// The primal elements of the mod's nature magic. Each has a powder ground with the mortar and pestle
// (item.ElementPowderItem) and an elemental flora that stores its essence (block.FloraBlock), named
// with the element's Latin genitive (flora ignis, flora aquae, ...).
public enum Element implements StringRepresentable {
    TERRA("terra", "terrae", 0x8B6B3D),
    AQUA("aqua", "aquae", 0x3F76E4),
    IGNIS("ignis", "ignis", 0xE8531F),
    AER("aer", "aeris", 0xD8F0F5),
    VITA("vita", "vitae", 0x5FD35A),
    LUX("lux", "lucis", 0xFFE066),
    UMBRA("umbra", "umbrae", 0x4B2E6B);

    public static final StringRepresentable.EnumCodec<Element> CODEC = StringRepresentable.fromEnum(Element::values);

    private final String name;
    private final String genitive;
    private final int color;
    private final DustParticleOptions particle;

    Element(String name, String genitive, int color) {
        this.name = name;
        this.genitive = genitive;
        this.color = color;
        this.particle = new DustParticleOptions(color, 1.0F);
    }

    // Registry name of this element's powder (e.g. ignis_powder)
    public String powderId() {
        return name + "_powder";
    }

    // Registry name of this element's flora (e.g. flora_ignis)
    public String floraId() {
        return "flora_" + genitive;
    }

    public int color() {
        return color;
    }

    public DustParticleOptions particle() {
        return particle;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
