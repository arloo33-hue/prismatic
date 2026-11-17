package com.prismaticarsenal;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.*;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PaItems {
    private PaItems() {}

    static AbilityInfo info(ItemStack s) {
        return ModContent.ABILITY_OF.get(s.getItem());
    }

    static float charge(Item item, ItemStack stack, int remaining, float fullTicks) {
        return Math.min(1f, (item.getMaxUseTime(stack) - remaining) / fullTicks);
    }

    /** Swords. The Prismatic Sword charges a growing fireball (hold right click) that one-shots what it hits. */
    public static class PaSword extends SwordItem {
        private final boolean fireball;

        public PaSword(ToolMaterial m, int dmg, float speed, Settings s, boolean fireball) {
            super(m, dmg, speed, s);
            this.fireball = fireball;
        }

        @Override
        public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            Abilities.onHit(stack, target, attacker, 1f);
            return super.postHit(stack, target, attacker);
        }

        @Override
        public UseAction getUseAction(ItemStack stack) { return fireball ? UseAction.BOW : UseAction.NONE; }

        @Override
        public int getMaxUseTime(ItemStack stack) { return fireball ? 72000 : 0; }

        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            ItemStack stack = user.getStackInHand(hand);
            if (!fireball || hand != Hand.MAIN_HAND) return TypedActionResult.pass(stack);
            user.setCurrentHand(hand);
            return TypedActionResult.consume(stack);
        }

        @Override
        public void usageTick(World world, LivingEntity user, ItemStack stack, int remaining) {
            if (!fireball || !(world instanceof ServerWorld sw)) return;
            float c = charge(this, stack, remaining, 60f);
            Vec3d at = user.getEyePos().add(user.getRotationVector().multiply(1.6 + 1.4 * c));
            double r = (0.1 + 1.6 * c) * 0.5;
            sw.spawnParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 2 + (int) (c * 14), r, r, r, 0.01);
            if (c > 0.4f) sw.spawnParticles(ParticleTypes.LAVA, at.x, at.y, at.z, 1, 0.1, 0.1, 0.1, 0);
            if (user instanceof PlayerEntity p) p.sendMessage(Text.literal("Prismatic Fireball " + Math.round(c * 100) + "%" + (c >= 1f ? "  READY" : "")), true);
        }

        @Override
        public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remaining) {
            if (!fireball || world.isClient || !(user instanceof PlayerEntity p)) return;
            float c = charge(this, stack, remaining, 60f);
            if (c < 0.25f) return;
            ThrownToolEntity ball = new ThrownToolEntity(world, p);
            ball.setItem(new ItemStack(Items.FIRE_CHARGE));
            ball.configure(0f, "fireball", 11, "");
            ball.annihilate = true;
            ball.blast = 1f + 3f * c;
            ball.setNoGravity(true);
            ball.setVelocity(p, p.getPitch(), p.getYaw(), 0f, 1.6f, 0f);
            world.spawnEntity(ball);
            p.getItemCooldownManager().set(this, 600);   // 30 seconds
            world.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1f, 0.8f);
        }
    }

    public static class PaPickaxe extends PickaxeItem {
        public PaPickaxe(ToolMaterial m, int dmg, float speed, Settings s) { super(m, dmg, speed, s); }

        @Override
        public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            Abilities.onHit(stack, target, attacker, 1f);
            return super.postHit(stack, target, attacker);
        }
    }

    public static class PaAxe extends AxeItem {
        public PaAxe(ToolMaterial m, float dmg, float speed, Settings s) { super(m, dmg, speed, s); }

        @Override
        public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            Abilities.onHit(stack, target, attacker, 1f);
            AbilityInfo info = info(stack);
            if (info != null) {   // Cleave: also hits mobs around the target
                float share = 0.3f + info.tier() * 0.05f;
                for (LivingEntity n : Abilities.nearby(target, attacker, 1.5 + info.tier() * 0.25)) Abilities.extra(n, attacker, (4 + info.tier()) * share);
            }
            return super.postHit(stack, target, attacker);
        }
    }

    public static class PaShovel extends ShovelItem {
        public PaShovel(ToolMaterial m, float dmg, float speed, Settings s) { super(m, dmg, speed, s); }

        @Override
        public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            Abilities.onHit(stack, target, attacker, 1f);
            return super.postHit(stack, target, attacker);
        }
    }

    public static class PaHoe extends HoeItem {
        public PaHoe(ToolMaterial m, int dmg, float speed, Settings s) { super(m, dmg, speed, s); }

        @Override
        public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            Abilities.onHit(stack, target, attacker, 1f);
            return super.postHit(stack, target, attacker);
        }
    }

    /** Spears: more damage the faster you are moving (sprinting, riding, falling). Better ores scale faster. */
    public static class PaSpear extends SwordItem {
        public PaSpear(ToolMaterial m, int dmg, float speed, Settings s) { super(m, dmg, speed, s); }

        @Override
        public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            AbilityInfo info = info(stack);
            int tier = info == null ? 1 : info.tier();
            float v = ModEvents.speedOf(attacker);
            if (v > 3f && attacker.getWorld() instanceof ServerWorld sw) {
                Abilities.extra(target, attacker, (v - 3f) * (0.6f + tier * 0.22f));
                sw.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(0.5), target.getZ(), 10, 0.3, 0.3, 0.3, 0.2);
            }
            Abilities.onHit(stack, target, attacker, 1f);
            return super.postHit(stack, target, attacker);
        }
    }

    /** Maces: the farther you fall before hitting, the more damage. Better ores stack it faster. */
    public static class PaMace extends SwordItem {
        public PaMace(ToolMaterial m, int dmg, float speed, Settings s) { super(m, dmg, speed, s); }

        @Override
        public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            AbilityInfo info = info(stack);
            int tier = info == null ? 1 : info.tier();
            float fall = attacker.fallDistance;
            if (fall > 1.5f && attacker.getWorld() instanceof ServerWorld sw) {
                float smash = Abilities.smashDamage(fall, tier, stack);
                Abilities.extra(target, attacker, smash);
                sw.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, target.getX(), target.getY(), target.getZ(), 1, 0, 0, 0, 0);
                for (LivingEntity n : Abilities.nearby(target, attacker, 2.5 + tier * 0.15)) {
                    Abilities.extra(n, attacker, smash * 0.35f);
                    n.takeKnockback(0.9, target.getX() - n.getX(), target.getZ() - n.getZ());
                }
                attacker.fallDistance = 0f;
                Vec3d v = attacker.getVelocity();
                attacker.setVelocity(v.x, 0.6, v.z);
                attacker.velocityModified = true;
                if (info != null && info.key().equals("prismatic") && fall >= 12f) Abilities.meteor(target, attacker);
                if (attacker instanceof PlayerEntity p && fall > 6f) p.sendMessage(Text.literal("Smash! " + Math.round(smash) + " bonus damage from " + Math.round(fall) + " blocks"), true);
            }
            Abilities.onHit(stack, target, attacker, 1f);
            return super.postHit(stack, target, attacker);
        }

        /** Prismatic Mace: right click = fly 30s, sneak + right click = black hole for 30s. */
        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            ItemStack stack = user.getStackInHand(hand);
            AbilityInfo info = info(stack);
            if (info == null || !info.key().equals("prismatic")) return TypedActionResult.pass(stack);
            if (user instanceof ServerPlayerEntity sp) {
                if (user.isSneaking()) ModEvents.blackHole(sp); else ModEvents.startFlight(sp);
            }
            return TypedActionResult.success(stack, world.isClient);
        }
    }

    /** Ore tridents: hold to charge, release to throw (it stays in your hand). Takes trident enchantments. */
    public static class PaTrident extends TridentItem {
        public PaTrident(Settings s) { super(s); }

        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            user.setCurrentHand(hand);
            return TypedActionResult.consume(user.getStackInHand(hand));
        }

        @Override
        public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remaining) {
            if (world.isClient || !(user instanceof PlayerEntity p)) return;
            float c = charge(this, stack, remaining, 15f);
            if (c < 0.3f) return;
            AbilityInfo info = info(stack);
            int riptide = EnchantmentHelper.getRiptide(stack);
            if (riptide > 0 && p.isTouchingWaterOrRain()) {   // Riptide launches you instead
                Vec3d d = p.getRotationVector().multiply(1.2 * riptide);
                p.addVelocity(d.x, d.y, d.z);
                p.velocityModified = true;
                p.useRiptide(20);
                world.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 1f, 1f);
            } else {
                float dmg = Abilities.baseDamage("trident") * Abilities.tierMult(stack) * (0.35f + 0.65f * c);
                Abilities.launch(p, stack, dmg, "trident", info, 1.4f + 2.4f * c);
                world.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1f, 1f);
            }
            stack.damage(2, p, pl -> pl.sendToolBreakStatus(pl.getActiveHand()));
        }

        @Override
        public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            Abilities.onHit(stack, target, attacker, 1f);
            return super.postHit(stack, target, attacker);
        }
    }

    /** Tool Longbow: fires whatever weapon or tool is in your offhand. Sneak + use loads the next one. */
    public static class LongbowItem extends BowItem {
        private static final Map<UUID, Integer> NEXT_SLOT = new HashMap<>();

        public LongbowItem(Settings s) { super(s); }

        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            ItemStack stack = user.getStackInHand(hand);
            if (user.isSneaking()) {
                if (!world.isClient) loadOffhand(user);
                return TypedActionResult.success(stack, world.isClient);
            }
            user.setCurrentHand(hand);
            return TypedActionResult.consume(stack);
        }

        @Override
        public void onStoppedUsing(ItemStack bow, World world, LivingEntity user, int remaining) {
            if (world.isClient || !(user instanceof PlayerEntity p)) return;
            float c = charge(this, bow, remaining, 25f);
            if (c < 0.15f) return;
            ItemStack off = p.getOffHandStack();
            String kind = Abilities.kindOf(off);
            if (kind == null) { p.sendMessage(Text.literal("Put a weapon or tool in your offhand (sneak + use to load one)"), true); return; }
            float dmg = Abilities.baseDamage(kind) * Abilities.tierMult(off) * (0.35f + 0.65f * c);
            dmg *= 1f + 0.25f * EnchantmentHelper.getLevel(Enchantments.POWER, bow);
            if (kind.equals("spear")) dmg += Math.max(0f, ModEvents.speedOf(p) - 3f) * 0.6f * Abilities.tierMult(off);
            ThrownToolEntity e = Abilities.launch(p, off, dmg, kind, info(off), 1.4f + 2.4f * c);
            if (EnchantmentHelper.getLevel(Enchantments.FLAME, bow) > 0) e.setOnFireFor(100);
            e.punch = EnchantmentHelper.getLevel(Enchantments.PUNCH, bow);
            if (EnchantmentHelper.getLevel(Enchantments.INFINITY, bow) == 0 && off.isDamageable()) {
                int cost = kind.equals("mace") || kind.equals("spear") || kind.equals("trident") ? 3 : 2;
                off.damage(cost, p, pl -> pl.sendEquipmentBreakStatus(EquipmentSlot.OFFHAND));
            }
            bow.damage(1, p, pl -> pl.sendToolBreakStatus(pl.getActiveHand()));
            world.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1f, 0.6f);
        }

        static void loadOffhand(PlayerEntity p) {
            PlayerInventory inv = p.getInventory();
            int size = inv.main.size(), start = NEXT_SLOT.getOrDefault(p.getUuid(), 0) % size;
            for (int k = 0; k < size; k++) {
                int i = (start + k) % size;
                ItemStack s = inv.main.get(i);
                if (s.isEmpty() || s.getItem() instanceof LongbowItem || Abilities.kindOf(s) == null) continue;
                inv.main.set(i, p.getOffHandStack());
                inv.offHand.set(0, s);
                NEXT_SLOT.put(p.getUuid(), i + 1);
                p.sendMessage(Text.literal("Loaded: ").append(s.getName()), true);
                return;
            }
            p.sendMessage(Text.literal("No weapons or tools to load"), true);
        }
    }
}
