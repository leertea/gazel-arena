package com.gazelarena;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Baut den Thronsaal. Der Saal wird bei jedem Start/Reset komplett neu gesetzt;
 * bereits korrekte Bloecke werden uebersprungen, daher ist ein Reset guenstig.
 */
public final class ArenaBuilder {
    /** Halbe Kantenlaenge des Innenraums (Innenraum = 2*HALF+1 Bloecke breit). */
    public static final int HALF = 22;
    /** Innenhoehe ueber dem Boden. */
    public static final int HEIGHT = 18;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState WALL = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    private static final BlockState CEILING = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    private static final BlockState FLOOR_A = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
    private static final BlockState FLOOR_B = Blocks.DEEPSLATE_TILES.defaultBlockState();
    private static final BlockState LIGHT = Blocks.SEA_LANTERN.defaultBlockState();
    private static final BlockState GOLD = Blocks.GOLD_BLOCK.defaultBlockState();
    private static final BlockState PILLAR = Blocks.QUARTZ_PILLAR.defaultBlockState();
    private static final BlockState CARPET = Blocks.RED_CARPET.defaultBlockState();
    private static final BlockState PLATFORM = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
    private static final BlockState PLATFORM_2 = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    private static final BlockState THRONE_SEAT = Blocks.QUARTZ_STAIRS.defaultBlockState()
            .setValue(StairBlock.FACING, Direction.NORTH);
    private static final BlockState THRONE_ARM = Blocks.QUARTZ_SLAB.defaultBlockState();

    private ArenaBuilder() {
    }

    public static AABB bounds(BlockPos c) {
        return new AABB(
                c.getX() - HALF - 2, c.getY() - 2, c.getZ() - HALF - 2,
                c.getX() + HALF + 3, c.getY() + HEIGHT + 3, c.getZ() + HALF + 3);
    }

    /** Spawnpunkt des Spielers (Suedende, Blick nach Norden zum Thron). */
    public static BlockPos playerSpawn(BlockPos c) {
        return c.offset(0, 1, HALF - 3);
    }

    /** Spawnpunkt des Bosses (vor dem Thron). */
    public static BlockPos bossSpawn(BlockPos c) {
        return c.offset(0, 1, -HALF + 8);
    }

    public static void build(ServerLevel level, BlockPos c) {
        for (int dx = -HALF - 1; dx <= HALF + 1; dx++) {
            for (int dz = -HALF - 1; dz <= HALF + 1; dz++) {
                for (int dy = -1; dy <= HEIGHT + 1; dy++) {
                    BlockPos pos = c.offset(dx, dy, dz);
                    BlockState wanted = stateAt(dx, dy, dz);
                    if (level.getBlockState(pos) != wanted) {
                        level.setBlock(pos, wanted, 2 | 16);
                    }
                }
            }
        }
    }

    /** Entfernt alles ausser Spielern (Boss, Minions, Projektile, Items, XP ...). */
    public static void clearEntities(ServerLevel level, BlockPos c) {
        for (Entity e : level.getEntitiesOfClass(Entity.class, bounds(c), x -> !(x instanceof Player))) {
            e.discard();
        }
    }

    private static BlockState stateAt(int dx, int dy, int dz) {
        int ax = Math.abs(dx);
        int az = Math.abs(dz);
        boolean edge = ax == HALF + 1 || az == HALF + 1;

        if (dy == -1) {
            return CEILING;
        }
        if (dy == HEIGHT + 1) {
            if (!edge && ax % 8 == 0 && az % 8 == 0) {
                return LIGHT;
            }
            return CEILING;
        }
        if (edge) {
            return WALL;
        }
        if (dy == 0) {
            // Bodenlichter, damit nichts im Saal spawnt
            if (ax % 8 == 4 && az % 8 == 4) {
                return LIGHT;
            }
            return ((dx + dz) & 1) == 0 ? FLOOR_A : FLOOR_B;
        }

        // Thron (Nordwand)
        if (dy >= 3) {
            if (dz == -HALF && ax <= 1 && dy <= 8) {
                return GOLD;
            }
            if (dz == -HALF + 1 && dy == 3) {
                if (dx == 0) {
                    return THRONE_SEAT;
                }
                if (ax == 1) {
                    return THRONE_ARM;
                }
            }
        }
        // Podest
        if (dz <= -HALF + 1 && ax <= 3 && dy <= 2) {
            return dy == 2 ? PLATFORM_2 : PLATFORM;
        }
        if (dz <= -HALF + 3 && ax <= 5 && dy == 1) {
            return PLATFORM;
        }
        // Saeulen
        if (ax == 14 && az <= 16 && az % 8 == 0) {
            return (dy == 1 || dy == HEIGHT) ? GOLD : PILLAR;
        }
        // Roter Teppich
        if (dy == 1 && ax <= 2 && dz >= -HALF + 4 && dz <= HALF) {
            return CARPET;
        }
        return AIR;
    }
}
