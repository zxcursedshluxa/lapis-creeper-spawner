package com.example.lapisspawner;

import net.minecraft.block.Blocks;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;

public final class CreeperInBlockRenderer {
    private static MobSpawnerBlockEntity fakeSpawner;

    public static void render(MatrixStack matrices, VertexConsumer vc,
                              float tickDelta, int light) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        if (fakeSpawner == null) {
            fakeSpawner = new MobSpawnerBlockEntity(BlockPos.ORIGIN, Blocks.SPAWNER.getDefaultState());
            fakeSpawner.setWorld(mc.world);
            
            // В 1.21.4 это должно заставить спавнер запомнить крипера
            fakeSpawner.getLogic().setEntityId(EntityType.CREEPER, mc.world, mc.world.getRandom(), BlockPos.ORIGIN);
        }

        VertexConsumerProvider provider = new VertexConsumerProvider() {
            @Override
            public VertexConsumer getBuffer(RenderLayer layer) {
                return vc;
            }
        };

        matrices.push();
        matrices.translate(0.5, 0.0, 0.5);
        
        BlockEntityRenderDispatcher dispatcher = mc.getBlockEntityRenderDispatcher();
        var renderer = dispatcher.get(fakeSpawner);

        if (renderer != null) {
            renderer.render(fakeSpawner, tickDelta, matrices, provider, light, 0);
        }

        matrices.pop();
    }

    private CreeperInBlockRenderer() {}
}
