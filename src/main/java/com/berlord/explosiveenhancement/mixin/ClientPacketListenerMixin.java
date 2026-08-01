package com.berlord.explosiveenhancement.mixin;

import com.berlord.explosiveenhancement.EEHandler;
import com.berlord.explosiveenhancement.ExplosiveConfig;
import com.berlord.explosiveenhancement.ExplosionPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Explosion;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces the vanilla client explosion path while preserving sound and knockback. */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Inject(method = "handleExplosion", at = @At("HEAD"), cancellable = true)
    private void explosiveenhancement$enhanceExplosion(ClientboundExplodePacket packet, CallbackInfo ci) {
        ExplosiveConfig cfg = ExplosiveConfig.get();
        if (!cfg.modEnabled) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return;
        }

        PacketUtils.ensureRunningOnSameThread(packet, (ClientPacketListener) (Object) this, minecraft);

        double x = packet.getX();
        double y = packet.getY();
        double z = packet.getZ();
        float power = packet.getPower();

        boolean underwater = cfg.underwaterExplosions
                && level.getFluidState(BlockPos.containing(x, y, z)).is(FluidTags.WATER);
        boolean keepVanillaParticle = ExplosionPolicy.keepDefaultParticle(
                underwater, cfg.showDefaultExplosion, cfg.showDefaultExplosionUnderwater);

        Explosion explosion = new Explosion(
                level,
                null,
                x,
                y,
                z,
                power,
                packet.getToBlow(),
                packet.getBlockInteraction(),
                packet.getSmallExplosionParticles(),
                packet.getLargeExplosionParticles(),
                packet.getExplosionSound()
        );
        explosion.finalizeExplosion(keepVanillaParticle);

        player.setDeltaMovement(
                player.getDeltaMovement().add(
                        packet.getKnockbackX(),
                        packet.getKnockbackY(),
                        packet.getKnockbackZ()
                )
        );

        EEHandler.spawnEnhancedParticles(level, x, y, z, power);
        ci.cancel();
    }
}
