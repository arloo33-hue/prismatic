package com.prismaticarsenal;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;
import java.util.function.Predicate;

public final class ModEvents {
    private ModEvents() {}

    private static final Map<UUID, Vec3d> LAST_POS = new HashMap<>();
    private static final Map<UUID, Float> SPEED = new HashMap<>();
    private static final Map<UUID, Long> FLIGHT_END = new HashMap<>();
    private static final Map<String, Long> COOLDOWN = new HashMap<>();
    private static final List<BlackHole> HOLES = new ArrayList<>();
    private static final Map<BlockPos, Integer> BEDROCK_HITS = new HashMap<>();
    private static final List<String> BEDROCK_PICKS = List.of("prismatic", "eclipsium", "chronite", "dragonite");
    private static long tick = 0;
    private static boolean breaking = false;

    record BlackHole(ServerWorld world, Vec3d pos, UUID owner, long end) {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ModEvents::onTick);
        PlayerBlockBreakEvents.AFTER.register(ModEvents::afterBreak);
        AttackBlockCallback.EVENT.register(ModEvents::attackBlock);
        // No fall damage while holding the Prismatic Mace
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
                !(source.isOf(DamageTypes.FALL) && entity instanceof PlayerEntity p && isPrismaticMace(p.getMainHandStack())));
    }

    /** Blocks per second (players are tracked by position because the server doesn't know their velocity). */
    public static float speedOf(LivingEntity e) {
        if (e instanceof PlayerEntity) return SPEED.getOrDefault(e.getUuid(), 0f);
        return (float) e.getVelocity().length() * 20f;
    }

    static boolean isPrismaticMace(ItemStack s) {
        return s.getItem() == ModContent.GEAR.get(PaMaterial.PRISMATIC).get("mace");
    }

    private static void give(PlayerEntity p, StatusEffect e, int amp) {
        p.addStatusEffect(new StatusEffectInstance(e, 60, amp, true, false));
    }

    // ---------- every tick ----------
    private static void onTick(MinecraftServer server) {
        tick++;
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            Vec3d now = p.getPos(), last = LAST_POS.put(p.getUuid(), now);
            if (last != null) SPEED.put(p.getUuid(), (float) now.distanceTo(last) * 20f);
            if (tick % 40 == 0) setBonus(p);
            if (tick % 20 == 0 && isPrismaticMace(p.getMainHandStack())) {
                give(p, StatusEffects.STRENGTH, 2);
                give(p, StatusEffects.RESISTANCE, 1);
                give(p, StatusEffects.FIRE_RESISTANCE, 0);
                give(p, StatusEffects.SPEED, 0);
            }
            Long end = FLIGHT_END.get(p.getUuid());
            if (end != null) {
                if (tick >= end) {
                    FLIGHT_END.remove(p.getUuid());
                    if (!p.isCreative() && !p.isSpectator()) {
                        p.getAbilities().allowFlying = false;
                        p.getAbilities().flying = false;
                        p.sendAbilitiesUpdate();
                    }
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 200, 0));
                    p.sendMessage(Text.literal("Flight ended. Slow Falling for 10s"), true);
                } else if ((end - tick) % 20 == 0) {
                    p.sendMessage(Text.literal("Flight: " + (end - tick) / 20 + "s"), true);
                }
            }
        }
        tickHoles();
    }

    private static void setBonus(ServerPlayerEntity p) {
        PaMaterial set = null;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            PaMaterial m = ModContent.ARMOR_OF.get(p.getEquippedStack(slot).getItem());
            if (m == null || (set != null && set != m)) return;
            set = m;
        }
        for (String part : set.bonus.split(",")) {
            String[] kv = part.split(":");
            StatusEffect effect = Registries.STATUS_EFFECT.get(new Identifier(kv[0]));
            if (effect != null) p.addStatusEffect(new StatusEffectInstance(effect, 260, Integer.parseInt(kv[1]), true, false));
        }
    }

    // ---------- Prismatic Mace: flight + black hole ----------
    public static void startFlight(ServerPlayerEntity p) {
        String key = p.getUuidAsString() + ":flight";
        long ready = COOLDOWN.getOrDefault(key, 0L);
        if (FLIGHT_END.containsKey(p.getUuid())) { p.sendMessage(Text.literal("Already flying"), true); return; }
        if (tick < ready) { p.sendMessage(Text.literal("Flight recharging: " + (ready - tick) / 20 + "s"), true); return; }
        FLIGHT_END.put(p.getUuid(), tick + 600);
        COOLDOWN.put(key, tick + 1200);
        p.getAbilities().allowFlying = true;
        p.getAbilities().flying = true;
        p.sendAbilitiesUpdate();
        p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 1f, 1f);
        p.sendMessage(Text.literal("FLIGHT for 30 seconds"), true);
    }

    public static void blackHole(ServerPlayerEntity p) {
        String key = p.getUuidAsString() + ":hole";
        long ready = COOLDOWN.getOrDefault(key, 0L);
        if (tick < ready) { p.sendMessage(Text.literal("Black Hole recharging: " + (ready - tick) / 20 + "s"), true); return; }
        COOLDOWN.put(key, tick + 1200);
        HitResult hit = p.raycast(24, 0f, false);
        Vec3d pos = hit.getPos().subtract(p.getRotationVector().multiply(1.5));
        HOLES.add(new BlackHole(p.getServerWorld(), pos, p.getUuid(), tick + 600));
        p.getWorld().playSound(null, BlockPos.ofFloored(pos), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 2f, 0.5f);
        p.sendMessage(Text.literal("BLACK HOLE (30s)"), true);
    }

    private static void tickHoles() {
        Iterator<BlackHole> it = HOLES.iterator();
        while (it.hasNext()) {
            BlackHole h = it.next();
            ServerWorld w = h.world();
            Vec3d c = h.pos();
            if (tick >= h.end()) {
                w.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 1, 0, 0, 0, 0);
                it.remove();
                continue;
            }
            w.spawnParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, 12, 1.2, 0.4, 1.2, 0.05);
            w.spawnParticles(ParticleTypes.SQUID_INK, c.x, c.y, c.z, 3, 0.15, 0.15, 0.15, 0.0);
            if (tick % 4 == 0) w.spawnParticles(ParticleTypes.DRAGON_BREATH, c.x, c.y, c.z, 4, 0.3, 0.3, 0.3, 0.01);
            Entity owner = w.getEntity(h.owner());
            for (Entity e : w.getOtherEntities(owner, new Box(c, c).expand(14))) {
                if (e instanceof PlayerEntity pl && (pl.isCreative() || pl.isSpectator())) continue;
                Vec3d d = c.subtract(e.getPos());
                double dist = Math.max(0.3, d.length()), s = 0.08 + (14 - dist) / 14 * 0.35;
                e.addVelocity(d.x / dist * s, d.y / dist * s + 0.04, d.z / dist * s);
                e.velocityModified = true;
                if (dist < 2.2 && tick % 10 == 0 && e instanceof LivingEntity le) {
                    le.timeUntilRegen = 0;
                    le.damage(w.getDamageSources().magic(), 10f);
                }
            }
        }
    }

    // ---------- Vein Miner, Area Mining, Timber, Excavate ----------
    private static void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity be) {
        if (breaking || world.isClient) return;
        ItemStack tool = player.getMainHandStack();
        AbilityInfo info = ModContent.ABILITY_OF.get(tool.getItem());
        if (info == null) return;
        int t = info.tier();
        Item item = tool.getItem();
        List<BlockPos> extra = List.of();
        if (item instanceof PaItems.PaPickaxe && isOre(state)) extra = connected(world, pos, state.getBlock(), 2 + t * 3);
        else if (item instanceof PaItems.PaPickaxe && t >= 4 && !player.isSneaking()) extra = area(world, player, pos, t >= 10 ? 2 : 1, s -> s.isIn(BlockTags.PICKAXE_MINEABLE));
        else if (item instanceof PaItems.PaAxe && state.isIn(BlockTags.LOGS)) extra = connected(world, pos, state.getBlock(), 6 + t * 10);
        else if (item instanceof PaItems.PaShovel && t >= 2 && !player.isSneaking()) extra = area(world, player, pos, t >= 8 ? 2 : 1, s -> s.isIn(BlockTags.SHOVEL_MINEABLE));
        if (extra.isEmpty()) return;
        breaking = true;
        try {
            int broken = 0;
            for (BlockPos b : extra) {
                if (tool.isEmpty()) break;
                BlockState s = world.getBlockState(b);
                if (s.isAir() || s.getHardness(world, b) < 0 || (s.isToolRequired() && !tool.isSuitableFor(s))) continue;
                Block.dropStacks(s, world, b, world.getBlockEntity(b), player, tool);
                world.breakBlock(b, false, player);
                broken++;
            }
            if (broken > 0) tool.damage(Math.max(1, broken / 2), player, pl -> pl.sendToolBreakStatus(Hand.MAIN_HAND));
        } finally {
            breaking = false;
        }
    }

    private static boolean isOre(BlockState s) {
        return Registries.BLOCK.getId(s.getBlock()).getPath().endsWith("_ore") || s.isOf(Blocks.ANCIENT_DEBRIS);
    }

    private static List<BlockPos> connected(World w, BlockPos start, Block type, int max) {
        List<BlockPos> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>(List.of(start));
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(List.of(start));
        while (!queue.isEmpty() && out.size() < max) {
            BlockPos at = queue.poll();
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                BlockPos n = at.add(dx, dy, dz);
                if (!seen.add(n) || !w.getBlockState(n).isOf(type)) continue;
                out.add(n);
                queue.add(n);
                if (out.size() >= max) return out;
            }
        }
        return out;
    }

    private static List<BlockPos> area(World w, PlayerEntity p, BlockPos c, int r, Predicate<BlockState> ok) {
        List<BlockPos> out = new ArrayList<>();
        boolean flat = Math.abs(p.getPitch()) > 45f;
        Direction.Axis axis = p.getHorizontalFacing().getAxis();
        for (int a = -r; a <= r; a++) for (int b = -r; b <= r; b++) {
            if (a == 0 && b == 0) continue;
            BlockPos n = flat ? c.add(a, 0, b) : axis == Direction.Axis.X ? c.add(0, a, b) : c.add(a, b, 0);
            if (ok.test(w.getBlockState(n))) out.add(n);
        }
        return out;
    }

    // ---------- Bedrock Breaker (Prismatic 1 hit, Eclipsium 2, Chronite/Dragonite 3) ----------
    private static ActionResult attackBlock(PlayerEntity player, World world, Hand hand, BlockPos pos, Direction dir) {
        if (world.isClient || !world.getBlockState(pos).isOf(Blocks.BEDROCK)) return ActionResult.PASS;
        ItemStack tool = player.getMainHandStack();
        AbilityInfo info = ModContent.ABILITY_OF.get(tool.getItem());
        if (info == null || !(tool.getItem() instanceof PaItems.PaPickaxe) || !BEDROCK_PICKS.contains(info.key())) return ActionResult.PASS;
        if (pos.getY() <= world.getBottomY()) {
            player.sendMessage(Text.literal("The bottom bedrock layer is protected (void below!)"), true);
            return ActionResult.PASS;
        }
        int need = info.key().equals("prismatic") ? 1 : info.key().equals("eclipsium") ? 2 : 3;
        int hits = BEDROCK_HITS.merge(pos.toImmutable(), 1, Integer::sum);
        if (hits < need) return ActionResult.PASS;
        BEDROCK_HITS.remove(pos.toImmutable());
        world.breakBlock(pos, false, player);
        Block.dropStack(world, pos, new ItemStack(Items.BEDROCK));
        tool.damage(5, player, pl -> pl.sendToolBreakStatus(Hand.MAIN_HAND));
        return ActionResult.PASS;
    }
}
