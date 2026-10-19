package com.tuplugin.aeoneblockents.listener;

import com.tuplugin.aeoneblockents.manager.EventManager;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class EventListener implements Listener {

    private final EventManager eventManager;
    private final Map<Location, Long> placedBlocks;

    public EventListener(EventManager eventManager) {
        this.eventManager = eventManager;
        // Cache LRU en memoria con límite de 50,000 bloques colocados
        this.placedBlocks = Collections.synchronizedMap(new LinkedHashMap<>(2048, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Location, Long> eldest) {
                return size() > 50000;
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        placedBlocks.put(event.getBlock().getLocation(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        placedBlocks.remove(event.getBlock().getLocation());
    }

    public boolean isPlacedBlock(Block block) {
        if (block == null) return false;
        return placedBlocks.containsKey(block.getLocation());
    }
}
