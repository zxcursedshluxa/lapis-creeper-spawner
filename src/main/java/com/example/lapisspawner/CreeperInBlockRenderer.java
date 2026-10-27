package com.example.lapisspawner;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.util.math.RotationAxis;

public final class CreeperInBlockRenderer {
    private static CreeperEntity creeper;

    public static void render(MatrixStack matrices, VertexConsumerProvider vc,
                              float tickDelta, int light) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        if (creeper == null) {
            creeper = EntityType.CREEPER.create(mc.world);
            if (creeper == null) return;
        }

        matrices.push();
        matrices.translate(0.5, 0.0, 0.5);
        float angle = (mc.world.getTime() + tickDelta) * 10.0f;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
        matrices.scale(0.5f, 0.5f, 0.5f);

        EntityRenderDispatcher d = mc.getEntityRenderDispatcher();
        d.render(creeper, 0, 0, 0, 0f, tickDelta, matrices, vc, light);

        matrices.pop();
    }

    private CreeperInBlockRenderer() {}
}