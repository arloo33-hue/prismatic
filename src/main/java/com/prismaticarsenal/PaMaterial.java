package com.prismaticarsenal;

/** Generated from the add-on data: every custom ore, its tier, where it spawns, its on-hit ability and full-set bonus. */
public enum PaMaterial {
    TINSTONE("tinstone", "Tinstone", 1, "overworld", "knockback", "haste:0"),
    COBALTITE("cobaltite", "Cobaltite", 2, "overworld", "slow", "speed:0"),
    VERDITE("verdite", "Verdite", 2, "overworld", "regen", "regeneration:0"),
    SUNSTEEL("sunsteel", "Sunsteel", 3, "overworld", "burn", "fire_resistance:0"),
    FROSTIUM("frostium", "Frostium", 3, "overworld", "freeze", "resistance:0"),
    RUBELLITE("rubellite", "Rubellite", 4, "overworld", "lifesteal", "health_boost:0"),
    AZURITE("azurite", "Azurite", 4, "overworld", "launch", "water_breathing:0"),
    THUNDRITE("thundrite", "Thundrite", 5, "overworld", "lightning", "jump_boost:1"),
    BLOODSTONE("bloodstone", "Bloodstone", 5, "nether", "drain", "strength:0"),
    MOONSILVER("moonsilver", "Moonsilver", 5, "overworld", "lunar", "night_vision:0"),
    VENOMITE("venomite", "Venomite", 6, "overworld", "poison", "regeneration:0"),
    EMBERITE("emberite", "Emberite", 6, "nether", "blast", "fire_resistance:0"),
    GRAVITITE("gravitite", "Gravitite", 6, "overworld", "gravity", "slow_falling:0"),
    VOIDSTONE("voidstone", "Voidstone", 7, "end", "void", "invisibility:0"),
    AETHERIUM("aetherium", "Aetherium", 7, "overworld", "aether", "speed:1"),
    SOLARIUM("solarium", "Solarium", 8, "nether", "solar", "fire_resistance:0,strength:0"),
    CELESTINE("celestine", "Celestine", 8, "end", "celestial", "resistance:1"),
    CHRONITE("chronite", "Chronite", 9, "end", "chrono", "haste:2"),
    DRAGONITE("dragonite", "Dragonite", 9, "end", "dragon", "strength:1"),
    ECLIPSIUM("eclipsium", "Eclipsium", 10, "end", "eclipse", "strength:1,night_vision:0"),
    PRISMATIC("prismatic", "Prismatic", 11, "end", "prism", "strength:2,resistance:1,regeneration:0");

    public final String key, displayName, dim, ability, bonus;
    public final int tier;

    PaMaterial(String key, String displayName, int tier, String dim, String ability, String bonus) {
        this.key = key;
        this.displayName = displayName;
        this.tier = tier;
        this.dim = dim;
        this.ability = ability;
        this.bonus = bonus;
    }
}
