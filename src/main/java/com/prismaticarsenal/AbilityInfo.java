package com.prismaticarsenal;

/** Which on-hit ability an item has and how strong it is (ore tier 1 to 11). */
public record AbilityInfo(String key, String ability, int tier) {
    public float power(boolean mace) {
        return (1f + tier / 3f) * (mace ? 1.8f : 1f);
    }
}
