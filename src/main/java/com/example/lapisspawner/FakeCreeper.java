package com.example.lapisspawner;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class FakeCreeper extends CreeperEntity {
    private int lifeTimer = 0;

    public FakeCreeper(EntityType<? extends CreeperEntity> entityType, World world) {
        super(entityType, world);
        this.setInvulnerable(true);
        this.setNoGravity(false);
        this.setCharged(true); // Заряженный (синий)
    }

    @Override
    public void tick() {
        super.baseTick(); // Только базовое обновление позиции и анимации
        
        if (this.getWorld().isClient) {
            this.lifeTimer++;
            MinecraftClient mc = MinecraftClient.getInstance();
            
            if (mc.player != null) {
                this.lookAtEntity(mc.player, 30.0F, 30.0F);
                
                Vec3d targetPos = mc.player.getPos();
                Vec3d currentPos = this.getPos();
                Vec3d direction = targetPos.subtract(currentPos).normalize();
                
                double speed = 0.25;
                this.setVelocity(direction.x * speed, this.getVelocity().y, direction.z * speed);
                this.move(net.minecraft.entity.MovementType.SELF, this.getVelocity());

                if (this.squaredDistanceTo(mc.player) < 4.0) {
                    explodeEffect();
                    this.discard();
                    return;
                }
            }

            if (this.lifeTimer > 200) {
                this.discard();
            }
        }
    }

    private void explodeEffect() {
        this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2.0f, 1.0f);
        
        for (int i = 0; i < 20; i++) {
            this.getWorld().addParticle(ParticleTypes.EXPLOSION, 
                    this.getX() + (this.random.nextDouble() - 0.5), 
                    this.getY() + this.random.nextDouble() * 2.0, 
                    this.getZ() + (this.random.nextDouble() - 0.5), 
                    0, 0, 0);
        }
    }
}
