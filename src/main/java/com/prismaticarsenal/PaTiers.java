package com.prismaticarsenal;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/** Tool + armor stats for each ore, scaled by tier. */
public final class PaTiers {
    private PaTiers() {}

    public static ToolMaterial tool(PaMaterial m) {
        return new ToolMaterial() {
            public int getDurability() { return 150 + m.tier * 220; }
            public float getMiningSpeedMultiplier() { return 4f + m.tier * 1.2f; }
            public float getAttackDamage() { return m.tier * 0.6f; }
            public int getMiningLevel() { return m.tier >= 8 ? 4 : m.tier >= 5 ? 3 : m.tier >= 3 ? 2 : 1; }
            public int getEnchantability() { return 10 + m.tier; }
            public Ingredient getRepairIngredient() { return Ingredient.ofItems(ModContent.INGOTS.get(m)); }
        };
    }

    public static ArmorMaterial armor(PaMaterial m) {
        return new ArmorMaterial() {
            public int getDurability(ArmorItem.Type type) {
                int base = switch (type) { case HELMET -> 11; case CHESTPLATE -> 16; case LEGGINGS -> 15; case BOOTS -> 13; };
                return base * (10 + m.tier * 3);
            }
            public int getProtection(ArmorItem.Type type) {
                int t = m.tier;
                return switch (type) {
                    case HELMET, BOOTS -> Math.min(5, 1 + Math.round(t * 0.22f));
                    case CHESTPLATE -> Math.min(10, 3 + Math.round(t * 0.55f));
                    case LEGGINGS -> Math.min(8, 2 + Math.round(t * 0.45f));
                };
            }
            public int getEnchantability() { return 10 + m.tier; }
            public SoundEvent getEquipSound() { return m.tier >= 8 ? SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE : SoundEvents.ITEM_ARMOR_EQUIP_IRON; }
            public Ingredient getRepairIngredient() { return Ingredient.ofItems(ModContent.INGOTS.get(m)); }
            // Texture: assets/minecraft/textures/models/armor/pa_<ore>_layer_1.png / _layer_2.png
            public String getName() { return "pa_" + m.key; }
            public float getToughness() { return m.tier >= 5 ? (m.tier - 4) * 0.5f : 0f; }
            public float getKnockbackResistance() { return m.tier >= 9 ? 0.1f : 0f; }
        };
    }
}
