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
        // Жесткая проверка: если мир, клиент или рендерер не готовы, не делаем ничего
        if (this.client.world == null || this.entityRenderDispatcher == null || immediateConsumers == null) return;

        try {
            // Безопасная инициализация на главном потоке
            if (this.clientCreeper == null) {
                this.clientCreeper = new CreeperEntity(EntityType.CREEPER, this.client.world);
                EntityRenderer<CreeperEntity, CreeperEntityRenderState> renderer = 
                    (EntityRenderer<CreeperEntity, CreeperEntityRenderState>) this.entityRenderDispatcher.getRenderer(this.clientCreeper);
                if (renderer != null) {
                    this.creeperState = renderer.createRenderState();
                }
            }

            if (this.clientCreeper == null || this.creeperState == null) return;

            float tickDelta = tickCounter.getTickDelta(true);
            EntityRenderer<CreeperEntity, CreeperEntityRenderState> renderer = 
                (EntityRenderer<CreeperEntity, CreeperEntityRenderState>) this.entityRenderDispatcher.getRenderer(this.clientCreeper);
            
            if (renderer == null) return;
            renderer.updateRenderState(this.clientCreeper, this.creeperState, tickDelta);

            Vec3d cameraPos = camera.getPos();
            BlockPos playerPos = BlockPos.ofFloored(cameraPos);
            int radius = 32; 

            for (BlockPos pos : BlockPos.iterate(
                    playerPos.add(-radius, -12, -radius), 
                    playerPos.add(radius, 12, radius)
            )) {
                // Проверяем, что мы находимся в границах высоты мира, чтобы избежать выхода за лимиты
                if (pos.getY() >= this.client.world.getTopY() - 1 || pos.getY() < this.client.world.getBottomY()) continue;

                BlockState state = this.client.world.getBlockState(pos);
                
                if (state.isOf(Blocks.TINTED_GLASS)) {
                    matrices.push();
                    
                    double x = pos.getX() - cameraPos.x;
                    double y = pos.getY() - cameraPos.y;
                    double z = pos.getZ() - cameraPos.z;
                    
                    // 1. Позиционируем точно в центре блока спавнера
                    matrices.translate(x + 0.5, y + 0.45, z + 0.5);

                    // 2. ВРАЩЕНИЕ (Ось Y): Скорость вращения вокруг центра спавнера
                    long time = this.client.world.getTime();
                    float rotation = ((time + tickDelta) * 50.0f) % 360.0f;
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));

                    // 3. НАКЛОН (Ось X): Наклоняем модельку вперед на 15 градусов.
                    // Теперь наклон автоматически уводит голову наружу к стенкам решетки
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0f));

                    // 4. МАСШТАБ: Сделали его меньше (0.28f), чтобы он сидел супер аккуратно
                    matrices.scale(0.28f, 0.28f, 0.28f);

                    // 5. КОРРЕКЦИЯ ЦЕНТРА: Опускаем модельку так, чтобы центр вращения пришелся на туловище
                    matrices.translate(0.0, -0.75, 0.0);

                    int light = WorldRenderer.getLightmapCoordinates(this.client.world, pos.up());

                    // Безопасный вызов отрисовки
                    renderer.render(this.creeperState, matrices, immediateConsumers, light);

                    matrices.pop();
                }
            }
        } catch (Exception e) {
            // Если в процессе рендеринга произойдет любая ошибка, игра НЕ крашнется,
            // а просто пропустит один кадр отрисовки крипера
            System.err.println("[LapisSpawner] Ошибка отрисовки кадра: " + e.getMessage());
        }
    }
}
