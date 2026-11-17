package com.prismaticarsenal;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/** A weapon/tool fired from the Longbow or thrown. Renders as the 3D model of the exact item. */
public class ThrownToolEntity extends ThrownItemEntity {
    public float damage = 4f;
    public String kind = "sword";
    public String ability = "";
    public int tier = 0;
    public int punch = 0;
    public boolean annihilate = false;   // Prismatic fireball: kills whatever it hits directly
    public float blast = 0f;
    private int life = 0;

    public ThrownToolEntity(EntityType<? extends ThrownToolEntity> type, World world) {
        super(type, world);
    }

    public ThrownToolEntity(World world, LivingEntity owner) {
        super(ModContent.THROWN_TOOL, owner, world);
    }

    public ThrownToolEntity configure(float damage, String kind, int tier, String ability) {
        this.damage = damage;
        this.kind = kind;
        this.tier = tier;
        this.ability = ability;
        return this;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.IRON_SWORD;
    }

    @Override
    protected float getGravity() {
        return 0.03f;
    }

    @Override
    public void tick() {
        super.tick();
        if (annihilate && getWorld().isClient) {
            for (int i = 0; i < 3; i++) getWorld().addParticle(ParticleTypes.FLAME, getX() + random.nextGaussian() * 0.3, getY() + random.nextGaussian() * 0.3, getZ() + random.nextGaussian() * 0.3, 0, 0, 0);
        }
        if (!getWorld().isClient && ++life > 200) discard();
    }

    @Override
    protected void onEntityHit(EntityHitResult hit) {
        super.onEntityHit(hit);
        if (getWorld().isClient || !(hit.getEntity() instanceof LivingEntity target)) return;
        Entity owner = getOwner();
        if (annihilate) {
            target.damage(getDamageSources().thrown(this, owner), 1.0E6f);
            if (target.isAlive()) target.kill();
            return;
        }
        target.timeUntilRegen = 0;
        target.damage(getDamageSources().thrown(this, owner), damage);
        if (isOnFire()) target.setOnFireFor(5);
        if (punch > 0) target.takeKnockback(0.8 * punch, -getVelocity().x, -getVelocity().z);
        if (owner instanceof LivingEntity attacker) {
            if (kind.equals("spear")) target.takeKnockback(1.6, -getVelocity().x, -getVelocity().z);
            if (kind.equals("mace")) {
                ((ServerWorld) getWorld()).spawnParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
                for (LivingEntity n : Abilities.nearby(target, attacker, 3.5)) Abilities.extra(n, attacker, damage * 0.4f);
            }
            Abilities.apply(ability, tier, false, target, attacker, 1f);
        }
    }

    @Override
    protected void onCollision(HitResult result) {
        super.onCollision(result);
        if (getWorld().isClient) return;
        if (blast > 0f) {
            getWorld().createExplosion(this, getX(), getY(), getZ(), blast, true, World.ExplosionSourceType.NONE);
            if (getOwner() instanceof LivingEntity a) {
                for (LivingEntity n : getWorld().getEntitiesByClass(LivingEntity.class, getBoundingBox().expand(3 + blast), e -> e != a)) {
                    Abilities.extra(n, a, 20f + 10f * blast);
                    n.setOnFireFor(8);
                }
            }
        }
        discard();
    }
}
