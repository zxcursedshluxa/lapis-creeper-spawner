package com.example.lapisspawner.mixin;

import com.example.lapisspawner.CreeperInBlockRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderManager.class)
public class BlockRenderManagerMixin {

    @Inject(method = "renderBlock", at = @At("TAIL"))
    private void lapisSpawner$render(BlockState state, BlockPos pos, BlockRenderView world,
                                     MatrixStack matrices, VertexConsumer vc,
                                     boolean cull, Random random, CallbackInfo ci) {
        if (!state.isOf(Blocks.LAPIS_BLOCK)) return;

        float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false);

        matrices.push();
        matrices.translate(pos.getX(), pos.getY(), pos.getZ());
        CreeperInBlockRenderer.render(matrices, vc, tickDelta, 0xF000F0);
        matrices.pop();
    }
}
