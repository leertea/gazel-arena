package com.gazelarena;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/** Verwaltet den (einen) aktiven Arena-Kampf. */
public final class ArenaManager {
    private static final String BOSS_TAG = "gazelarena_boss";
    private static final String DATA_KEY = "gazelarena_return";

    private enum Phase { FIGHT, WON }

    private static final class Session {
        final UUID player;
        UUID boss;
        Phase phase = Phase.FIGHT;
        long startTick;
        long wonTick;
        int deadChecks;
        boolean defeatPending;

        Session(UUID player) {
            this.player = player;
        }
    }

    private static Session session;

    private ArenaManager() {
    }

    private static BlockPos center() {
        return new BlockPos(ArenaConfig.ARENA_X.get(), ArenaConfig.ARENA_Y.get(), ArenaConfig.ARENA_Z.get());
    }

    private static void msg(ServerPlayer p, String text) {
        p.sendSystemMessage(Component.literal(text));
    }

    // ------------------------------------------------------------------ Start / Leave

    public static int start(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return 0;
        }
        if (session != null) {
            if (session.player.equals(player.getUUID())) {
                msg(player, "§eDu kämpfst bereits gegen Gazel. Mit §f/gazel leave§e kannst du die Arena verlassen.");
            } else {
                msg(player, "§cDie Arena ist gerade besetzt. Versuche es gleich noch einmal.");
            }
            return 0;
        }

        EntityType<?> bossType = resolveBoss();
        if (bossType == null) {
            msg(player, "§cGazel Dwargo wurde nicht gefunden. Ist Tensura: Reincarnated installiert? "
                    + "Sonst die Entity-ID unter 'bossEntity' in config/gazelarena-common.toml anpassen.");
            return 0;
        }

        ServerLevel arena = server.overworld();
        BlockPos c = center();
        ArenaBuilder.build(arena, c);
        ArenaBuilder.clearEntities(arena, c);

        BlockPos bossPos = ArenaBuilder.bossSpawn(c);
        Entity boss = bossType.spawn(arena, null, null, bossPos, MobSpawnType.COMMAND, false, false);
        if (boss == null) {
            msg(player, "§cDer Boss konnte nicht gespawnt werden.");
            return 0;
        }
        boss.addTag(BOSS_TAG);
        if (boss instanceof Mob mob) {
            mob.setPersistenceRequired();
        }

        saveReturn(player);
        session = new Session(player.getUUID());
        session.boss = boss.getUUID();
        session.startTick = server.getTickCount();

        player.stopRiding();
        BlockPos sp = ArenaBuilder.playerSpawn(c);
        player.teleportTo(arena, sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5, 180.0F, 0.0F);
        player.fallDistance = 0.0F;
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 4, false, false));

        msg(player, "§6Du betrittst den Thronsaal. §cGazel Dwargo§6, der Heldenkönig, erwartet dich!");
        return 1;
    }

    public static int leave(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return 0;
        }
        if (session == null || !session.player.equals(player.getUUID())) {
            msg(player, "§cDu bist gerade nicht im Kampf gegen Gazel.");
            return 0;
        }
        finish(server, player);
        msg(player, "§7Du hast die Arena verlassen. Sie wurde zurückgesetzt.");
        return 1;
    }

    // ------------------------------------------------------------------ Tick

    public static void tick(MinecraftServer server) {
        Session s = session;
        if (s == null) {
            return;
        }
        long now = server.getTickCount();
        if (!s.defeatPending && now % 10 != 0) {
            return;
        }

        ServerPlayer p = server.getPlayerList().getPlayer(s.player);
        if (p == null) {
            finish(server, null);
            return;
        }
        if (s.defeatPending) {
            heal(p);
            msg(p, "§cGazel Dwargo hat dich besiegt. Die Arena wird zurückgesetzt – du darfst es erneut versuchen!");
            finish(server, p);
            return;
        }
        if (!isInside(server, p)) {
            finish(server, null);
            msg(p, "§7Du hast die Arena verlassen. Sie wurde zurückgesetzt.");
            return;
        }

        if (s.phase == Phase.FIGHT) {
            if (now - s.startTick < 40) {
                return;
            }
            Entity boss = s.boss == null ? null : server.overworld().getEntity(s.boss);
            boolean dead = boss == null || boss.isRemoved() || !boss.isAlive();
            s.deadChecks = dead ? s.deadChecks + 1 : 0;
            if (s.deadChecks >= 2) {
                win(server, p, s, now);
            }
        } else if (now - s.wonTick >= ArenaConfig.WIN_RETURN_SECONDS.get() * 20L) {
            finish(server, p);
            msg(p, "§7Die Arena wurde zurückgesetzt. Du kannst jederzeit mit §f/gazel§7 erneut kämpfen.");
        }
    }

    private static void win(MinecraftServer server, ServerPlayer p, Session s, long now) {
        s.phase = Phase.WON;
        s.wonTick = now;
        msg(p, "§a§lSieg! §r§aGazel Dwargo wurde besiegt. In " + ArenaConfig.WIN_RETURN_SECONDS.get()
                + " Sekunden wirst du zurückgebracht – sammle jetzt deine Beute ein.");
        String name = p.getName().getString();
        for (String cmd : ArenaConfig.VICTORY_COMMANDS.get()) {
            try {
                server.getCommands().performPrefixedCommand(
                        server.createCommandSourceStack().withSuppressedOutput(),
                        cmd.replace("%player%", name));
            } catch (Exception e) {
                GazelArenaMod.LOGGER.warn("Victory-Befehl fehlgeschlagen: {}", cmd, e);
            }
        }
    }

    // ------------------------------------------------------------------ Events

    /** Gibt true zurück, wenn der Tod verhindert werden soll (Niederlage in der Arena). */
    public static boolean onPlayerWouldDie(ServerPlayer p) {
        Session s = session;
        MinecraftServer server = p.getServer();
        if (s == null || server == null || !s.player.equals(p.getUUID()) || !isInside(server, p)) {
            return false;
        }
        heal(p);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4, false, false));
        s.defeatPending = true;
        return true;
    }

    public static void onLogin(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        if (server == null) {
            return;
        }
        if (session != null && session.player.equals(p.getUUID())) {
            return;
        }
        // Spieler hat sich in der Arena ausgeloggt (oder Server ist abgestuerzt) -> zurueckholen
        if (isInside(server, p)) {
            returnPlayer(server, p);
            msg(p, "§7Du wurdest aus der Gazel-Arena zurückgebracht.");
        }
    }

    public static void onLogout(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        if (server != null && session != null && session.player.equals(p.getUUID())) {
            finish(server, null);
        }
    }

    public static void onServerStopping() {
        session = null;
    }

    // ------------------------------------------------------------------ Helpers

    private static boolean isInside(MinecraftServer server, ServerPlayer p) {
        return p.level() == server.overworld() && ArenaBuilder.bounds(center()).contains(p.position());
    }

    /** Beendet den Kampf, setzt die Arena zurueck und holt den Spieler (falls vorhanden) zurueck. */
    private static void finish(MinecraftServer server, ServerPlayer p) {
        ServerLevel arena = server.overworld();
        BlockPos c = center();
        ArenaBuilder.clearEntities(arena, c);
        session = null;
        if (p != null) {
            returnPlayer(server, p);
        }
        ArenaBuilder.build(arena, c);
    }

    private static void heal(ServerPlayer p) {
        p.removeAllEffects();
        p.clearFire();
        p.setHealth(p.getMaxHealth());
        p.getFoodData().setFoodLevel(20);
        p.fallDistance = 0.0F;
    }

    private static void saveReturn(ServerPlayer p) {
        CompoundTag t = new CompoundTag();
        t.putString("dim", p.level().dimension().location().toString());
        t.putDouble("x", p.getX());
        t.putDouble("y", p.getY());
        t.putDouble("z", p.getZ());
        t.putFloat("yaw", p.getYRot());
        t.putFloat("pitch", p.getXRot());
        p.getPersistentData().put(DATA_KEY, t);
    }

    private static void returnPlayer(MinecraftServer server, ServerPlayer p) {
        ServerLevel dest = server.overworld();
        double x;
        double y;
        double z;
        float yaw = 0.0F;
        float pitch = 0.0F;

        CompoundTag t = p.getPersistentData().getCompound(DATA_KEY);
        if (!t.isEmpty() && t.contains("dim")) {
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(t.getString("dim")));
            ServerLevel l = server.getLevel(key);
            if (l != null) {
                dest = l;
            }
            x = t.getDouble("x");
            y = t.getDouble("y");
            z = t.getDouble("z");
            yaw = t.getFloat("yaw");
            pitch = t.getFloat("pitch");
        } else {
            BlockPos spawn = dest.getSharedSpawnPos();
            BlockPos top = dest.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn);
            x = top.getX() + 0.5;
            y = top.getY();
            z = top.getZ() + 0.5;
        }
        p.teleportTo(dest, x, y, z, yaw, pitch);
        p.fallDistance = 0.0F;
        p.getPersistentData().remove(DATA_KEY);
    }

    /** Sucht den Boss: erst die konfigurierte ID, dann irgendeine Entity mit "gazel" im Namen. */
    private static EntityType<?> resolveBoss() {
        ResourceLocation id = ResourceLocation.tryParse(ArenaConfig.BOSS_ENTITY.get());
        if (id != null) {
            var found = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
            if (found.isPresent()) {
                return found.get();
            }
        }
        EntityType<?> best = null;
        int bestScore = Integer.MAX_VALUE;
        for (var entry : BuiltInRegistries.ENTITY_TYPE.entrySet()) {
            ResourceLocation key = entry.getKey().location();
            if (!key.getPath().contains("gazel")) {
                continue;
            }
            int score = key.getPath().length() + (key.getNamespace().equals("tensura") ? 0 : 1000);
            if (score < bestScore) {
                bestScore = score;
                best = entry.getValue();
            }
        }
        if (best != null) {
            GazelArenaMod.LOGGER.info("bossEntity '{}' nicht gefunden, verwende {}",
                    ArenaConfig.BOSS_ENTITY.get(), BuiltInRegistries.ENTITY_TYPE.getKey(best));
        }
        return best;
    }
}
