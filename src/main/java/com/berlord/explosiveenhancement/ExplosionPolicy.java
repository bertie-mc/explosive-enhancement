package com.berlord.explosiveenhancement;

/** Dependency-free decisions shared by the packet hook and particle renderer. */
public final class ExplosionPolicy {

    private ExplosionPolicy() {
    }

    public static float effectivePower(boolean dynamicSize, float packetPower) {
        return dynamicSize ? packetPower : 4.0F;
    }

    public static double centerY(double packetY, float packetPower) {
        return packetY + (packetPower == 1.0F ? -0.5 : 0.5);
    }

    public static boolean keepDefaultParticle(
            boolean underwater, boolean showSurfaceDefault, boolean showUnderwaterDefault) {
        return underwater ? showUnderwaterDefault : showSurfaceDefault;
    }

    public static int ringParticleCount(float power) {
        return Math.max(8, (int) (power * 4));
    }

    public static int fireballParticleCount(float power) {
        return power < 2.0F ? 0 : Math.max(8, (int) (power * 5));
    }

    public static int sparkParticleCount(float power) {
        return Math.max(1, (int) (power * 6));
    }

    public static int bubbleParticleCount(int configuredAmount) {
        return Math.max(0, configuredAmount);
    }
}
