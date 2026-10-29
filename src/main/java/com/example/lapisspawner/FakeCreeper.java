package com.example.lapisspawner;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Field;

public class FakeCreeper {
    private final CreeperEntity creeper;
    private final ClientWorld world;
    private int fuseTimer = 0;
    private static final int MAX_FUSE = 30; // 1.5 секунды до взрыва при сближении
    private boolean isDead = false;

    public FakeCreeper(ClientWorld world, Vec3d spawnPos) {
        this.world = world;
        this.creeper = new CreeperEntity(EntityType.CREEPER, world);
        this.creeper.setPosition(spawnPos.x, spawnPos.y, spawnPos.z);
        
        // Делаем крипера заряженным (синим) через рефлексию данных
        makeCharged(this.creeper);

        // Добавляем сущность в клиентский мир
        world.addEntity(this.creeper);
    }

    private void makeCharged(CreeperEntity targetCreeper) {
        try {
            for (Field field : CreeperEntity.class.getDeclaredFields()) {
                if (field.getType().equals(TrackedData.class)) {
                    field.setAccessible(true);
                    Object obj = field.get(null);
                    if (obj instanceof TrackedData) {
                        try {
                            // Взводим булевы флаги (включая заряженность)
                            targetCreeper.getDataTracker().set((TrackedData<Boolean>) obj, true);
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void tick(Vec3d targetPos) {
        if (isDead || creeper.isRemoved()) {
            isDead = true;
            return;
        }

        // Движение к игроку (setVelocity + move)
        Vec3d pos = creeper.getPos();
        Vec3d direction = targetPos.subtract(pos);
        double distanceSq = direction.horizontalLengthSquared();

        if (distanceSq > 0.1) {
            direction = direction.normalize().multiply(0.08); // Скорость бега
            creeper.setVelocity(direction.x, creeper.getVelocity().y, direction.z);
            
            // Поворот лица крипера к игроку
            double dx = targetPos.x - pos.x;
            double dz = targetPos.z - pos.z;
            float yaw = (float) (Math.atan2(dz, dx) * (180 / Math.PI)) - 90.0F;
            creeper.setYaw(yaw);
            creeper.headYaw = yaw;
        }

        creeper.move(net.minecraft.entity.MovementType.SELF, creeper.getVelocity());

        // Проверка дистанции для взрыва (радиус <= 2.5 блока)
        if (distanceSq <= 6.25) {
            fuseTimer++;
            if (fuseTimer >= MAX_FUSE) {
                explode();
            }
        } else {
            if (fuseTimer > 0) fuseTimer--;
        }
    }

    private void explode() {
        if (isDead) return;
        isDead = true;

        Vec3d pos = creeper.getPos();

        // Звуки и частицы взрыва
        world.playSound(pos.x, pos.y, pos.z, SoundEvents.ENTITY_CREEPER_PRIMED, SoundCategory.HOSTILE, 1.0F, 0.5F, false);
        world.playSound(pos.x, pos.y, pos.z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 1.0F, 1.0F, false);

        for (int i = 0; i < 40; i++) {
            double ox = (world.random.nextDouble() - 0.5) * 2.0;
            double oy = world.random.nextDouble() * 2.0;
            double oz = (world.random.nextDouble() - 0.5) * 2.0;
            world.addParticle(ParticleTypes.EXPLOSION, pos.x + ox, pos.y + oy, pos.z + oz, 0.0, 0.0, 0.0);
            world.addParticle(ParticleTypes.FLASH, pos.x, pos.y + 1.0, pos.z, 0.0, 0.0, 0.0);
        }

        creeper.discard();
    }

    public void remove() {
        if (!creeper.isRemoved()) {
            creeper.discard();
        }
        isDead = true;
    }

    public boolean isDead() {
        return isDead || creeper.isRemoved();
    }

    public Vec3d getPos() {
        return creeper.getPos();
    }
}
