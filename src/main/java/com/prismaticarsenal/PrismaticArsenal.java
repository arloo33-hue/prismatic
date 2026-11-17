package com.prismaticarsenal;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

public class PrismaticArsenal implements ModInitializer {
    public static final String MOD_ID = "prismatic_arsenal";

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModContent.register();
        ModEvents.register();
    }
}
