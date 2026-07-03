package com.berlord.explosiveenhancement;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Explosive Enhancement — NeoForge 1.21.1 reimplementation.
 *
 * <p>Original Fabric mod by Superkat32; this is a faithful, asset-free port of its
 * visual behavior. It replaces the single vanilla central explosion particle with a
 * richer stack (blast-wave ring, fireball flash, sparks, a 6-particle mushroom cloud)
 * and swaps to an underwater variant (shockwave ring + rising bubbles) when the
 * explosion happens in water. All effects are vanilla-particle-backed — no custom
 * particle types, textures or registries — and individually toggleable via a GSON
 * config at {@code config/explosiveenhancement.json}.
 *
 * <p>Client-only: the whole behavior lives in {@code ClientPacketListenerMixin} which
 * targets a client packet handler, so there is nothing to register on a dedicated
 * server.
 */
@Mod(value = ExplosiveEnhancement.MOD_ID, dist = Dist.CLIENT)
public class ExplosiveEnhancement {
    public static final String MOD_ID = "explosiveenhancement";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ExplosiveEnhancement(IEventBus modBus) {
        // Load (or create with defaults) the JSON config eagerly so the mixin can read
        // it without a null check on the first explosion. The mixin re-reads the cached
        // instance, so this single load is enough for the session.
        ExplosiveConfig.load();
        LOGGER.info("Explosive Enhancement loaded (client-only, vanilla-particle reimplementation).");
    }
}
