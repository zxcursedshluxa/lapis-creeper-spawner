package com.example.lapisspawner;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.util.math.RotationAxis;

public final class CreeperInBlockRenderer {
    private static CreeperEntity creeper;
    private static EntityRenderState renderState;

    public static void render(MatrixStack matrices, VertexConsumerProvider vc,
                              float tickDelta, int light) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        if (creeper == null) {
            creeper = EntityType.CREEPER.create(mc.world, SpawnReason.MOB_SUMMONED);
            if (creeper == null) return;
        }

        matrices.push();
        matrices.translate(0.5, 0.0, 0.5);
        float angle = (mc.world.getTime() + tickDelta) * 10.0f;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
        matrices.scale(0.5f, 0.5f, 0.5f);

        EntityRenderDispatcher d = mc.getEntityRenderDispatcher();
        
        if (renderState == null) {
            renderState = d.getRenderer(creeper).createRenderState();
            renderState.light = light;
        }
        
        d.render(renderState, 0, 0, 0, matrices, vc, light);

        matrices.pop();
    }

    private CreeperInBlockRenderer() {}
}
