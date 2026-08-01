package com.berlord.explosiveenhancement.test;

import com.mojang.logging.LogUtils;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.Arrays;

@Mod(value = ExplosiveEnhancementClientTestMod.MOD_ID, dist = Dist.CLIENT)
public final class ExplosiveEnhancementClientTestMod {
    static final String MOD_ID = "explosiveenhancementtest";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ExplosiveEnhancementClientTestMod(IEventBus modBus) {
        modBus.addListener(this::onLoadComplete);
    }

    private void onLoadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            boolean applied = Arrays.stream(ClientPacketListener.class.getDeclaredMethods())
                    .map(Method::getName)
                    .anyMatch(name -> name.contains("explosiveenhancement$enhanceExplosion"));
            if (!applied) {
                throw new IllegalStateException("ClientPacketListener is missing the explosion handler");
            }
            LOGGER.info("EXPLOSIVE_ENHANCEMENT_MIXIN_OK");
        });
    }
}
