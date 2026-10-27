package com.example.lapisspawner;

import net.minecraft.block.Blocks;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

public final class CreeperInBlockRenderer {
    private static MobSpawnerBlockEntity fakeSpawner;

    public static void render(MatrixStack matrices, VertexConsumerProvider vc,
                              float tickDelta, int light) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        if (fakeSpawner == null) {
            fakeSpawner = new MobSpawnerBlockEntity(BlockPos.ORIGIN, Blocks.SPAWNER.getDefaultState());
            
            // В 1.21.4 NBT переехал в компоненты, но для спавнера работает старый метод setEntityId
            fakeSpawner.setEntityId(EntityType.CREEPER, mc.world, mc.world.getRandom(), BlockPos.ORIGIN);
        }

        // Крутим спавнер (его рендерер сам крутит моба)
        fakeSpawner.setWorld(mc.world);

        BlockEntityRenderDispatcher dispatcher = mc.getBlockEntityRenderDispatcher();
        var renderer = dispatcher.get(fakeSpawner);
        
        if (renderer != null) {
            renderer.render(fakeSpawner, tickDelta, matrices, vc, light, 0);
        }
    }

    private CreeperInBlockRenderer() {}
}
