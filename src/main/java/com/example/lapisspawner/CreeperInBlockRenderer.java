package com.example.lapisspawner;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.util.math.RotationAxis;

public final class CreeperInBlockRenderer {
    private static CreeperEntity creeper;

    public static void render(MatrixStack matrices, VertexConsumer vc,
                              float tickDelta, int light) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        if (creeper == null) {
            creeper = EntityType.CREEPER.create(mc.world, SpawnReason.MOB_SUMMONED);
            if (creeper == null) return;
        }

        // Создаем провайдер, который перенаправляет отрисовку в наш VertexConsumer
        VertexConsumerProvider provider = new VertexConsumerProvider() {
            @Override
            public VertexConsumer getBuffer(RenderLayer layer) {
                return vc;
            }
        };

        matrices.push();
        matrices.translate(0.5, 0.0, 0.5);
        float angle = (mc.world.getTime() + tickDelta) * 10.0f;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
        matrices.scale(0.5f, 0.5f, 0.5f);

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        // Теперь передаем наш провайдер
        dispatcher.render(creeper, 0, 0, 0, 0f, matrices, provider, light);

        matrices.pop();
    }

    private CreeperInBlockRenderer() {}
}
