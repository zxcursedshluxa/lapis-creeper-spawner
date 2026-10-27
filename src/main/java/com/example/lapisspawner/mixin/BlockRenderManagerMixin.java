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

@Mixin(WorldRenderer.class)
public class BlockRenderManagerMixin {

    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private EntityRenderDispatcher entityRenderDispatcher;

    @Unique private CreeperEntity clientCreeper;
    @Unique private CreeperEntityRenderState creeperState;

    @SuppressWarnings("unchecked")
    @Inject(
        method = "renderEntities", 
        at = @At("TAIL") // Внедряемся в самый конец метода отрисовки сущностей
    )
    private void onRenderEntities(
        MatrixStack matrices, 
        Camera camera, 
        RenderTickCounter tickCounter, 
        CallbackInfo ci
    ) {
        if (this.client.world == null) return;

        // Инициализируем сущность крипера и её стейт
        if (this.clientCreeper == null) {
            this.clientCreeper = new CreeperEntity(EntityType.CREEPER, this.client.world);
            EntityRenderer<CreeperEntity, CreeperEntityRenderState> renderer = 
                (EntityRenderer<CreeperEntity, CreeperEntityRenderState>) this.entityRenderDispatcher.getRenderer(this.clientCreeper);
            this.creeperState = renderer.createRenderState();
        }

        // Обновляем состояние анимаций крипера
        float tickDelta = tickCounter.getTickDelta(true);
        EntityRenderer<CreeperEntity, CreeperEntityRenderState> renderer = 
            (EntityRenderer<CreeperEntity, CreeperEntityRenderState>) this.entityRenderDispatcher.getRenderer(this.clientCreeper);
        renderer.updateRenderState(this.clientCreeper, this.creeperState, tickDelta);

        // Получаем позицию камеры и буферы отрисовки
        Vec3d cameraPos = camera.getPos();
        VertexConsumerProvider.Immediate consumers = this.client.getBufferBuilders().getEntityVertexConsumers();

        BlockPos playerPos = BlockPos.ofFloored(cameraPos);
        int radius = 48; // Радиус поиска стекол вокруг игрока

        for (BlockPos pos : BlockPos.iterate(
                playerPos.add(-radius, -12, -radius), 
                playerPos.add(radius, 12, radius)
        )) {
            BlockState state = this.client.world.getBlockState(pos);
            
            if (state.isOf(Blocks.TINTED_GLASS)) {
                matrices.push();
                
                // Рассчитываем координаты блока относительно камеры
                double x = pos.getX() - cameraPos.x;
                double y = pos.getY() - cameraPos.y;
                double z = pos.getZ() - cameraPos.z;
                matrices.translate(x + 0.5, y + 0.25, z + 0.5);

                // Вращение крипера (анимация спавнера)
                long time = this.client.world.getTime();
                float rotation = ((time + tickDelta) * 10.0f) % 360.0f;
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));

                // Масштабирование моба, чтобы он помещался в блок
                matrices.scale(0.4375f, 0.4375f, 0.4375f);

                // Получаем освещение в точке нахождения блока
                int light = WorldRenderer.getLightmapCoordinates(this.client.world, pos);

                // Отрисовываем крипера
                renderer.render(this.creeperState, matrices, consumers, light);

                matrices.pop();
            }
        }
    }
}
