package com.berlord.explosiveenhancement;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple GSON-backed config, mirroring the original mod's {@code ExplosiveConfig}.
 *
 * <p>Stored at {@code config/explosiveenhancement.json}. Every conceptual effect from
 * the original is exposed as a boolean toggle, plus the {@code bubbleAmount} count and
 * the {@code dynamicSize}/{@code alwaysShow}/{@code modEnabled} global switches. The
 * file is loaded once at mod construction; if it is missing or malformed it is
 * (re)written with defaults.
 */
public class ExplosiveConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ExplosiveConfig INSTANCE = new ExplosiveConfig();

    // ---- Global ---------------------------------------------------------------
    /** Master switch; when false the mod stays completely out of the way of vanilla. */
    public boolean modEnabled = true;
    /** Scale every effect by explosion power; when false a fixed power of 4 is used. */
    public boolean dynamicSize = true;
    /** Force-render every spawned particle even on the "Minimal" particle setting. */
    public boolean alwaysShow = false;

    // ---- Vanilla suppression --------------------------------------------------
    /** Keep vanilla's single central explosion particle in addition to ours. */
    public boolean showDefaultExplosion = false;
    /** Keep vanilla's central particle for underwater explosions. */
    public boolean showDefaultExplosionUnderwater = false;
    /**
     * Keep vanilla's post-explosion per-block smoke. NOTE: on 1.21.1 that smoke is
     * driven by a separate server-side level event, not by the client explosion packet
     * this mod hooks, so toggling this currently has no effect — kept for config
     * compatibility with the original.
     */
    public boolean showDefaultSmoke = false;

    // ---- Surface effects ------------------------------------------------------
    /** The flat expanding shock ring at the explosion center. */
    public boolean showBlastWave = true;
    /** The bright central flash/fireball. */
    public boolean showFireball = true;
    /** Outward spark streaks. */
    public boolean showSparks = true;
    /** The 6-particle rising mushroom cloud of smoke. */
    public boolean showMushroomCloud = true;

    // ---- Underwater effects ---------------------------------------------------
    /** Swap surface effects for the underwater variants when exploding in water. */
    public boolean underwaterExplosions = true;
    /** The underwater shock ring (replaces the blast wave underwater). */
    public boolean showUnderwaterBlastWave = true;
    /** The central shockwave puff (replaces the fireball underwater). */
    public boolean showShockwave = true;
    /** Outward bubble sparks (replaces the sparks underwater). */
    public boolean showUnderwaterSparks = true;
    /** How many rising bubbles to emit for an underwater explosion. */
    public int bubbleAmount = 50;

    // --------------------------------------------------------------------------

    public static ExplosiveConfig get() {
        return INSTANCE;
    }

    private static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve(ExplosiveEnhancement.MOD_ID + ".json");
    }

    /** Load from disk, or write defaults if absent/corrupt. */
    public static void load() {
        Path file = path();
        try {
            if (Files.exists(file)) {
                String json = Files.readString(file);
                ExplosiveConfig parsed = GSON.fromJson(json, ExplosiveConfig.class);
                if (parsed != null) {
                    INSTANCE = parsed;
                }
                // Re-save so any newly added fields get written back with their defaults.
                save();
            } else {
                save();
            }
        } catch (Exception e) {
            ExplosiveEnhancement.LOGGER.warn("Failed to read {} — using defaults.", file, e);
            INSTANCE = new ExplosiveConfig();
        }
    }

    /** Persist the current instance to disk. */
    public static void save() {
        Path file = path();
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            ExplosiveEnhancement.LOGGER.warn("Failed to write {}.", file, e);
        }
    }
}
