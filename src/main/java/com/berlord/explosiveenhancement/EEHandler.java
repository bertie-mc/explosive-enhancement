package com.berlord.explosiveenhancement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;

/** Spawns the configured surface or underwater particle composition. */
public final class EEHandler {

    private EEHandler() {
    }

    public static void spawnEnhancedParticles(Level level, double x, double y, double z, float power) {
        ExplosiveConfig cfg = ExplosiveConfig.get();
        float p = ExplosionPolicy.effectivePower(cfg.dynamicSize, power);
        double centerY = ExplosionPolicy.centerY(y, power);

        boolean underwater = cfg.underwaterExplosions
                && level.getFluidState(BlockPos.containing(x, y, z)).is(FluidTags.WATER);

        if (underwater) {
            spawnUnderwater(level, cfg, x, centerY, z, p);
        } else {
            spawnSurface(level, cfg, x, centerY, z, p);
        }
    }

    private static void spawnSurface(Level level, ExplosiveConfig cfg, double x, double y, double z, float p) {
        if (cfg.showBlastWave) {
            addParticle(level, cfg, ParticleTypes.FLASH, x, y, z, 1.0, 0.0, 0.0);

            double radius = p * 0.75;
            int ringCount = ExplosionPolicy.ringParticleCount(p);
            for (int i = 0; i < ringCount; i++) {
                double angle = (Math.PI * 2.0) * i / ringCount;
                double dx = Math.cos(angle);
                double dz = Math.sin(angle);
                double speed = (p * 1.75) * 0.02;
                addParticle(level, cfg, ParticleTypes.POOF,
                        x + dx * radius, y, z + dz * radius,
                        dx * speed, 0.0, dz * speed);
            }
        }

        if (cfg.showFireball) {
            addParticle(level, cfg, ParticleTypes.FLASH, x, y, z, 0.0, 0.0, 0.0);
            int fireballCount = ExplosionPolicy.fireballParticleCount(p);
            if (fireballCount > 0) {
                for (int i = 0; i < fireballCount; i++) {
                    double vx = (level.random.nextDouble() - 0.5) * p * 0.25;
                    double vy = (level.random.nextDouble() - 0.2) * p * 0.2;
                    double vz = (level.random.nextDouble() - 0.5) * p * 0.25;
                    addParticle(level, cfg, ParticleTypes.FLAME, x, y, z, vx, vy, vz);
                    addParticle(level, cfg, ParticleTypes.POOF, x, y, z, vx * 0.6, vy * 0.6, vz * 0.6);
                }
            }
        }

        if (cfg.showSparks) {
            int sparkCount = ExplosionPolicy.sparkParticleCount(p);
            for (int i = 0; i < sparkCount; i++) {
                double vx = (level.random.nextDouble() - 0.5) * p * 0.4;
                double vy = level.random.nextDouble() * p * 0.3;
                double vz = (level.random.nextDouble() - 0.5) * p * 0.4;
                addParticle(level, cfg, ParticleTypes.LAVA, x, y, z, vx, vy, vz);
            }
        }

        if (cfg.showMushroomCloud) {
            spawnMushroomCloud(level, cfg, x, y, z, p, ParticleTypes.LARGE_SMOKE);
        }
    }

    private static void spawnUnderwater(Level level, ExplosiveConfig cfg, double x, double y, double z, float p) {
        if (cfg.showUnderwaterBlastWave) {
            double radius = p * 0.75;
            int ringCount = ExplosionPolicy.ringParticleCount(p);
            for (int i = 0; i < ringCount; i++) {
                double angle = (Math.PI * 2.0) * i / ringCount;
                double dx = Math.cos(angle);
                double dz = Math.sin(angle);
                addParticle(level, cfg, ParticleTypes.BUBBLE_COLUMN_UP,
                        x + dx * radius, y, z + dz * radius,
                        0.0, 0.0, 0.0);
            }
        }

        if (cfg.showShockwave) {
            addParticle(level, cfg, ParticleTypes.FLASH, x, y, z, 0.0, 0.0, 0.0);
            int shockCount = ExplosionPolicy.ringParticleCount(p);
            for (int i = 0; i < shockCount; i++) {
                double angle = (Math.PI * 2.0) * i / shockCount;
                double dx = Math.cos(angle);
                double dz = Math.sin(angle);
                double speed = (p * 1.25) * 0.02;
                addParticle(level, cfg, ParticleTypes.POOF, x, y, z, dx * speed, 0.0, dz * speed);
            }
        }

        if (cfg.showUnderwaterSparks) {
            int sparkCount = ExplosionPolicy.sparkParticleCount(p);
            for (int i = 0; i < sparkCount; i++) {
                double vx = (level.random.nextDouble() - 0.5) * p * 0.4;
                double vy = level.random.nextDouble() * p * 0.3;
                double vz = (level.random.nextDouble() - 0.5) * p * 0.4;
                addParticle(level, cfg, ParticleTypes.BUBBLE, x, y, z, vx, vy, vz);
            }
        }

        int bubbles = ExplosionPolicy.bubbleParticleCount(cfg.bubbleAmount);
        for (int i = 0; i < bubbles; i++) {
            double vx = randRange(1, 7) * 0.3 * randSign();
            double vz = randRange(1, 7) * 0.3 * randSign();
            double vy = randRange(1, 10) * 0.1;
            addParticle(level, cfg, ParticleTypes.BUBBLE, x, y, z, vx, vy, vz);
        }
    }

    /** Six rising particles: one slow center, one vertical, and four diagonal. */
    private static void spawnMushroomCloud(Level level, ExplosiveConfig cfg,
                                           double x, double y, double z, float p,
                                           ParticleOptions smoke) {
        double velY = p * 0.4 / 1.85;
        double centerVelY = (p * 0.25) / 1.85;
        double xzVel = 0.15 * p * 0.5;

        addParticle(level, cfg, smoke, x, y, z, 0.0, centerVelY, 0.0);
        addParticle(level, cfg, smoke, x, y, z, 0.0, velY, 0.0);
        addParticle(level, cfg, smoke, x, y, z, xzVel, velY, 0.0);
        addParticle(level, cfg, smoke, x, y, z, -xzVel, velY, 0.0);
        addParticle(level, cfg, smoke, x, y, z, 0.0, velY, xzVel);
        addParticle(level, cfg, smoke, x, y, z, 0.0, velY, -xzVel);
    }

    private static void addParticle(Level level, ExplosiveConfig cfg, ParticleOptions particle,
                                    double x, double y, double z,
                                    double vx, double vy, double vz) {
        if (cfg.alwaysShow) {
            level.addAlwaysVisibleParticle(particle, x, y, z, vx, vy, vz);
        } else {
            level.addParticle(particle, x, y, z, vx, vy, vz);
        }
    }

    private static int randRange(int min, int max) {
        return min + (int) (Math.random() * (max - min + 1));
    }

    private static int randSign() {
        return (int) (Math.random() * 3) - 1;
    }
}
