package com.example.lapisspawner.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.CreeperEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(WorldRenderer.class)
public class BlockRenderManagerMixin {

    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private EntityRenderDispatcher entityRenderDispatcher;

    @Unique private CreeperEntity clientCreeper;
    @Unique private CreeperEntityRenderState creeperState;

    @SuppressWarnings("unchecked")
    @Inject(
        method = "renderEntities", 
        at = @At("TAIL")
    )
    private void onRenderEntities(
        MatrixStack matrices,
        VertexConsumerProvider.Immediate immediateConsumers,
        Camera camera,
        RenderTickCounter tickCounter,
        List<?> entities,
        CallbackInfo ci
    ) {
        if (this.client.world == null) return;

        if (this.clientCreeper == null) {
            this.clientCreeper = new CreeperEntity(EntityType.CREEPER, this.client.world);
            EntityRenderer<CreeperEntity, CreeperEntityRenderState> renderer = 
                (EntityRenderer<CreeperEntity, CreeperEntityRenderState>) this.entityRenderDispatcher.getRenderer(this.clientCreeper);
            this.creeperState = renderer.createRenderState();
        }

        float tickDelta = tickCounter.getTickDelta(true);
        EntityRenderer<CreeperEntity, CreeperEntityRenderState> renderer = 
            (EntityRenderer<CreeperEntity, CreeperEntityRenderState>) this.entityRenderDispatcher.getRenderer(this.clientCreeper);
        renderer.updateRenderState(this.clientCreeper, this.creeperState, tickDelta);

        Vec3d cameraPos = camera.getPos();
        BlockPos playerPos = BlockPos.ofFloored(cameraPos);
        int radius = 32; 

        for (BlockPos pos : BlockPos.iterate(
                playerPos.add(-radius, -12, -radius), 
                playerPos.add(radius, 12, radius)
        )) {
            BlockState state = this.client.world.getBlockState(pos);
            
            if (state.isOf(Blocks.TINTED_GLASS)) {
                matrices.push();
                
                double x = pos.getX() - cameraPos.x;
                double y = pos.getY() - cameraPos.y;
                double z = pos.getZ() - cameraPos.z;
                
                // 1. Позиционируем в центре блока
                matrices.translate(x + 0.5, y + 0.4, z + 0.5);

                // 2. ВРАЩЕНИЕ: Сначала крутим моба вокруг вертикальной оси Y.
                // Так ось вращения останется строго вертикальной, и он будет стоять на лапах.
                long time = this.client.world.getTime();
                float rotation = ((time + tickDelta) * 45.0f) % 360.0f;
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));

                // 3. НАКЛОН: Наклоняем саму модельку на 20 градусов, чтобы создать покачивание при вращении.
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(20.0f));

                // 4. МАСШТАБ: Оптимальный размер
                matrices.scale(0.33f, 0.33f, 0.33f);

                // 5. КОРРЕКЦИЯ ЦЕНТРА: Опускаем модельку, смещая её точку привязки (ноги) вниз,
                // чтобы центр вращения пришелся ровно на туловище крипера.
                matrices.translate(0.0, -0.75, 0.0);

                int light = WorldRenderer.getLightmapCoordinates(this.client.world, pos.up());

                renderer.render(this.creeperState, matrices, immediateConsumers, light);

                matrices.pop();
            }
        }
    }
}
