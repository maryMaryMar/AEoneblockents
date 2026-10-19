package com.tuplugin.aeoneblockents.impl;

import com.tuplugin.aeoneblockents.DynamicEventsPlugin;
import com.tuplugin.aeoneblockents.manager.ServerEvent;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class BloodMoonEvent extends ServerEvent {

    private final NamespacedKey mobKey;
    private final boolean preventSleep;
    private final boolean creeperAntiGrief;
    private final int maxMobsPerPlayer;
    private final int spawnIntervalSeconds;
    private final int speedAmplifier;
    private final int strengthAmplifier;
    private final double healthMultiplier;
    private final double damageMultiplier;
    private final double maxArrowDamage;
    private int tickCounter = 0;

    public BloodMoonEvent(DynamicEventsPlugin plugin) {
        super(plugin, "blood_moon", "🩸 Luna de Sangre",
                plugin.getConfig().getInt("events.blood_moon.duration-seconds", 300),
                BossBar.Color.RED);
        this.mobKey = new NamespacedKey(plugin, "blood_mob");
        this.preventSleep = plugin.getConfig().getBoolean("events.blood_moon.prevent-sleep", true);
        this.creeperAntiGrief = plugin.getConfig().getBoolean("events.blood_moon.mob-spawning.creeper-anti-grief", true);
        this.maxMobsPerPlayer = plugin.getConfig().getInt("events.blood_moon.mob-spawning.max-mobs-per-player", 4);
        this.spawnIntervalSeconds = plugin.getConfig().getInt("events.blood_moon.mob-spawning.interval-seconds", 10);
        this.speedAmplifier = plugin.getConfig().getInt("events.blood_moon.mob-buffs.speed-amplifier", 1);
        this.strengthAmplifier = plugin.getConfig().getInt("events.blood_moon.mob-buffs.strength-amplifier", 0);
        this.healthMultiplier = plugin.getConfig().getDouble("events.blood_moon.mob-buffs.health-multiplier", 1.5);
        this.damageMultiplier = plugin.getConfig().getDouble("events.blood_moon.mob-buffs.damage-multiplier", 1.5);
        this.maxArrowDamage = plugin.getConfig().getDouble("events.blood_moon.mob-buffs.skeleton-max-arrow-damage", 4.5);
    }

    @Override
    public void onStart() {
        Bukkit.broadcast(Component.text("¡La Luna de Sangre despierta en OneBlock! Criaturas acechan tu isla.", NamedTextColor.DARK_RED));
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!isWorldDisabled(p.getWorld())) {
                p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 1.0f, 0.6f);
            }
        }
    }

    @Override
    public void onStop() {
        // Despawn limpio de todos los mobs de evento con efecto de humo
        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (isBloodMob(entity)) {
                    world.spawnParticle(Particle.LARGE_SMOKE, entity.getLocation().clone().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.05);
                    entity.remove();
                }
            }
        }
        Bukkit.broadcast(Component.text("La Luna de Sangre se disipa. La calma regresa a las islas.", NamedTextColor.GREEN));
    }

    @Override
    public void onTick() {
        tickCounter++;
        if (tickCounter % spawnIntervalSeconds != 0) return;

        // Invasión periódica segura por jugador
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isDead() || isWorldDisabled(player.getWorld())) continue;
            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) continue;

            int nearbyBloodMobs = countNearbyBloodMobs(player, 25);
            if (nearbyBloodMobs >= maxMobsPerPlayer) continue;

            spawnBloodMobNearPlayer(player);
        }
    }

    private int countNearbyBloodMobs(Player player, double radius) {
        int count = 0;
        for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof LivingEntity living && isBloodMob(living)) {
                count++;
            }
        }
        return count;
    }

    private void spawnBloodMobNearPlayer(Player player) {
        World world = player.getWorld();
        Location playerLoc = player.getLocation();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // Probabilidad de generar Phantom aéreo sobre la isla
        boolean allowPhantoms = plugin.getConfig().getBoolean("events.blood_moon.mob-spawning.allow-phantoms", true);
        double phantomChance = plugin.getConfig().getDouble("events.blood_moon.mob-spawning.phantom-chance", 0.25);
        if (allowPhantoms && random.nextDouble() < phantomChance) {
            Location airLoc = playerLoc.clone().add(random.nextInt(-8, 9), random.nextInt(7, 12), random.nextInt(-8, 9));
            if (airLoc.getBlock().getType().isAir()) {
                Phantom phantom = world.spawn(airLoc, Phantom.class);
                applyBloodBuffs(phantom);
                return;
            }
        }

        // Búsqueda de bloque sólido en anillo seguro de 4 a 12 bloques
        int minRadius = plugin.getConfig().getInt("events.blood_moon.mob-spawning.min-radius", 4);
        int maxRadius = plugin.getConfig().getInt("events.blood_moon.mob-spawning.max-radius", 12);

        for (int attempts = 0; attempts < 12; attempts++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double dist = minRadius + (maxRadius - minRadius) * random.nextDouble();
            int x = playerLoc.getBlockX() + (int) (dist * Math.cos(angle));
            int z = playerLoc.getBlockZ() + (int) (dist * Math.sin(angle));

            // Escanear en un rango vertical de +/- 4 bloques del jugador
            for (int dy = 3; dy >= -4; dy--) {
                int y = playerLoc.getBlockY() + dy;
                Block ground = world.getBlockAt(x, y, z);
                Block feet = world.getBlockAt(x, y + 1, z);
                Block head = world.getBlockAt(x, y + 2, z);

                if (ground.getType().isSolid() && feet.getType().isAir() && head.getType().isAir()
                        && ground.getType() != Material.LAVA && ground.getType() != Material.WATER
                        && ground.getType() != Material.CHEST && ground.getType() != Material.HOPPER) {

                    Location spawnLoc = new Location(world, x + 0.5, y + 1, z + 0.5);
                    spawnRandomIslandMob(spawnLoc);
                    return;
                }
            }
        }
    }

    private void spawnRandomIslandMob(Location loc) {
        World world = loc.getWorld();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double roll = random.nextDouble();

        LivingEntity entity;
        if (roll < 0.45) {
            entity = world.spawn(loc, Zombie.class);
        } else if (roll < 0.70) {
            entity = world.spawn(loc, Skeleton.class);
        } else if (roll < 0.85) {
            entity = world.spawn(loc, Spider.class);
        } else {
            entity = world.spawn(loc, Creeper.class);
        }

        applyBloodBuffs(entity);
    }

    private void applyBloodBuffs(LivingEntity entity) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(mobKey, PersistentDataType.BYTE, (byte) 1);

        entity.setCustomNameVisible(false);
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 600, speedAmplifier));
        if (strengthAmplifier >= 0) {
            entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 600, strengthAmplifier));
        }

        // Vida aumentada configurable
        AttributeInstance maxHealth = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(maxHealth.getBaseValue() * healthMultiplier);
            entity.setHealth(maxHealth.getValue());
        }

        // Daño de ataque base configurable
        AttributeInstance attackDamage = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (attackDamage != null) {
            attackDamage.setBaseValue(attackDamage.getBaseValue() * damageMultiplier);
        }

        // Partículas rojas al spawnear
        entity.getWorld().spawnParticle(Particle.DUST, entity.getLocation().clone().add(0, 1, 0),
                8, 0.3, 0.4, 0.3, new Particle.DustOptions(Color.fromRGB(220, 20, 20), 1.2f));
    }

    private boolean isBloodMob(LivingEntity entity) {
        return entity.getPersistentDataContainer().has(mobKey, PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        if (!creeperAntiGrief) return;
        // Anti-Grief: los creepers de Luna de Sangre causan daño a entidades pero NO destruyen bloques de la isla
        if (event.getEntity() instanceof LivingEntity living && isBloodMob(living)) {
            event.blockList().clear(); // Preserva 100% de los bloques
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Daño de flechas de esqueleto configurable para balancear knockback y peligro
        if (event.getDamager() instanceof Arrow arrow && arrow.getShooter() instanceof Skeleton skeleton) {
            if (isBloodMob(skeleton) && event.getEntity() instanceof Player) {
                event.setDamage(Math.min(event.getDamage(), maxArrowDamage));
            }
        } else if (event.getDamager() instanceof LivingEntity damager && isBloodMob(damager)) {
            // Asegurar escalado del multiplicador de daño en ataques cuerpo a cuerpo
            if (damageMultiplier > 1.0 && !(damager instanceof Creeper)) {
                event.setDamage(event.getDamage() * damageMultiplier);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMobDeath(EntityDeathEvent event) {
        if (isBloodMob(event.getEntity())) {
            event.setDroppedExp(event.getDroppedExp() * 2);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSleep(PlayerBedEnterEvent event) {
        if (isWorldDisabled(event.getPlayer().getWorld())) return;
        if (preventSleep) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("No puedes dormir durante la Luna de Sangre.", NamedTextColor.RED));
        }
    }
}
