package com.gazelarena;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ArenaConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> BOSS_ENTITY = BUILDER
            .comment("Entity-ID des Bosses. Wird die ID nicht gefunden, sucht die Mod automatisch",
                    "nach einer Entity, deren Name 'gazel' enthaelt (bevorzugt aus Tensura).")
            .define("bossEntity", "tensura:gazel_dwargo");

    public static final ModConfigSpec.IntValue ARENA_X = BUILDER
            .comment("Mittelpunkt der Arena im Overworld (X). Die Arena wird dort automatisch gebaut.")
            .defineInRange("arenaX", 1000000, -29000000, 29000000);

    public static final ModConfigSpec.IntValue ARENA_Y = BUILDER
            .comment("Hoehe des Arena-Bodens (Y).")
            .defineInRange("arenaY", 200, -40, 260);

    public static final ModConfigSpec.IntValue ARENA_Z = BUILDER
            .comment("Mittelpunkt der Arena im Overworld (Z).")
            .defineInRange("arenaZ", 1000000, -29000000, 29000000);

    public static final ModConfigSpec.IntValue WIN_RETURN_SECONDS = BUILDER
            .comment("Sekunden nach dem Sieg, in denen der Spieler Loot einsammeln kann, bevor er zurueckteleportiert wird.")
            .defineInRange("winReturnSeconds", 20, 3, 600);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> VICTORY_COMMANDS = BUILDER
            .comment("Befehle, die bei einem Sieg als Server ausgefuehrt werden. %player% = Spielername.",
                    "Beispiel: [\"give %player% minecraft:diamond 5\"]")
            .defineListAllowEmpty("victoryCommands", List.of(), () -> "", o -> o instanceof String);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ArenaConfig() {
    }
}
