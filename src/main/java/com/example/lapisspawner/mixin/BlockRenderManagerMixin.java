package com.example.mixin;

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
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private EntityRenderDispatcher entityRenderDispatcher;

    @Unique private CreeperEntity clientCreeper;
    @Unique private CreeperEntityRenderState creeperState;

    @Inject(
        method = "render", 
        at = @At(
            value = "INVOKE", 
            target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
        )
    )
    private void onRenderEntities(
        RenderTickCounter tickCounter, 
        boolean renderBlockOutline, 
        Camera camera, 
        GameRenderer gameRenderer, 
        LightmapTextureManager lightmapTextureManager, 
        Matrix4f matrix4f, 
        Matrix4f matrix4f2, 
        CallbackInfo ci
    ) {
        if (this.client.world == null) return;

        // Инициализируем сущность и её состояние один раз на главном потоке
        if (this.clientCreeper == null) {
            this.clientCreeper = new CreeperEntity(EntityType.CREEPER, this.client.world);
            
            // В 1.21.4 получаем правильный объект состояния через рендерер диспетчера
            EntityRenderer<CreeperEntity, CreeperEntityRenderState> renderer = 
                (EntityRenderer<CreeperEntity, CreeperEntityRenderState>) this.entityRenderDispatcher.getRenderer(this.clientCreeper);
            this.creeperState = renderer.createRenderState();
        }

        // Обновляем состояние крипера (анимации, текстуры) для текущего кадра
        float tickDelta = tickCounter.getTickDelta(true);
        EntityRenderer<CreeperEntity, CreeperEntityRenderState> renderer = 
            (EntityRenderer<CreeperEntity, CreeperEntityRenderState>) this.entityRenderDispatcher.getRenderer(this.clientCreeper);
        renderer.updateRenderState(this.clientCreeper, this.creeperState, tickDelta);

        // Получаем позицию камеры, чтобы правильно рассчитать относительные координаты блоков
        Vec3d cameraPos = camera.getPos();
        VertexConsumerProvider.Immediate consumers = this.client.getBufferBuilders().getEntityVertexConsumers();
        MatrixStack matrices = new MatrixStack();

        // Ищем блоки вокруг игрока в радиусе прорисовки (находимся строго в главном потоке отрисовки)
        BlockPos playerPos = BlockPos.ofFloored(cameraPos);
        int radius = 64; // Радиус отображения криперов в стекле

        for (BlockPos pos : BlockPos.iterate(
                playerPos.add(-radius, -16, -radius), 
                playerPos.add(radius, 16, radius)
        )) {
            BlockState state = this.client.world.getBlockState(pos);
            
            if (state.isOf(Blocks.TINTED_GLASS)) {
                matrices.push();
                
                // Вычисляем координаты блока относительно камеры
                double x = pos.getX() - cameraPos.x;
                double y = pos.getY() - cameraPos.y;
                double z = pos.getZ() - cameraPos.z;
                matrices.translate(x + 0.5, y + 0.25, z + 0.5);

                // Анимация вращения моба как в спавнере
                long time = this.client.world.getTime();
                float rotation = ((time + tickDelta) * 10.0f) % 360.0f;
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));

                // Масштаб моба внутри спавнера
                matrices.scale(0.4375f, 0.4375f, 0.4375f);

                // Получаем правильное освещение блока
                int light = WorldRenderer.getLightmapCoordinates(this.client.world, pos);

                // Отрисовываем крипера через официальный метод 1.21.4
                renderer.render(this.creeperState, matrices, consumers, light);

                matrices.pop();
            }
        }
    }
}
