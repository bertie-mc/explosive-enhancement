package com.berlord.explosiveenhancement;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/** Client-only explosion particle replacement. */
@Mod(value = ExplosiveEnhancement.MOD_ID, dist = Dist.CLIENT)
public class ExplosiveEnhancement {
    public static final String MOD_ID = "explosiveenhancement";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ExplosiveEnhancement(IEventBus ignored) {
        ExplosiveConfig.load();
        LOGGER.info("Explosive Enhancement loaded.");
    }
}
