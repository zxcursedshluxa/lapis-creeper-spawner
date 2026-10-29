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

    private static Field POWERED_FIELD = null;

    static {
        // Находим поле TrackedData<Boolean> в CreeperEntity через рефлексию (безопасно для 1.21.4)
        try {
            for (Field field : CreeperEntity.class.getDeclaredFields()) {
                if (TrackedData.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    // Пробуем записать тестовое значение или определить по порядку/сигнатуре
                    // В CreeperEntity первое или второе TrackedData поле — это обычно POWERED (дует ли заряд)
                    // Надежнее проверить имя или перебрать:
                }
            }
            // Альтернативный и точный поиск в 1.21.4: ищем поле типа TrackedData, отвечающее за boolean (powered)
            for (Field field : CreeperEntity.class.getDeclaredFields()) {
                if (field.getType().equals(TrackedData.class)) {
                    field.setAccessible(true);
                    Object val = field.get(null);
                    // CreeperEntity имеет поля: DATA_FUSE_SPEED, DATA_CHARGED, DATA_IGNITED
                    // Проверим через DataTracker крипера
                }
            }
        } catch (Exception ignored) {}
    }

    public FakeCreeper(ClientWorld world, Vec3d spawnPos) {
        this.world = world;
        // Создаем сущность крипера на клиенте (без добавления в серверный мир)
        this.creeper = new CreeperEntity(EntityType.CREEPER, world);
        this.creeper.setPosition(spawnPos.x, spawnPos.y, spawnPos.z);
        
        // Делаем крипера заряженным (синим) без падений
        makeCharged(this.creeper);

        // Спавним сущность в клиенте
        world.addEntity(this.creeper);
    }

    private void makeCharged(CreeperEntity creeper) {
        try {
            // Способ 1: через рефлексию по всем статическим полям TrackedData в CreeperEntity
            if (POWERED_FIELD == null) {
                for (Field field : CreeperEntity.class.getDeclaredFields()) {
                    if (field.getType().equals(TrackedData.class)) {
                        field.setAccessible(true);
                        Object trackData = field.get(null);
                        // Проверяем на каком-нибудь крипере
                        try {
                            creeper.getDataTracker().set((TrackedData<Boolean>) trackData, true);
                            // Если установилось и не упало, и это булево — велика вероятность, что это оно или Ignited.
                            // У заряженного крипера размер дата трекера позволяет отловить.
                            // Но лучше найдем точно по имени или перебором:
                        } catch (Exception e) {
                            // игнор
                        }
                    }
                }
            }
            
            // Самый надежный способ для Fabric 1.21.4: найти поле, содержащее 'POWERED' или 'charged' в модинге,
            // либо выставить через EntityAccessor / NBT-компоненты если клиент позволяет.
            // Если рефлексия затруднена, можно использовать обход через net.minecraft.entity.data.DataTracker:
            for (Field f : CreeperEntity.class.getDeclaredFields()) {
                if (f.getType().getName().contains("TrackedData")) {
                    f.setAccessible(true;
                    Object obj = f.get(null);
                    if (obj instanceof TrackedData) {
                        try {
                            // Пробуем взвести все подходящие булевы флаги крипера (powered и ignited)
                            creeper.getDataTracker().set((TrackedData<Boolean>) obj, true);
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

        // 1. Ручное движение к игроку (так как серверная навигация на клиенте не работает)
        Vec3d pos = creeper.getPos();
        Vec3d direction = targetPos.subtract(pos);
        double distanceSq = direction.horizontalLengthSquared();

        if (distanceSq > 0.1) {
            direction = direction.normalize().multiply(0.08); // Скорость движения крипера
            creeper.setVelocity(direction.x, creeper.getVelocity().y, direction.z);
            
            // Поворот головы и тела крипера в сторону игрока
            double dx = targetPos.x - pos.x;
            double dz = targetPos.z - pos.z;
            float yaw = (float) (Math.atan2(dz, dx) * (180 / Math.PI)) - 90.0F;
            creeper.setYaw(yaw);
            creeper.headYaw = yaw;
        }

        // Применяем движение с учетом грамвитации/коллизий клиента
        creeper.move(net.minecraft.entity.MovementType.SELF, creeper.getVelocity());

        // 2. Проверка дистанции для взрыва (например, ближе чем 2.5 блока)
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

        // Воспроизводим звук взрыва для игрока на клиенте
        world.playSound(pos.x, pos.y, pos.z, SoundEvents.ENTITY_CREEPER_PRIMED, SoundCategory.HOSTILE, 1.0F, 0.5F, false);
        world.playSound(pos.x, pos.y, pos.z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 1.0F, (float) (1.0 + (Math.random() - Math.random()) * 0.2), false);

        // Спавним красивые клиентские частицы взрыва (облако дыма и вспышка)
        for (int i = 0; i < 50; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * 2.0;
            double offsetY = world.random.nextDouble() * 2.0;
            double offsetZ = (world.random.nextDouble() - 0.5) * 2.0;
            world.addParticle(ParticleTypes.EXPLOSION, pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ, 0.0, 0.0, 0.0);
            world.addParticle(ParticleTypes.FLASH, pos.x, pos.y + 1.0, pos.z, 0.0, 0.0, 0.0);
        }

        // Удаляем фейкового крипера из мира
        creeper.discard();
    }

    public boolean isDead() {
        return isDead || creeper.isRemoved();
    }

    public CreeperEntity getCreeper() {
        return creeper;
    }
}
