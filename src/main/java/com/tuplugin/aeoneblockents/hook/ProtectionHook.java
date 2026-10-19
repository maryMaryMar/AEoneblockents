package com.tuplugin.aeoneblockents.hook;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.logging.Level;

public class ProtectionHook {

    private boolean griefPreventionEnabled = false;
    private Object gpDataStore = null;
    private Method gpGetClaimAt = null;

    private boolean worldGuardEnabled = false;
    private Method wgGetInstance = null;
    private Method wgGetPlatform = null;
    private Method wgGetRegionContainer = null;
    private Method wgContainerGet = null;
    private Method wgGetApplicableRegions = null;
    private Method wgSizeMethod = null;
    private Method wgBukkitAdapterAdapt = null;

    private boolean bentoBoxEnabled = false;
    private Method bbGetInstance = null;
    private Method bbGetIslands = null;
    private Method bbGetIslandAt = null;

    public ProtectionHook() {
        initGriefPrevention();
        initWorldGuard();
        initBentoBox();
    }

    private void initGriefPrevention() {
        if (Bukkit.getPluginManager().isPluginEnabled("GriefPrevention")) {
            try {
                Class<?> gpClass = Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");
                Object gpInstance = gpClass.getField("instance").get(null);
                if (gpInstance != null) {
                    Class<?> claimClass = Class.forName("me.ryanhamshire.GriefPrevention.Claim");
                    this.gpDataStore = gpClass.getField("dataStore").get(gpInstance);
                    this.gpGetClaimAt = gpDataStore.getClass().getMethod("getClaimAt", Location.class, boolean.class, claimClass);
                    this.griefPreventionEnabled = true;
                    Bukkit.getLogger().info("[AEoneblockents] Hook con GriefPrevention inicializado.");
                }
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.WARNING, "[AEoneblockents] No se pudo inicializar hook con GriefPrevention: " + e.getMessage());
            }
        }
    }

    private void initWorldGuard() {
        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            try {
                Class<?> wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
                Class<?> platformClass = Class.forName("com.sk89q.worldguard.internal.platform.WorldGuardPlatform");
                Class<?> regionContainerClass = Class.forName("com.sk89q.worldguard.protection.regions.RegionContainer");
                Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
                Class<?> worldEditWorldClass = Class.forName("com.sk89q.worldedit.world.World");
                Class<?> blockVector3Class = Class.forName("com.sk89q.worldedit.math.BlockVector3");
                Class<?> regionQueryClass = Class.forName("com.sk89q.worldguard.protection.regions.RegionQuery");

                this.wgGetInstance = wgClass.getMethod("getInstance");
                this.wgGetPlatform = wgClass.getMethod("getPlatform");
                this.wgGetRegionContainer = platformClass.getMethod("getRegionContainer");
                this.wgContainerGet = regionContainerClass.getMethod("get", worldEditWorldClass);
                this.wgBukkitAdapterAdapt = bukkitAdapterClass.getMethod("adapt", org.bukkit.World.class);

                this.worldGuardEnabled = true;
                Bukkit.getLogger().info("[AEoneblockents] Hook con WorldGuard inicializado.");
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.WARNING, "[AEoneblockents] No se pudo inicializar hook con WorldGuard: " + e.getMessage());
            }
        }
    }

    private void initBentoBox() {
        if (Bukkit.getPluginManager().isPluginEnabled("BentoBox")) {
            try {
                Class<?> bbClass = Class.forName("world.bentobox.bentobox.BentoBox");
                this.bbGetInstance = bbClass.getMethod("getInstance");
                Object bbInstance = this.bbGetInstance.invoke(null);
                this.bbGetIslands = bbClass.getMethod("getIslands");
                Object islandsManager = this.bbGetIslands.invoke(bbInstance);
                this.bbGetIslandAt = islandsManager.getClass().getMethod("getIslandAt", Location.class);
                this.bentoBoxEnabled = true;
                Bukkit.getLogger().info("[AEoneblockents] Hook con BentoBox (OneBlock) inicializado.");
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.INFO, "[AEoneblockents] BentoBox no detectado o API interna no enlazada.");
            }
        }
    }

    public boolean isProtected(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;

        // 1. Chequeo GriefPrevention
        if (griefPreventionEnabled && gpDataStore != null && gpGetClaimAt != null) {
            try {
                Object claim = gpGetClaimAt.invoke(gpDataStore, loc, false, null);
                if (claim != null) return true;
            } catch (Exception ignored) {}
        }

        // 2. Chequeo WorldGuard
        if (worldGuardEnabled && wgGetInstance != null) {
            try {
                Object wgInstance = wgGetInstance.invoke(null);
                Object platform = wgGetPlatform.invoke(wgInstance);
                Object container = wgGetRegionContainer.invoke(platform);
                Object adaptedWorld = wgBukkitAdapterAdapt.invoke(null, loc.getWorld());
                Object regionManager = wgContainerGet.invoke(container, adaptedWorld);

                if (regionManager != null) {
                    Method getApplicable = regionManager.getClass().getMethod("getApplicableRegions",
                            Class.forName("com.sk89q.worldedit.math.BlockVector3"));
                    Class<?> blockVector3Class = Class.forName("com.sk89q.worldedit.math.BlockVector3");
                    Method atMethod = blockVector3Class.getMethod("at", double.class, double.class, double.class);
                    Object vec = atMethod.invoke(null, loc.getX(), loc.getY(), loc.getZ());
                    Object set = getApplicable.invoke(regionManager, vec);
                    if (set != null) {
                        Method sizeMethod = set.getClass().getMethod("size");
                        int size = (int) sizeMethod.invoke(set);
                        if (size > 0) return true;
                    }
                }
            } catch (Exception ignored) {}
        }

        // 3. Chequeo BentoBox (Isla protegida de OneBlock)
        if (bentoBoxEnabled && bbGetInstance != null && bbGetIslands != null && bbGetIslandAt != null) {
            try {
                Object bbInstance = bbGetInstance.invoke(null);
                Object islandsManager = bbGetIslands.invoke(bbInstance);
                Object islandOpt = bbGetIslandAt.invoke(islandsManager, loc);
                if (islandOpt instanceof java.util.Optional<?> opt) {
                    if (opt.isPresent()) return true;
                }
            } catch (Exception ignored) {}
        }

        return false;
    }
}
