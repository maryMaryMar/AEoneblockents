package com.tuplugin.aeoneblockents.manager;

import com.tuplugin.aeoneblockents.DynamicEventsPlugin;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.List;

public abstract class ServerEvent implements Listener {

    protected final DynamicEventsPlugin plugin;
    protected final String id;
    protected final String displayName;
    protected final int durationSeconds;
    protected int remainingSeconds;
    protected boolean active = false;
    protected BossBar bossBar;

    public ServerEvent(DynamicEventsPlugin plugin, String id, String displayName, int durationSeconds, BossBar.Color barColor) {
        this.plugin = plugin;
        this.id = id;
        this.displayName = displayName;
        this.durationSeconds = durationSeconds;
        this.remainingSeconds = durationSeconds;
        this.bossBar = BossBar.bossBar(
                Component.text(displayName + " - " + formatTime(remainingSeconds), NamedTextColor.GOLD),
                1.0f,
                barColor,
                BossBar.Overlay.PROGRESS
        );
    }

    public void start() {
        this.active = true;
        this.remainingSeconds = durationSeconds;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isWorldDisabled(player.getWorld())) {
                player.showBossBar(bossBar);
            }
        }
        onStart();
    }

    public void stop() {
        this.active = false;
        HandlerList.unregisterAll(this);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.hideBossBar(bossBar);
        }
        onStop();
    }

    public void tick() {
        if (!active) return;
        remainingSeconds--;

        float progress = Math.max(0.0f, Math.min(1.0f, (float) remainingSeconds / (float) durationSeconds));
        bossBar.progress(progress);
        bossBar.name(Component.text(displayName + " - " + formatTime(remainingSeconds), NamedTextColor.GOLD));

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isWorldDisabled(player.getWorld())) {
                player.hideBossBar(bossBar);
            } else {
                player.showBossBar(bossBar);
            }
        }
        onTick();
    }

    public abstract void onStart();
    public abstract void onStop();
    public abstract void onTick();

    public boolean isWorldDisabled(World world) {
        if (world == null) return true;
        List<String> disabled = plugin.getConfig().getStringList("settings.disabled-worlds");
        return disabled.contains(world.getName());
    }

    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getRemainingSeconds() { return remainingSeconds; }
    public boolean isActive() { return active; }
}
