package com.prismaticarsenal;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.List;

public final class Abilities {
    private Abilities() {}

    // ---------- weapon types + damage ----------
    public static String kindOf(ItemStack s) {
        if (s == null || s.isEmpty()) return null;
        Item i = s.getItem();
        if (i instanceof PaItems.PaSpear) return "spear";
        if (i instanceof PaItems.PaMace) return "mace";
        if (i instanceof TridentItem) return "trident";
        if (i instanceof SwordItem) return "sword";
        if (i instanceof AxeItem) return "axe";
        if (i instanceof PickaxeItem) return "pickaxe";
        if (i instanceof ShovelItem) return "shovel";
        if (i instanceof HoeItem) return "hoe";
        return null;
    }

    public static float baseDamage(String kind) {
        return switch (kind) {
            case "spear" -> 24f; case "mace" -> 26f; case "trident" -> 25f; case "axe" -> 11f;
            case "pickaxe" -> 7f; case "shovel" -> 6f; case "hoe" -> 5f; default -> 9f;
        };
    }

    public static float tierMult(ItemStack s) {
        AbilityInfo info = ModContent.ABILITY_OF.get(s.getItem());
        if (info != null) return 0.8f + info.tier() * 0.1f;
        if (s.getItem() instanceof ToolItem t) {
            ToolMaterial m = t.getMaterial();
            if (m == ToolMaterials.WOOD) return 0.6f;
            if (m == ToolMaterials.STONE) return 0.75f;
            if (m == ToolMaterials.GOLD) return 0.8f;
            if (m == ToolMaterials.IRON) return 0.95f;
            if (m == ToolMaterials.DIAMOND) return 1.15f;
            if (m == ToolMaterials.NETHERITE) return 1.35f;
        }
        return 1f;
    }

    public static ThrownToolEntity launch(PlayerEntity p, ItemStack stack, float dmg, String kind, AbilityInfo info, float speed) {
        ThrownToolEntity e = new ThrownToolEntity(p.getWorld(), p);
        ItemStack copy = stack.copy();
        copy.setCount(1);
        e.setItem(copy);
        e.configure(dmg + EnchantmentHelper.getLevel(Enchantments.SHARPNESS, stack) * 1.25f, kind, info == null ? 0 : info.tier(), info == null ? "" : info.ability());
        e.setVelocity(p, p.getPitch(), p.getYaw(), 0f, speed, 0.5f);
        p.getWorld().spawnEntity(e);
        return e;
    }

    /** Vanilla-style mace bands, but damage per block and the strong band both grow with ore tier. */
    public static float smashDamage(float fall, int tier, ItemStack stack) {
        float k = 1f + tier * 0.18f;
        int band2 = 5 + tier;
        float dmg = Math.min(fall, 3f) * 4f * k;
        dmg += Math.min(Math.max(fall - 3f, 0f), band2) * 2f * k;
        dmg += Math.max(fall - 3f - band2, 0f) * (1f + tier * 0.1f) * k;
        return dmg;
    }

    // ---------- helpers ----------
    public static List<LivingEntity> nearby(LivingEntity target, LivingEntity attacker, double r) {
        return target.getWorld().getEntitiesByClass(LivingEntity.class, target.getBoundingBox().expand(r), e -> e != attacker && e != target && e.isAlive());
    }

    /** Extra damage on top of a hit (resets the hit cooldown so it always lands). */
    public static void extra(LivingEntity t, LivingEntity a, float amount) {
        if (amount <= 0f) return;
        t.timeUntilRegen = 0;
        t.damage(a instanceof PlayerEntity p ? a.getDamageSources().playerAttack(p) : a.getDamageSources().mobAttack(a), amount);
    }

    static void effect(LivingEntity e, StatusEffect type, float ticks, int amp) {
        e.addStatusEffect(new StatusEffectInstance(type, Math.round(ticks), Math.max(0, amp)));
    }

    static void lightning(World w, LivingEntity at) {
        LightningEntity l = EntityType.LIGHTNING_BOLT.create(w);
        if (l == null) return;
        l.refreshPositionAfterTeleport(at.getX(), at.getY(), at.getZ());
        w.spawnEntity(l);
    }

    static void particles(LivingEntity at, net.minecraft.particle.ParticleEffect type, int n) {
        if (at.getWorld() instanceof ServerWorld sw) sw.spawnParticles(type, at.getX(), at.getBodyY(0.5), at.getZ(), n, 0.4, 0.5, 0.4, 0.05);
    }

    // ---------- on-hit abilities ----------
    public static void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker, float mult) {
        if (attacker.getWorld().isClient) return;
        AbilityInfo info = ModContent.ABILITY_OF.get(stack.getItem());
        if (info == null) return;
        apply(info.ability(), info.tier(), stack.getItem() instanceof PaItems.PaMace, target, attacker, mult);
        if (info.key().equals("prismatic") && stack.getItem() instanceof PaItems.PaMace) {
            // Chain sparks to the 3 nearest enemies
            nearby(target, attacker, 8).stream().limit(3).forEach(n -> { extra(n, attacker, 10f); particles(n, ParticleTypes.ELECTRIC_SPARK, 12); });
        }
    }

    public static void apply(String ability, int tier, boolean mace, LivingEntity t, LivingEntity a, float mult) {
        if (ability == null || ability.isEmpty() || !t.isAlive()) return;
        World w = t.getWorld();
        float p = (1f + tier / 3f) * (mace ? 1.8f : 1f) * mult;
        switch (ability) {
            case "knockback" -> t.takeKnockback(0.6 * p, a.getX() - t.getX(), a.getZ() - t.getZ());
            case "slow" -> effect(t, StatusEffects.SLOWNESS, 60 * p, Math.min(4, (int) p));
            case "regen" -> effect(a, StatusEffects.REGENERATION, 40 * p, (int) (p / 2));
            case "burn" -> t.setOnFireFor(Math.round(3 * p));
            case "freeze" -> { effect(t, StatusEffects.SLOWNESS, 80 * p, 3); effect(t, StatusEffects.MINING_FATIGUE, 80 * p, 2); t.setFrozenTicks(Math.max(t.getFrozenTicks(), 160)); particles(t, ParticleTypes.SNOWFLAKE, 15); }
            case "lifesteal" -> a.heal(1.5f * p);
            case "launch" -> { t.addVelocity(0, 0.5 + 0.35 * p * 0.5, 0); t.velocityModified = true; }
            case "lightning" -> { if (w.random.nextFloat() < 0.18f * p) lightning(w, t); }
            case "drain" -> { effect(t, StatusEffects.WEAKNESS, 80 * p, 1); a.heal(1.5f * p); }
            case "lunar" -> { long time = w.getTimeOfDay() % 24000; if (time > 12500 && time < 23500) { extra(t, a, 3 * p); particles(t, ParticleTypes.END_ROD, 10); } }
            case "poison" -> effect(t, StatusEffects.POISON, 60 * p, Math.min(3, (int) p));
            case "blast" -> { particles(t, ParticleTypes.EXPLOSION, 3); for (LivingEntity n : nearby(t, a, 3 + p)) { extra(n, a, 2.5f * p); n.setOnFireFor(3); } }
            case "gravity" -> effect(t, StatusEffects.LEVITATION, 15 * p, 3);
            case "void" -> {
                effect(t, StatusEffects.BLINDNESS, 40 * p, 0);
                if (!(t instanceof PlayerEntity)) {
                    double ang = w.random.nextDouble() * Math.PI * 2, d = 3 + p * 2;
                    t.teleport(t.getX() + Math.cos(ang) * d, t.getY() + 1, t.getZ() + Math.sin(ang) * d, true);
                }
                particles(t, ParticleTypes.REVERSE_PORTAL, 20);
            }
            case "aether" -> { effect(a, StatusEffects.SPEED, 40 * p, 1); effect(a, StatusEffects.JUMP_BOOST, 40 * p, 1); }
            case "solar" -> { t.setOnFireFor(6); for (LivingEntity n : nearby(t, a, 2 + p)) n.setOnFireFor(Math.round(2 * p)); particles(t, ParticleTypes.LAVA, 6); }
            case "celestial" -> { a.heal(2 * p); effect(a, StatusEffects.RESISTANCE, 30 * p, 1); }
            case "chrono" -> { effect(t, StatusEffects.SLOWNESS, 40 * p, 6); effect(a, StatusEffects.HASTE, 60 * p, 2); effect(a, StatusEffects.SPEED, 60 * p, 1); }
            case "dragon" -> { particles(t, ParticleTypes.DRAGON_BREATH, 20); effect(t, StatusEffects.WITHER, 30 * p, 1); for (LivingEntity n : nearby(t, a, 2 + p)) { effect(n, StatusEffects.WITHER, 30 * p, 1); extra(n, a, p); } }
            case "eclipse" -> { effect(t, StatusEffects.WITHER, 50 * p, 2); effect(t, StatusEffects.DARKNESS, 50 * p, 0); }
            case "prism" -> {
                if (w.random.nextBoolean()) lightning(w, t);
                a.heal(p);
                particles(t, ParticleTypes.TOTEM_OF_UNDYING, 20);
                for (LivingEntity n : nearby(t, a, 4 + p / 2)) { extra(n, a, 1.5f * p); n.takeKnockback(1.2, t.getX() - n.getX(), t.getZ() - n.getZ()); }
            }
            default -> {}
        }
    }

    /** Prismatic Mace smash from 12+ blocks: one-shots the target and calls a ring of lightning. */
    public static void meteor(LivingEntity t, LivingEntity a) {
        World w = t.getWorld();
        extra(t, a, 1.0E6f);
        if (t.isAlive()) t.kill();
        for (int i = 0; i < 6; i++) {
            LightningEntity l = EntityType.LIGHTNING_BOLT.create(w);
            if (l == null) continue;
            double ang = i * Math.PI / 3;
            l.refreshPositionAfterTeleport(t.getX() + Math.cos(ang) * 5, t.getY(), t.getZ() + Math.sin(ang) * 5);
            w.spawnEntity(l);
        }
        if (a instanceof PlayerEntity p) p.sendMessage(net.minecraft.text.Text.literal("METEOR SMASH!"), true);
    }
}
