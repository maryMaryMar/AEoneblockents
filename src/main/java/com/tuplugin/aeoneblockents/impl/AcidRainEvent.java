package com.tuplugin.aeoneblockents.impl;

import com.tuplugin.aeoneblockents.DynamicEventsPlugin;
import com.tuplugin.aeoneblockents.manager.ServerEvent;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Player;

import java.util.Set;

public class AcidRainEvent extends ServerEvent {

    private final double damage;
    private final int damageIntervalSeconds;
    private final boolean protectInClaims;
    private final boolean bypassArmor;
    private int tickCounter = 0;

    private static final Set<Biome> DRY_BIOMES = Set.of(
            Biome.DESERT, Biome.BADLANDS, Biome.ERODED_BADLANDS,
            Biome.WOODED_BADLANDS, Biome.SAVANNA, Biome.SAVANNA_PLATEAU,
            Biome.WINDSWEPT_SAVANNA
    );

    public AcidRainEvent(DynamicEventsPlugin plugin) {
        super(plugin, "acid_rain", "🌧️ Lluvia Ácida",
                plugin.getConfig().getInt("events.acid_rain.duration-seconds", 180),
                BossBar.Color.GREEN);
        this.damage = plugin.getConfig().getDouble("events.acid_rain.damage", 1.0);
        this.damageIntervalSeconds = Math.max(1, plugin.getConfig().getInt("events.acid_rain.damage-interval-seconds", 1));
        this.protectInClaims = plugin.getConfig().getBoolean("events.acid_rain.protect-in-claims", false);
        this.bypassArmor = plugin.getConfig().getBoolean("events.acid_rain.bypass-armor", true);
    }

    @Override
    public void onStart() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == World.Environment.NORMAL && !isWorldDisabled(world)) {
                world.setStorm(true);
                world.setWeatherDuration(durationSeconds * 20);
            }
        }
        Bukkit.broadcast(Component.text("¡Ha comenzado una Lluvia Ácida! Busca refugio bajo techo en tu isla.", NamedTextColor.GREEN));
    }

    @Override
    public void onStop() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == World.Environment.NORMAL && !isWorldDisabled(world)) {
                world.setStorm(false);
            }
        }
        Bukkit.broadcast(Component.text("La Lluvia Ácida ha cesado. El aire vuelve a ser respirable.", NamedTextColor.AQUA));
    }

    @Override
    public void onTick() {
        tickCounter++;
        // Frecuencia configurable en segundos (1 tick de ServerEvent = 1 segundo)
        if (tickCounter % damageIntervalSeconds != 0) return;

        DamageSource damageSource = null;
        if (bypassArmor) {
            try {
                damageSource = DamageSource.builder(DamageType.MAGIC).build();
            } catch (Throwable ignored) {
                damageSource = null;
            }
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.isDead() || isWorldDisabled(p.getWorld())) continue;
            if (p.getWorld().getEnvironment() != World.Environment.NORMAL) continue;
            if (p.getGameMode() == org.bukkit.GameMode.CREATIVE || p.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;

            Location loc = p.getLocation();
            if (DRY_BIOMES.contains(loc.getBlock().getBiome())) continue;

            if (protectInClaims && plugin.getProtectionHook() != null && plugin.getProtectionHook().isProtected(loc)) {
                continue;
            }

            int highestY = p.getWorld().getHighestBlockYAt(loc, HeightMap.MOTION_BLOCKING);
            if (loc.getBlockY() >= highestY) {
                // Expuesto a la intemperie: daño ambiental directo
                if (damageSource != null) {
                    p.damage(damage, damageSource);
                } else {
                    p.damage(damage);
                }
                p.getWorld().spawnParticle(Particle.SNEEZE, loc.clone().add(0, 1.2, 0), 6, 0.3, 0.5, 0.3, 0.05);
                p.playSound(loc, Sound.BLOCK_LAVA_EXTINGUISH, 0.3f, 1.6f);
            }
        }
    }
}
