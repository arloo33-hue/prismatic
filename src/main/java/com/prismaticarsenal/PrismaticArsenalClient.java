package com.prismaticarsenal;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;

public class PrismaticArsenalClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Thrown/fired weapons render as the 3D item model of the exact item that was fired
        EntityRendererRegistry.register(ModContent.THROWN_TOOL, ctx -> new FlyingItemEntityRenderer<>(ctx, 1.75f, false));
    }
}
