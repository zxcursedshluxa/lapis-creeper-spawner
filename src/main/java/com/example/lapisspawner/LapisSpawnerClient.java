package com.example.lapisspawner;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

public class LapisSpawnerClient implements ClientModInitializer {
    // Таймер (10 секунд = 200 тиков)
    private int spawnTimer = 0;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            // Увеличиваем таймер каждый тик
            spawnTimer++;

            // Если прошло 10 секунд (200 тиков)
            if (spawnTimer >= 200) {
                spawnTimer = 0; // Сбрасываем таймер

                // Ищем тонированное стекло в радиусе 15 блоков
                BlockPos playerPos = client.player.getBlockPos();
                int radius = 15;
                BlockPos spawnerPos = null;

                // Ищем первый попавшийся блок тонированного стекла
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= radius; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            BlockPos checkPos = playerPos.add(x, y, z);
                            if (client.world.getBlockState(checkPos).isOf(Blocks.TINTED_GLASS)) {
                                spawnerPos = checkPos;
                                break;
                            }
                        }
                        if (spawnerPos != null) break;
                    }
                    if (spawnerPos != null) break;
                }

                // Если нашли блок — спавним 3 криперов вокруг него
                if (spawnerPos != null) {
                    // 1. Крипер слева (Север / -Z)
                    spawnCreeper(client, spawnerPos.getX() + 2.5, spawnerPos.getY() + 1.0, spawnerPos.getZ() - 2.5);
                    
                    // 2. Крипер справа (Юг / +Z)
                    spawnCreeper(client, spawnerPos.getX() - 2.5, spawnerPos.getY() + 1.0, spawnerPos.getZ() + 2.5);
                    
                    // 3. Крипер сзади (Запад / -X)
                    spawnCreeper(client, spawnerPos.getX() - 2.5, spawnerPos.getY() + 1.0, spawnerPos.getZ() - 2.5);
                    
                    // Воспроизводим звук спавна для каждого (или один общий)
                    client.world.playSound(client.player, spawnerPos, SoundEvents.ENTITY_CREEPER_PRIMED, SoundCategory.HOSTILE, 1.0f, 1.0f);
                }
            }
        });
    }

    // Вспомогательный метод для создания крипера
    private void spawnCreeper(MinecraftClient client, double x, double y, double z) {
        FakeCreeper fake = new FakeCreeper(EntityType.CREEPER, client.world);
        fake.setPosition(x, y, z);
        client.world.addEntity(fake);
    }
}
