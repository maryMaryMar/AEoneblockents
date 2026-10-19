package com.tuplugin.aeoneblockents.impl;

import com.tuplugin.aeoneblockents.DynamicEventsPlugin;
import com.tuplugin.aeoneblockents.manager.ServerEvent;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class LuckyHarvestEvent extends ServerEvent {

    private final int multiplier;
    private final double triggerChance;
    private final boolean autoReplant;
    private final double bonusCropChance;
    private final int bonusCropAmount;

    private static final Set<Material> VALID_CROPS = Set.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES,
            Material.BEETROOTS, Material.NETHER_WART, Material.COCOA
    );

    public LuckyHarvestEvent(DynamicEventsPlugin plugin) {
        super(plugin, "lucky_harvest", "🌾 Cosecha con Suerte",
                plugin.getConfig().getInt("events.lucky_harvest.duration-seconds", 300),
                BossBar.Color.GREEN);
        this.multiplier = Math.max(1, plugin.getConfig().getInt("events.lucky_harvest.multiplier", 3));
        this.triggerChance = plugin.getConfig().getDouble("events.lucky_harvest.trigger-chance", 1.0);
        this.autoReplant = plugin.getConfig().getBoolean("events.lucky_harvest.auto-replant", true);
        this.bonusCropChance = plugin.getConfig().getDouble("events.lucky_harvest.bonus-crop-chance", 0.35);
        this.bonusCropAmount = Math.max(1, plugin.getConfig().getInt("events.lucky_harvest.bonus-crop-amount", 2));
    }

    @Override
    public void onStart() {
        Bukkit.broadcast(Component.text("¡Cosecha con Suerte activada! Los cultivos rinden x" + multiplier + (autoReplant ? " con auto-replantado." : "."), NamedTextColor.GREEN));
    }

    @Override
    public void onStop() {
        Bukkit.broadcast(Component.text("La Cosecha con Suerte ha terminado.", NamedTextColor.GRAY));
    }

    @Override
    public void onTick() {
        // Evento reactivo a cosechas
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDropItem(BlockDropItemEvent event) {
        if (isWorldDisabled(event.getBlock().getWorld())) return;

        Block block = event.getBlock();
        Material blockType = event.getBlockState().getType();
        if (!VALID_CROPS.contains(blockType)) return;

        BlockData data = event.getBlockState().getBlockData();
        if (!(data instanceof Ageable ageable) || ageable.getAge() < ageable.getMaximumAge()) {
            return;
        }

        Player player = event.getPlayer();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // Probabilidad configurable de activación en la cosecha
        if (triggerChance < 1.0 && random.nextDouble() >= triggerChance) {
            return;
        }

        // Multiplicador de cosechas
        for (Item itemEntity : event.getItems()) {
            ItemStack stack = itemEntity.getItemStack();
            int newAmount = Math.min(stack.getMaxStackSize(), stack.getAmount() * multiplier);
            stack.setAmount(newAmount);
            itemEntity.setItemStack(stack);
        }

        // Probabilidad de cultivo extra adicional
        if (bonusCropChance > 0.0 && random.nextDouble() < bonusCropChance) {
            ItemStack bonusItem = new ItemStack(blockType == Material.WHEAT ? Material.BREAD : blockType, bonusCropAmount);
            Item extra = block.getWorld().dropItem(block.getLocation().clone().add(0.5, 0.5, 0.5), bonusItem);
            event.getItems().add(extra);
        }

        player.sendActionBar(Component.text("¡Cosecha x" + multiplier + " con Suerte!", NamedTextColor.GREEN));
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.4f);

        // Auto-replantado inteligente verificado en Farmland / Soul Sand
        if (autoReplant) {
            Block ground = block.getRelative(0, -1, 0);
            Material groundType = ground.getType();

            boolean validSoil = (blockType == Material.NETHER_WART && groundType == Material.SOUL_SAND)
                    || (blockType != Material.NETHER_WART && groundType == Material.FARMLAND);

            if (validSoil) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    block.setType(blockType);
                    BlockData newBlockData = block.getBlockData();
                    if (newBlockData instanceof Ageable newAgeable) {
                        newAgeable.setAge(0);
                        block.setBlockData(newAgeable);
                    }
                });
            }
        }
    }
}
