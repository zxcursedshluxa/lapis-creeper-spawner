package com.example.lapisspawner.mixin;

import com.example.lapisspawner.CreeperInBlockRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderManager.class)
public class BlockRenderManagerMixin {

    @Inject(method = "renderBlock", at = @At("TAIL"))
    private void lapisSpawner$render(BlockState state, BlockPos pos, World world,
                                     MatrixStack matrices, VertexConsumerProvider vc,
                                     boolean cull, Random random, CallbackInfo ci) {
        if (!state.isOf(Blocks.LAPIS_BLOCK)) return;

        matrices.push();
        matrices.translate(pos.getX(), pos.getY(), pos.getZ());
        CreeperInBlockRenderer.render(matrices, vc,
                MinecraftClient.getInstance().getTickDelta(),
                0xF000F0);
        matrices.pop();
    }
}