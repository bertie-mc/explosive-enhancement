package com.berlord.explosiveenhancement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplosionPolicyTest {

    @Test
    void selectsDynamicOrFixedPower() {
        assertEquals(2.5F, ExplosionPolicy.effectivePower(true, 2.5F));
        assertEquals(4.0F, ExplosionPolicy.effectivePower(false, 2.5F));
    }

    @Test
    void lowersOnlySmallExplosions() {
        assertEquals(9.5, ExplosionPolicy.centerY(10.0, 1.0F));
        assertEquals(10.5, ExplosionPolicy.centerY(10.0, 2.0F));
    }

    @Test
    void selectsTheEnvironmentSpecificVanillaParticleFlag() {
        assertTrue(ExplosionPolicy.keepDefaultParticle(false, true, false));
        assertFalse(ExplosionPolicy.keepDefaultParticle(false, false, true));
        assertTrue(ExplosionPolicy.keepDefaultParticle(true, false, true));
        assertFalse(ExplosionPolicy.keepDefaultParticle(true, true, false));
    }

    @Test
    void computesParticleCountsAtThresholds() {
        assertEquals(8, ExplosionPolicy.ringParticleCount(1.0F));
        assertEquals(16, ExplosionPolicy.ringParticleCount(4.0F));
        assertEquals(0, ExplosionPolicy.fireballParticleCount(1.99F));
        assertEquals(10, ExplosionPolicy.fireballParticleCount(2.0F));
        assertEquals(1, ExplosionPolicy.sparkParticleCount(0.0F));
        assertEquals(24, ExplosionPolicy.sparkParticleCount(4.0F));
        assertEquals(0, ExplosionPolicy.bubbleParticleCount(-1));
        assertEquals(50, ExplosionPolicy.bubbleParticleCount(50));
    }
}
