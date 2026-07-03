package com.berlord.explosiveenhancement.mixin;

import com.berlord.explosiveenhancement.EEHandler;
import com.berlord.explosiveenhancement.ExplosiveConfig;
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

/**
 * Replaces the vanilla client explosion visual with Explosive Enhancement's particle
 * stack.
 *
 * <p><b>Why a HEAD re-implement instead of {@code @WrapOperation} on an
 * {@code addParticle} call?</b> On 1.21.1 {@code ClientPacketListener#handleExplosion}
 * does <i>not</i> spawn the explosion particle inline — it builds an {@link Explosion}
 * and calls {@code Explosion#finalizeExplosion(true)}, which is what actually plays the
 * sound and spawns the single central particle (gated by that boolean). There is no
 * {@code addParticle} invocation in {@code handleExplosion} to wrap, so we take the
 * spec-sanctioned fallback: inject at {@code HEAD}, re-run vanilla's behavior, and
 * cancel.
 *
 * <p>Fidelity is preserved by reconstructing the explosion exactly as vanilla and
 * calling {@code finalizeExplosion(showDefaultExplosion)} — passing the config flag so
 * the vanilla central particle appears only when the user opted in, while vanilla's
 * sound and (server-authoritative) block handling are untouched. Knockback is replayed
 * identically. We then layer our enhanced particles on top.
 *
 * <p>NOTE on {@code showDefaultSmoke}: vanilla's per-block explosion smoke on 1.21.1 is
 * driven by a separate server-side level event, not by this packet, so it is not
 * reachable from here (the config field is kept for compatibility but is a no-op).
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Inject(method = "handleExplosion", at = @At("HEAD"), cancellable = true)
    private void explosiveenhancement$enhanceExplosion(ClientboundExplodePacket packet, CallbackInfo ci) {
        ExplosiveConfig cfg = ExplosiveConfig.get();
        // Master switch off -> stay completely out of vanilla's way.
        if (!cfg.modEnabled) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return; // Shouldn't happen for an explosion packet, but be safe.
        }

        // Mirror vanilla's same-thread guard so packet ordering/threading is unchanged.
        PacketUtils.ensureRunningOnSameThread(packet, (ClientPacketListener) (Object) this, minecraft);

        double x = packet.getX();
        double y = packet.getY();
        double z = packet.getZ();
        float power = packet.getPower();

        // Decide whether to keep vanilla's central particle. finalizeExplosion(boolean)
        // gates EXACTLY that single particle; it always plays the explosion sound and
        // runs the (no-op on the client) block loop regardless.
        boolean underwater = cfg.underwaterExplosions
                && level.getFluidState(BlockPos.containing(x, y, z)).is(FluidTags.WATER);
        boolean keepVanillaParticle = underwater ? cfg.showDefaultExplosionUnderwater : cfg.showDefaultExplosion;

        // Reconstruct the explosion exactly as vanilla does and finalize it. This
        // preserves the vanilla sound (and any block side effects) while letting us
        // suppress/keep the vanilla central particle via the boolean.
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

        // Replay vanilla knockback on the local player, identical to the original method.
        player.setDeltaMovement(
                player.getDeltaMovement().add(
                        packet.getKnockbackX(),
                        packet.getKnockbackY(),
                        packet.getKnockbackZ()
                )
        );

        // Layer our enhanced particle stack on top of whatever vanilla kept.
        EEHandler.spawnEnhancedParticles(level, x, y, z, power);

        // We've fully handled the packet; don't let vanilla run it again.
        ci.cancel();
    }
}
