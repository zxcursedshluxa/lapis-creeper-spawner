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

    // Укажи здесь координаты твоего блока тонированного стекла
    private static final BlockPos SPAWNER_POS = new BlockPos(0, 64, 0); 
    private static final int SPAWN_INTERVAL_TICKS = 200; // 10 секунд (20 тиков * 10)
    private static final double SPAWN_RADIUS_SQ = 15.0 * 15.0; // 15 блоков радиус
    private static final double DESPAWN_DISTANCE_SQ = 64.0 * 64.0;

    private int timer = 0;
    private final List<FakeCreeper> activeCreepers = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            // 1. Проверяем расстояние от игрока до спавнера (15 блоков)
            double distanceSq = client.player.squaredDistanceTo(
                SPAWNER_POS.getX() + 0.5,
                SPAWNER_POS.getY() + 0.5,
                SPAWNER_POS.getZ() + 0.5
            );

            if (distanceSq <= SPAWN_RADIUS_SQ) {
                timer++;
                if (timer >= SPAWN_INTERVAL_TICKS) {
                    timer = 0;
                    spawnCreepersAround(client);
                }
            } else {
                // Если игрок отошел далеко, сбрасываем таймер
                if (timer > 0) timer--;
            }

            // 2. Обновляем криперов каждый тик и удаляем тех, кто далеко или взорвался
            Vec3d playerPos = client.player.getPos();
            Iterator<FakeCreeper> iterator = activeCreepers.iterator();
            while (iterator.hasNext()) {
                FakeCreeper creeper = iterator.next();
                if (creeper.isDead()) {
                    iterator.remove();
                    continue;
                }

                creeper.tick(playerPos);

                // Если крипер отлетел слишком далеко от игрока — удаляем
                if (creeper.getPos().squaredDistanceTo(playerPos) > DESPAWN_DISTANCE_SQ) {
                    creeper.remove();
                    iterator.remove();
                }
            }
        });
    }

    private void spawnCreepersAround(MinecraftClient client) {
        if (client.world == null || client.player == null) return;

        Vec3d blockCenter = new Vec3d(
            SPAWNER_POS.getX() + 0.5,
            SPAWNER_POS.getY(),
            SPAWNER_POS.getZ() + 0.5
        );

        // Определяем стороны на основе взгляда игрока
        Vec3d rotationVector = client.player.getRotationVector();
        Vec3d forward = new Vec3d(rotationVector.x, 0, rotationVector.z).normalize();
        Vec3d right = new Vec3d(-forward.z, 0, forward.x);

        // Позиции: слева, справа, сзади от блока
        Vec3d posLeft = blockCenter.add(right.multiply(-2.0));
        Vec3d posRight = blockCenter.add(right.multiply(2.0));
        Vec3d posBack = blockCenter.add(forward.multiply(-2.0));

        // Добавляем их в список активных
        activeCreepers.add(new FakeCreeper(client.world, posLeft));
        activeCreepers.add(new FakeCreeper(client.world, posRight));
        activeCreepers.add(new FakeCreeper(client.world, posBack));
    }
}
