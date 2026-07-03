package com.berlord.explosiveenhancement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;

/**
 * Pure particle-spawning logic for an enhanced explosion, ported from Superkat32's
 * original mod but mapped onto vanilla {@link ParticleTypes} (the original ships custom
 * animated particle types/textures; this port is deliberately asset-free).
 *
 * <p>The spawn math — power scaling factors (1.75 / 1.25), the {@code y + 0.5} offset,
 * the small-explosion {@code y - 0.5} shift, the exact 6-particle mushroom-cloud
 * pattern, and the underwater 50-bubble loop with its random velocities — is preserved
 * from the source so the layout/feel matches even though the sprites differ.
 */
public final class EEHandler {

    private EEHandler() {
    }

    /**
     * Spawn the full enhanced-explosion particle stack at the given world position.
     *
     * @param level the client level
     * @param x     explosion center X (from the packet)
     * @param y     explosion center Y
     * @param z     explosion center Z
     * @param power explosion power/radius (from {@code ClientboundExplodePacket#getPower()})
     */
    public static void spawnEnhancedParticles(Level level, double x, double y, double z, float power) {
        ExplosiveConfig cfg = ExplosiveConfig.get();

        // Effective power: scaled by real explosion power, or pinned to 4 when dynamic
        // sizing is off (matches the original's "fallback to 4" behavior).
        float p = cfg.dynamicSize ? power : 4.0F;

        // Tiny (power == 1) explosions sit a touch lower so the ring hugs the ground,
        // exactly as the original shifts small explosions down by 0.5.
        double centerY = y + 0.5;
        if (power == 1.0F) {
            centerY = y - 0.5;
        }

        boolean underwater = cfg.underwaterExplosions
                && level.getFluidState(BlockPos.containing(x, y, z)).is(FluidTags.WATER);

        if (underwater) {
            spawnUnderwater(level, cfg, x, centerY, z, p);
        } else {
            spawnSurface(level, cfg, x, centerY, z, p);
        }
    }

    // -------------------------------------------------------------------------
    //  Surface (in-air) effects
    // -------------------------------------------------------------------------
    private static void spawnSurface(Level level, ExplosiveConfig cfg, double x, double y, double z, float p) {
        // (1) Blast wave: a flat expanding ring. The original uses a dedicated flat
        // sprite scaled by power*1.75; we approximate the disc with a horizontal ring
        // of POOF puffs (reads as a flat shock disc) plus one bright central flash.
        //
        // IMPORTANT: do NOT use ParticleTypes.EXPLOSION here. The client mixin already
        // suppresses vanilla's central explosion particle; re-spawning EXPLOSION (which
        // renders as HugeExplosionParticle, i.e. the exact vanilla poof) would make the
        // "enhanced" explosion look identical to vanilla. Use FLASH for the bright center
        // so the disc reads as a shock wave, not the vanilla cloud.
        if (cfg.showBlastWave) {
            addParticle(level, cfg, ParticleTypes.FLASH, x, y, z, 1.0, 0.0, 0.0);

            double radius = p * 0.75;
            int ringCount = Math.max(8, (int) (p * 4));
            for (int i = 0; i < ringCount; i++) {
                double angle = (Math.PI * 2.0) * i / ringCount;
                double dx = Math.cos(angle);
                double dz = Math.sin(angle);
                // Spawn out along the ring with a small outward velocity so the disc
                // visibly expands; the outward speed tracks the original's power*1.75
                // blast-wave scale (kept small so POOF reads as a flat shock disc).
                double speed = (p * 1.75) * 0.02;
                addParticle(level, cfg, ParticleTypes.POOF,
                        x + dx * radius, y, z + dz * radius,
                        dx * speed, 0.0, dz * speed);
            }
        }

        // (2) Fireball flash: a bright central puff (FLASH), scale power*1.25 in the
        // original. For "big" explosions (power >= 2) add a burst of outward FLAME +
        // POOF puffs to read as a fireball.
        //
        // IMPORTANT: do NOT use ParticleTypes.EXPLOSION_EMITTER here. It renders as
        // HugeExplosionSeedParticle, which spawns 6 vanilla EXPLOSION puffs/tick for 8
        // ticks — i.e. the exact vanilla explosion cloud the mixin just suppressed. That
        // is precisely why the enhanced explosion looked unchanged. Use a flame/poof
        // burst instead so big blasts look distinct from vanilla.
        if (cfg.showFireball) {
            addParticle(level, cfg, ParticleTypes.FLASH, x, y, z, 0.0, 0.0, 0.0);
            if (p >= 2.0F) {
                int fireballCount = Math.max(8, (int) (p * 5));
                for (int i = 0; i < fireballCount; i++) {
                    double vx = (level.random.nextDouble() - 0.5) * p * 0.25;
                    double vy = (level.random.nextDouble() - 0.2) * p * 0.2;
                    double vz = (level.random.nextDouble() - 0.5) * p * 0.25;
                    addParticle(level, cfg, ParticleTypes.FLAME, x, y, z, vx, vy, vz);
                    addParticle(level, cfg, ParticleTypes.POOF, x, y, z, vx * 0.6, vy * 0.6, vz * 0.6);
                }
            }
        }

        // (3) Sparks: short-lived outward streaks. Count ~power*6, random spherical
        // velocity of magnitude ~power*0.2.
        if (cfg.showSparks) {
            int sparkCount = Math.max(1, (int) (p * 6));
            for (int i = 0; i < sparkCount; i++) {
                double vx = (level.random.nextDouble() - 0.5) * p * 0.4;
                double vy = level.random.nextDouble() * p * 0.3;
                double vz = (level.random.nextDouble() - 0.5) * p * 0.4;
                addParticle(level, cfg, ParticleTypes.LAVA, x, y, z, vx, vy, vz);
            }
        }

        // (4) Mushroom cloud: exactly 6 rising smoke particles in the original's
        // pattern (1 center low-velY, 1 straight up, 4 diagonal). LARGE_SMOKE stands in
        // for the custom rising-smoke sprite.
        if (cfg.showMushroomCloud) {
            spawnMushroomCloud(level, cfg, x, y, z, p, ParticleTypes.LARGE_SMOKE);
        }
    }

    // -------------------------------------------------------------------------
    //  Underwater effects
    // -------------------------------------------------------------------------
    private static void spawnUnderwater(Level level, ExplosiveConfig cfg, double x, double y, double z, float p) {
        // Blast wave -> underwater shock ring of upward bubble columns.
        if (cfg.showUnderwaterBlastWave) {
            double radius = p * 0.75;
            int ringCount = Math.max(8, (int) (p * 4));
            for (int i = 0; i < ringCount; i++) {
                double angle = (Math.PI * 2.0) * i / ringCount;
                double dx = Math.cos(angle);
                double dz = Math.sin(angle);
                addParticle(level, cfg, ParticleTypes.BUBBLE_COLUMN_UP,
                        x + dx * radius, y, z + dz * radius,
                        0.0, 0.0, 0.0);
            }
        }

        // Fireball -> central shockwave puff.
        //
        // IMPORTANT: do NOT use ParticleTypes.EXPLOSION here either — same problem as the
        // surface path (it is the suppressed vanilla poof). Use a bright FLASH plus an
        // outward POOF ring so the underwater shockwave is visually distinct.
        if (cfg.showShockwave) {
            addParticle(level, cfg, ParticleTypes.FLASH, x, y, z, 0.0, 0.0, 0.0);
            int shockCount = Math.max(8, (int) (p * 4));
            for (int i = 0; i < shockCount; i++) {
                double angle = (Math.PI * 2.0) * i / shockCount;
                double dx = Math.cos(angle);
                double dz = Math.sin(angle);
                double speed = (p * 1.25) * 0.02;
                addParticle(level, cfg, ParticleTypes.POOF, x, y, z, dx * speed, 0.0, dz * speed);
            }
        }

        // Sparks -> outward bubbles.
        if (cfg.showUnderwaterSparks) {
            int sparkCount = Math.max(1, (int) (p * 6));
            for (int i = 0; i < sparkCount; i++) {
                double vx = (level.random.nextDouble() - 0.5) * p * 0.4;
                double vy = level.random.nextDouble() * p * 0.3;
                double vz = (level.random.nextDouble() - 0.5) * p * 0.4;
                addParticle(level, cfg, ParticleTypes.BUBBLE, x, y, z, vx, vy, vz);
            }
        }

        // Rising bubble plume: loop bubbleAmount times with the documented random
        // velocities. velX/velZ = rand(1..7) * 0.3 * rand{-1,0,1}; velY = rand(1..10) * 0.1.
        int bubbles = Math.max(0, cfg.bubbleAmount);
        for (int i = 0; i < bubbles; i++) {
            double vx = randRange(1, 7) * 0.3 * randSign();
            double vz = randRange(1, 7) * 0.3 * randSign();
            double vy = randRange(1, 10) * 0.1;
            addParticle(level, cfg, ParticleTypes.BUBBLE, x, y, z, vx, vy, vz);
        }
    }

    // -------------------------------------------------------------------------
    //  Shared helpers
    // -------------------------------------------------------------------------

    /**
     * The original's exact 6-particle mushroom-cloud layout:
     * <ul>
     *   <li>1 center particle, lower rise velocity {@code (power*0.25)/1.85}</li>
     *   <li>1 straight up</li>
     *   <li>4 diagonal: +x, -x, +z, -z</li>
     * </ul>
     * with {@code velY = power*0.4/1.85} and horizontal {@code xzVel = 0.15*power*0.5}.
     */
    private static void spawnMushroomCloud(Level level, ExplosiveConfig cfg,
                                           double x, double y, double z, float p,
                                           ParticleOptions smoke) {
        double velY = p * 0.4 / 1.85;
        double centerVelY = (p * 0.25) / 1.85;
        double xzVel = 0.15 * p * 0.5;

        // center (low rise)
        addParticle(level, cfg, smoke, x, y, z, 0.0, centerVelY, 0.0);
        // straight up
        addParticle(level, cfg, smoke, x, y, z, 0.0, velY, 0.0);
        // four diagonals
        addParticle(level, cfg, smoke, x, y, z, xzVel, velY, 0.0);   // +x
        addParticle(level, cfg, smoke, x, y, z, -xzVel, velY, 0.0);  // -x
        addParticle(level, cfg, smoke, x, y, z, 0.0, velY, xzVel);   // +z
        addParticle(level, cfg, smoke, x, y, z, 0.0, velY, -xzVel);  // -z
    }

    /**
     * Dispatch to the always-visible or normal particle path depending on the
     * {@code alwaysShow} config (the original's {@code isImportant}/{@code alwaysShow}
     * forced render on low particle settings).
     */
    private static void addParticle(Level level, ExplosiveConfig cfg, ParticleOptions particle,
                                    double x, double y, double z,
                                    double vx, double vy, double vz) {
        if (cfg.alwaysShow) {
            level.addAlwaysVisibleParticle(particle, x, y, z, vx, vy, vz);
        } else {
            level.addParticle(particle, x, y, z, vx, vy, vz);
        }
    }

    /** Inclusive random integer in [min, max]. */
    private static int randRange(int min, int max) {
        return min + (int) (Math.random() * (max - min + 1));
    }

    /** Returns one of -1, 0, or +1, matching the original's rand(-1..0..1) sign pick. */
    private static int randSign() {
        return (int) (Math.random() * 3) - 1;
    }
}
