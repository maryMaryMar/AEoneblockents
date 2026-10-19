package com.tuplugin.aeoneblockents.manager;

import com.tuplugin.aeoneblockents.DynamicEventsPlugin;
import com.tuplugin.aeoneblockents.impl.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public class EventManager {

    private final DynamicEventsPlugin plugin;
    private final Map<String, Supplier<ServerEvent>> factories = new LinkedHashMap<>();
    private ServerEvent activeEvent = null;
    private BukkitTask tickTask;
    private BukkitTask autoStartTask;
    private BukkitTask bloodMoonCheckerTask;

    public EventManager(DynamicEventsPlugin plugin) {
        this.plugin = plugin;
        registerFactories();
        startSchedulers();
    }

    private void registerFactories() {
        factories.clear();
        factories.put("acid_rain", () -> new AcidRainEvent(plugin));
        factories.put("blood_moon", () -> new BloodMoonEvent(plugin));
        factories.put("lucky_mining", () -> new LuckyMiningEvent(plugin));
        factories.put("lucky_harvest", () -> new LuckyHarvestEvent(plugin));
    }

    public void startSchedulers() {
        cancelSchedulers();

        // 1. Reloj de 1 segundo (20 ticks) para actualizar tiempo y tick de evento activo
        this.tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (activeEvent != null && activeEvent.isActive()) {
                if (activeEvent.getRemainingSeconds() <= 0) {
                    stopActiveEvent();
                } else {
                    activeEvent.tick();
                }
            }
        }, 20L, 20L);

        // 2. Scheduler aleatorio periódico para OneBlock
        if (plugin.getConfig().getBoolean("settings.auto-scheduler-enabled", true)) {
            long intervalMinutes = Math.max(1L, plugin.getConfig().getLong("settings.interval-minutes", 45L));
            long intervalTicks = intervalMinutes * 60L * 20L;

            this.autoStartTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                int minPlayers = plugin.getConfig().getInt("settings.min-players-required", 1);
                if (activeEvent == null && Bukkit.getOnlinePlayers().size() >= minPlayers) {
                    List<String> eventList = new ArrayList<>(factories.keySet());
                    if ("DAY_INTERVAL".equalsIgnoreCase(plugin.getConfig().getString("events.blood_moon.trigger-mode", "DAY_INTERVAL"))) {
                        eventList.remove("blood_moon");
                    }
                    if (!eventList.isEmpty()) {
                        String randomId = eventList.get(ThreadLocalRandom.current().nextInt(eventList.size()));
                        startEvent(randomId);
                    }
                }
            }, intervalTicks, intervalTicks);
        }

        // 3. Verificador de Luna de Sangre cada 10 días en OneBlock
        if ("DAY_INTERVAL".equalsIgnoreCase(plugin.getConfig().getString("events.blood_moon.trigger-mode", "DAY_INTERVAL"))) {
            int dayInterval = Math.max(1, plugin.getConfig().getInt("events.blood_moon.day-interval", 10));
            this.bloodMoonCheckerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (activeEvent != null) return;
                World world = Bukkit.getWorlds().stream()
                        .filter(w -> w.getEnvironment() == World.Environment.NORMAL)
                        .findFirst().orElse(null);
                if (world == null) return;

                long fullTime = world.getFullTime();
                long currentDay = fullTime / 24000L;
                long dayTime = world.getTime();

                if (currentDay > 0 && currentDay % dayInterval == 0 && dayTime >= 13000L && dayTime <= 13200L) {
                    startEvent("blood_moon");
                }
            }, 100L, 100L);
        }
    }

    public void cancelSchedulers() {
        if (tickTask != null) tickTask.cancel();
        if (autoStartTask != null) autoStartTask.cancel();
        if (bloodMoonCheckerTask != null) bloodMoonCheckerTask.cancel();
    }

    public void shutdown() {
        stopActiveEvent();
        cancelSchedulers();
    }

    public void reload() {
        stopActiveEvent();
        startSchedulers();
    }

    public void refreshConfigs() {
        startSchedulers();
    }

    public boolean startEvent(String id) {
        if (activeEvent != null) return false;
        Supplier<ServerEvent> supplier = factories.get(id.toLowerCase());
        if (supplier == null) return false;

        this.activeEvent = supplier.get();
        this.activeEvent.start();
        return true;
    }

    public boolean stopActiveEvent() {
        if (activeEvent == null) return false;
        activeEvent.stop();
        activeEvent = null;
        return true;
    }

    public ServerEvent getActiveEvent() { return activeEvent; }
    public Set<String> getEventIds() { return Collections.unmodifiableSet(factories.keySet()); }
}
