package com.example.lapisspawner;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

public class FakeCreeper extends CreeperEntity {
    private int lifeTimer = 0;

    public FakeCreeper(EntityType<? extends CreeperEntity> entityType, World world) {
        super(entityType, world);
        this.setInvulnerable(true);
        this.setPowered(true); // Делаем его заряженным (синим)
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient) {
            lifeTimer++;
            MinecraftClient mc = MinecraftClient.getInstance();
            
            if (mc.player != null) {
                // Крипер бежит к игроку (если навигация работает)
                this.getNavigation().startMovingTo(mc.player, 1.2);
                
                // Если крипер подошел близко (меньше 2 блоков) — взрываем
                if (this.squaredDistanceTo(mc.player) < 4.0) {
                    this.getWorld().playSound(this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2.0f, 1.0f, false);
                    this.discard(); // Исчезает
                }
            }
            
            // Если крипер прожил больше 10 секунд (200 тиков) — он исчезает
            if (lifeTimer > 200) {
                this.discard();
            }
        }
    }
}
