package com.prismaticarsenal;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ExperienceDroppingBlock;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.gen.GenerationStep;

import java.util.*;

public final class ModContent {
    private ModContent() {}

    public static final Map<PaMaterial, Item> INGOTS = new EnumMap<>(PaMaterial.class);
    public static final Map<PaMaterial, Block> ORES = new EnumMap<>(PaMaterial.class);
    public static final Map<PaMaterial, Map<String, Item>> GEAR = new EnumMap<>(PaMaterial.class);
    public static final Map<Item, AbilityInfo> ABILITY_OF = new HashMap<>();
    public static final Map<Item, PaMaterial> ARMOR_OF = new HashMap<>();
    private static final List<Item> TAB = new ArrayList<>();

    public static final EntityType<ThrownToolEntity> THROWN_TOOL = Registry.register(Registries.ENTITY_TYPE, PrismaticArsenal.id("thrown_tool"),
            FabricEntityTypeBuilder.<ThrownToolEntity>create(SpawnGroup.MISC, ThrownToolEntity::new)
                    .dimensions(EntityDimensions.fixed(0.4f, 0.4f)).trackRangeBlocks(64).trackedUpdateRate(10).build());

    private static <T extends Item> T item(String name, T item) {
        Registry.register(Registries.ITEM, PrismaticArsenal.id(name), item);
        TAB.add(item);
        return item;
    }

    private static Item.Settings settings(int tier) {
        return tier >= 9 ? new Item.Settings().fireproof() : new Item.Settings();
    }

    public static void register() {
        for (PaMaterial m : PaMaterial.values()) {
            boolean prism = m == PaMaterial.PRISMATIC;
            Block base = m.dim.equals("nether") ? Blocks.NETHERRACK : m.dim.equals("end") ? Blocks.END_STONE : Blocks.STONE;
            Block ore = new ExperienceDroppingBlock(FabricBlockSettings.copyOf(base).requiresTool()
                    .strength(prism ? 30f : 2f + m.tier * 0.4f, prism ? 1200f : 3f + m.tier),
                    prism ? UniformIntProvider.create(0, 0) : UniformIntProvider.create(1, 2 + m.tier / 2));
            Registry.register(Registries.BLOCK, PrismaticArsenal.id(m.key + "_ore"), ore);
            ORES.put(m, ore);
            item(m.key + "_ore", new BlockItem(ore, settings(m.tier)));
            if (prism) item("prismatic_shard", new Item(settings(m.tier)));
            else item("raw_" + m.key, new Item(new Item.Settings()));
            INGOTS.put(m, item(m.key + "_ingot", new Item(settings(m.tier))));

            ToolMaterial tm = PaTiers.tool(m);
            ArmorMaterial am = PaTiers.armor(m);
            Map<String, Item> g = new LinkedHashMap<>();
            g.put("helmet", new ArmorItem(am, ArmorItem.Type.HELMET, settings(m.tier)));
            g.put("chestplate", new ArmorItem(am, ArmorItem.Type.CHESTPLATE, settings(m.tier)));
            g.put("leggings", new ArmorItem(am, ArmorItem.Type.LEGGINGS, settings(m.tier)));
            g.put("boots", new ArmorItem(am, ArmorItem.Type.BOOTS, settings(m.tier)));
            g.put("sword", new PaItems.PaSword(tm, 3, -2.4f, settings(m.tier), prism));
            g.put("pickaxe", new PaItems.PaPickaxe(tm, 1, -2.8f, settings(m.tier)));
            g.put("axe", new PaItems.PaAxe(tm, 5f, -3.0f, settings(m.tier)));
            g.put("shovel", new PaItems.PaShovel(tm, 1.5f, -3.0f, settings(m.tier)));
            g.put("hoe", new PaItems.PaHoe(tm, 0, -1.0f, settings(m.tier)));
            g.put("spear", new PaItems.PaSpear(tm, 4, -2.8f, settings(m.tier)));
            g.put("mace", new PaItems.PaMace(tm, 6, -3.2f, settings(m.tier)));
            g.put("trident", new PaItems.PaTrident(settings(m.tier).maxDamage(tm.getDurability())));
            AbilityInfo info = new AbilityInfo(m.key, m.ability, m.tier);
            for (Map.Entry<String, Item> e : g.entrySet()) {
                item(m.key + "_" + e.getKey(), e.getValue());
                ABILITY_OF.put(e.getValue(), info);
                if (e.getValue() instanceof ArmorItem) ARMOR_OF.put(e.getValue(), m);
            }
            GEAR.put(m, g);

            BiomeModifications.addFeature(
                    m.dim.equals("nether") ? BiomeSelectors.foundInTheNether() : m.dim.equals("end") ? BiomeSelectors.foundInTheEnd() : BiomeSelectors.foundInOverworld(),
                    GenerationStep.Feature.UNDERGROUND_ORES,
                    RegistryKey.of(RegistryKeys.PLACED_FEATURE, PrismaticArsenal.id(m.key + "_ore")));
        }

        // 1.20.1 has no vanilla Mace, so the add-on adds a base Mace that the upgrades are built from
        item("mace", new PaItems.PaMace(ToolMaterials.IRON, 6, -3.2f, new Item.Settings()));
        for (VanillaMace v : VanillaMace.values()) {
            Item it = item(v.key + "_mace", new PaItems.PaMace(v.material, 6, -3.2f, settings(v.key.equals("netherite") ? 9 : 0)));
            ABILITY_OF.put(it, new AbilityInfo(v.key, v.ability, v.tier));
        }
        item("longbow", new PaItems.LongbowItem(new Item.Settings().maxDamage(1200)));

        Registry.register(Registries.ITEM_GROUP, PrismaticArsenal.id("main"), FabricItemGroup.builder()
                .icon(() -> new ItemStack(GEAR.get(PaMaterial.PRISMATIC).get("mace")))
                .displayName(Text.translatable("itemGroup.prismatic_arsenal"))
                .entries((ctx, entries) -> TAB.forEach(entries::add))
                .build());
    }
}
