package com.prismaticarsenal;

import net.minecraft.item.ToolMaterial;
import net.minecraft.item.ToolMaterials;

/** Mace upgrades made from vanilla materials. */
public enum VanillaMace {
    COPPER("copper", 2, "lightning", ToolMaterials.IRON),
    IRON("iron", 3, "knockback", ToolMaterials.IRON),
    GOLD("gold", 3, "regen", ToolMaterials.GOLD),
    AMETHYST("amethyst", 4, "slow", ToolMaterials.IRON),
    EMERALD("emerald", 5, "lifesteal", ToolMaterials.IRON),
    DIAMOND("diamond", 7, "launch", ToolMaterials.DIAMOND),
    NETHERITE("netherite", 9, "blast", ToolMaterials.NETHERITE);

    public final String key, ability;
    public final int tier;
    public final ToolMaterial material;

    VanillaMace(String key, int tier, String ability, ToolMaterial material) {
        this.key = key;
        this.tier = tier;
        this.ability = ability;
        this.material = material;
    }
}
