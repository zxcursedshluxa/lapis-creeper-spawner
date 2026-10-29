package com.example.lapisspawner;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class LapisSpawnerClient implements ClientModInitializer {
    private int spawnCooldown = 0;
    private static final int TEN_SECONDS_TICKS = 200;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            World world = client.world;
            if (world == null || client.player == null) return;

            if (spawnCooldown > 0) {
                spawnCooldown--;
                return;
            }

            BlockPos playerPos = client.player.getBlockPos();
            int radius = 15;

            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos checkPos = playerPos.add(x, y, z);
                        
                        if (world.getBlockState(checkPos).isOf(Blocks.TINTED_GLASS)) {
                            
                            FakeCreeper fakeCreeper = new FakeCreeper(EntityType.CREEPER, world);
                            fakeCreeper.refreshPositionAndAngles(
                                    checkPos.getX() + 0.5, 
                                    checkPos.getY() + 1.0, 
                                    checkPos.getZ() + 0.5, 
                                    0, 0
                            );
                            
                            world.spawnEntity(fakeCreeper);
                            spawnCooldown = TEN_SECONDS_TICKS;
                            return;
                        }
                    }
                }
            }
        });
    }
}
