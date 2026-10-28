package com.gazelarena;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(GazelArenaMod.MOD_ID)
public class GazelArenaMod {
    public static final String MOD_ID = "gazelarena";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GazelArenaMod(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, ArenaConfig.SPEC);
    }
}
