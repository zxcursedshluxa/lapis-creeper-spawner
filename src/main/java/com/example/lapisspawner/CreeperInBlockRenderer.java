package com.example.lapisspawner;

import net.minecraft.block.Blocks;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;

public final class CreeperInBlockRenderer {
    private static MobSpawnerBlockEntity fakeSpawner;

    public static void render(MatrixStack matrices, VertexConsumerProvider vc,
                              float tickDelta, int light) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        if (fakeSpawner == null) {
            fakeSpawner = new MobSpawnerBlockEntity(BlockPos.ORIGIN, Blocks.SPAWNER.getDefaultState());
            fakeSpawner.setWorld(mc.world);
        }

        BlockEntityRenderDispatcher dispatcher = mc.getBlockEntityRenderDispatcher();
        var renderer = dispatcher.get(fakeSpawner);
        
        if (renderer != null) {
            renderer.render(fakeSpawner, tickDelta, matrices, vc, light, 0);
        }
    }

    private CreeperInBlockRenderer() {}
}
