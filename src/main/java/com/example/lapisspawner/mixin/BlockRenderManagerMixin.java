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
                
                // 1. Позиционируем точно в центре блока спавнера
                matrices.translate(x + 0.5, y + 0.43, z + 0.5);

                // 2. ВРАЩЕНИЕ (Ось Y): Скорость вращения вокруг центра спавнера
                long time = this.client.world.getTime();
                float rotation = ((time + tickDelta) * 50.0f) % 360.0f;
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));

                // 3. СМЕНА ОСИ (РАЗВОРТ К СТЕНАМ): Разворачиваем крипера спиной к центру.
                // Благодаря этому наклон "вперед" сработает как наклон "наружу" (головой к прутьям)!
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f));

                // 4. НАКЛОН (Ось X): Наклоняем модельку вперед (теперь она отклоняется к стенам)
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(14.0f));

                // 5. ВАНИЛЬНОЕ СМЕЩЕНИЕ: Сдвигаем модельку, чтобы конус вращения был широким вверху
                matrices.translate(0.0, -0.4, 0.0);

                // 6. МАСШТАБ: Сделали аккуратный ванильный размер (0.31f) — чуть меньше предыдущего
                matrices.scale(0.31f, 0.31f, 0.31f);


            }
        }
    }
}
