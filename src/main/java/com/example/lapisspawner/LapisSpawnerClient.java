package com.example.lapisspawner;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LapisSpawnerClient implements ClientModInitializer {

    // Пример координат твоего блока-спавнера (замени на реальные координаты или список твоих блоков)
    private static final BlockPos SPAWNER_POS = new BlockPos(0, 64, 0); // <--- Укажи координаты своего блока тонированного стекла

    private int timer = 0;
    private final List<FakeCreeper> activeCreepers = new ArrayList<>();

    @Override
    onInitializeClient() { // Важно: в Fabric это onInitializeClient()
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null || client.player == null) return;

            // 1. Проверяем расстояние от игрока до блока (15 блоков радиус)
            double distanceSq = client.player.squaredDistanceTo(
                SPAWNER_POS.getX() + 0.5,
                SPAWNER_POS.getY() + 0.5,
                SPAWNER_POS.getZ() + 0.5
            );

            // Если игрок в радиусе 15 блоков (15 * 15 = 225)
            if (distanceSq <= 225.0) {
                timer++;
                // Каждые 10 секунд (20 тиков * 10 = 200 тиков)
                if (timer >= 200) {
                    timer = 0;
                    spawnCreepersAround(client);
                }
            } else {
                // Если игрок отошел на 16+ блоков (16 * 16 = 256) — можно сбрасывать таймер или останавливать
                if (distanceSq > 256.0) {
                    timer = 0;
                }
            }

            // 2. Обновляем всех активных фейковых криперов каждый тик
            Vec3d playerPos = client.player.getPos();
            Iterator<FakeCreeper> iterator = activeCreepers.iterator();
            while (iterator.hasNext()) {
                FakeCreeper fakeCreeper = iterator.next();
                if (fakeCreeper.isDead()) {
                    iterator.remove();
                } else {
                    fakeCreeper.tick(playerPos);
                }
            }
        });
    }

    private void processSpawnCreepersAround(MinecraftClient client) {
        if (client.world == null || client.player == null) return;

        Vec3d blockCenter = new Vec3d(
            SPAWNER_POS.getX() + 0.5,
            SPAWNER_POS.getY(),
            SPAWNER_POS.getZ() + 0.5
        );

        // Получаем направление взгляда игрока, чтобы корректно спавнить слева, справа и сзади
        Vec3d rotationVector = client.player.getRotationVector();
        Vec3d forward = new Vec3d(rotationVector.x, 0, rotationVector.z).normalize();
        Vec3d right = new Vec3d(-forward.z, 0, forward.x); // Справа

        // Позиции для 3 криперов относительно блока или игрока:
        // 1. Слева (-right)
        // 2. Справа (+right)
        // 3. Сзади (-forward)
        Vec3d posLeft = blockCenter.add(right.multiply(-2.0));
        Vec3d posRight = blockCenter.add(right.multiply(2.0));
        Vec3d posBack = blockCenter.add(forward.multiply(-2.0));

        // Спавним трех фейковых заряженных криперов
        activeCreepers.add(new FakeCreeper(client.world, posLeft));
        activeCreepers.add(new FakeCreeper(client.world, posRight));
        activeCreepers.add(new FakeCreeper(client.world, posBack));
    }
}
