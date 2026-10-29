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
import java.util.Random;

public class FakeCreeper {
    private final CreeperEntity creeper;
    private final ClientWorld world;
    private int fuseTimer = 0;
    private static final int MAX_FUSE = 30;
    private boolean isDead = false;
    
    private int hurtTime = 0;
    private double lastY;
    private boolean wasOnGround = true;

    // Переменные для случайного блуждания ("ходят когда хотят")
    private final Random random = new Random();
    private Vec3d wanderTarget = null;
    private int wanderTimer = 0;

    public FakeCreeper(ClientWorld world, Vec3d spawnPos) {
        this.world = world;
        this.creeper = new CreeperEntity(EntityType.CREEPER, world);
        
        this.creeper.setPosition(spawnPos.x, spawnPos.y + 0.5, spawnPos.z);
        this.lastY = creeper.getY();
        
        makeCharged(this.creeper);
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

        if (hurtTime > 0) {
            hurtTime--;
        }
        creeper.hurtTime = hurtTime;

        // Ванильный тик клиента (гравитация, анимации ног)
        creeper.baseTick();

        // Физика воды (плавание)
        if (creeper.isTouchingWater()) {
            Vec3d vel = creeper.getVelocity();
            creeper.setVelocity(vel.x, Math.max(vel.y, 0.06), vel.z);
            creeper.setJumping(true);
        } else {
            creeper.setJumping(false);
        }

        // Урон от падения
        if (!wasOnGround && creeper.isOnGround()) {
            double fallDistance = lastY - creeper.getY();
            if (fallDistance > 3.0) {
                triggerHurtVisual();
                world.playSound(creeper.getX(), creeper.getY(), creeper.getZ(), 
                    SoundEvents.ENTITY_GENERIC_SMALL_FALL, SoundCategory.HOSTILE, 0.7F, 1.0F, false);
            }
        }
        wasOnGround = creeper.isOnGround();
        if (creeper.isOnGround()) {
            lastY = creeper.getY();
        }

        // --- ЛОГИКА ДВИЖЕНИЯ: БЛУЖДАНИЕ ИЛИ ПРЕСЛЕДОВАНИЕ ---
        Vec3d pos = creeper.getPos();
        double distanceToPlayerSq = pos.squaredDistanceTo(targetPos);

        Vec3d moveTarget;
        double speed;

        // Если игрок ближе 10 блоков — крипер агрессивно бежит за ним
        if (distanceToPlayerSq < 100.0) {
            moveTarget = targetPos;
            speed = creeper.isTouchingWater() ? 0.05 : 0.09;
        } else {
            // Если игрок далеко — крипер "ходит когда хочет" (случайное блуждание)
            if (wanderTarget == null || wanderTimer <= 0 || pos.squaredDistanceTo(wanderTarget) < 2.0) {
                // Выбираем новую случайную точку в радиусе 4 блоков
                double rx = (random.nextDouble() - 0.5) * 8.0;
                double rz = (random.nextDouble() - 0.5) * 8.0;
                wanderTarget = new Vec3d(pos.x + rx, pos.y, pos.z + rz);
                wanderTimer = 80 + random.nextInt(100); // Идет к ней от 4 до 9 секунд, потом думает
            }
            wanderTimer--;
            moveTarget = wanderTarget;
            speed = 0.04; // Медленная скорость вальяжной ходьбы
        }

        // Движение к выбранной цели (игроку или случайной точке)
        Vec3d direction = moveTarget.subtract(pos);
        double horizontalDistSq = direction.horizontalLengthSquared();

        if (horizontalDistSq > 0.1) {
            direction = direction.normalize().multiply(speed);
            creeper.setVelocity(direction.x, creeper.getVelocity().y, direction.z);
            
            // Поворот лица
            double dx = moveTarget.x - pos.x;
            double dz = moveTarget.z - pos.z;
            float yaw = (float) (Math.atan2(dz, dx) * (180 / Math.PI)) - 90.0F;
            creeper.setYaw(yaw);
            creeper.headYaw = yaw;
        }

        creeper.move(net.minecraft.entity.MovementType.SELF, creeper.getVelocity());

        // Логика взрыва при сближении с игроком
        if (distanceToPlayerSq <= 6.25) {
            fuseTimer++;
            if (fuseTimer % 10 == 0) {
                triggerHurtVisual();
            }
            if (fuseTimer >= MAX_FUSE) {
                explode();
            }
        } else {
            if (fuseTimer > 0) fuseTimer--;
        }
    }

    public void triggerHurtVisual() {
        this.hurtTime = 10;
        creeper.hurtTime = 10;
        world.playSound(creeper.getX(), creeper.getY(), creeper.getZ(), 
            SoundEvents.ENTITY_CREEPER_HURT, SoundCategory.HOSTILE, 0.5F, 1.0F, false);
    }

    private void explode() {
        if (isDead) return;
        isDead = true;

        Vec3d pos = creeper.getPos();
        world.playSound(pos.x, pos.y, pos.z, SoundEvents.ENTITY_CREEPER_PRIMED, SoundCategory.HOSTILE, 1.0F, 0.5F, false);
        world.playSound(pos.x, pos.y, pos.z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 1.0F, 1.0F, false);

        for (int i = 0; i < 40; i++) {
            double ox = (world.random.nextDouble() - 0.5) * 2.0;
            double oy = (world.random.nextDouble() - 0.5) * 2.0;
            double oz = (world.random.nextDouble() - 0.5) * 2.0;
            world.addParticle(ParticleTypes.EXPLOSION, pos.x + ox, pos.y + oy + 1.0, pos.z + oz, 0.0, 0.0, 0.0);
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
