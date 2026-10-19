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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class LuckyMiningEvent extends ServerEvent {

    private final int multiplier;
    private final int bonusDiamonds;
    private final double triggerChance;
    private final double diamondChance;
    private final boolean antiSilkTouch;
    private final boolean trackPlacedBlocks;

    public LuckyMiningEvent(DynamicEventsPlugin plugin) {
        super(plugin, "lucky_mining", "🌙 Luna de Suerte",
                plugin.getConfig().getInt("events.lucky_mining.duration-seconds", 300),
                BossBar.Color.YELLOW);
        this.multiplier = Math.max(1, plugin.getConfig().getInt("events.lucky_mining.multiplier", 4));
        this.bonusDiamonds = Math.max(1, plugin.getConfig().getInt("events.lucky_mining.bonus-diamonds", 2));
        this.triggerChance = plugin.getConfig().getDouble("events.lucky_mining.trigger-chance", 1.0);
        this.diamondChance = plugin.getConfig().getDouble("events.lucky_mining.bonus-diamonds-chance", 0.50);
        this.antiSilkTouch = plugin.getConfig().getBoolean("events.lucky_mining.anti-exploit.anti-silk-touch", true);
        this.trackPlacedBlocks = plugin.getConfig().getBoolean("events.lucky_mining.anti-exploit.track-placed-blocks", true);
    }

    @Override
    public void onStart() {
        Bukkit.broadcast(Component.text("¡La Luna de Suerte ha comenzado! Pica minerales para obtener x" + multiplier + " o bonus de +" + bonusDiamonds + " diamantes.", NamedTextColor.YELLOW));
    }

    @Override
    public void onStop() {
        Bukkit.broadcast(Component.text("La Luna de Suerte ha finalizado. Los minerales vuelven a su estado habitual.", NamedTextColor.GRAY));
    }

    @Override
    public void onTick() {
        // Evento reactivo a la interacción de minería
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDropItem(BlockDropItemEvent event) {
        if (isWorldDisabled(event.getBlock().getWorld())) return;

        Material blockType = event.getBlockState().getType();
        if (!isOre(blockType)) return;

        Player player = event.getPlayer();
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) return;

        // Anti-Exploit: Toque de Seda no activa multiplicador ni diamantes extra
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (antiSilkTouch && tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
            return;
        }

        // Anti-Exploit: Bloques colocados artificialmente no generan bonus
        if (trackPlacedBlocks && plugin.getEventListener() != null && plugin.getEventListener().isPlacedBlock(event.getBlock())) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();

        // Probabilidad general configurable de que el evento afecte este bloque
        if (triggerChance < 1.0 && random.nextDouble() >= triggerChance) {
            return;
        }

        boolean rollDiamonds = random.nextDouble() < diamondChance;

        if (rollDiamonds) {
            // Recompensa: Bonus de diamantes extra
            ItemStack extraDiamonds = new ItemStack(Material.DIAMOND, bonusDiamonds);
            Item diamondItem = event.getBlock().getWorld().dropItem(event.getBlock().getLocation().clone().add(0.5, 0.5, 0.5), extraDiamonds);
            event.getItems().add(diamondItem);

            player.sendActionBar(Component.text("¡Luna de Suerte: +" + bonusDiamonds + " Diamantes extra!", NamedTextColor.AQUA));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.8f);
        } else {
            // Recompensa: Multiplicador apilable en drops
            for (Item itemEntity : event.getItems()) {
                ItemStack stack = itemEntity.getItemStack();
                int newAmount = Math.min(stack.getMaxStackSize(), stack.getAmount() * multiplier);
                stack.setAmount(newAmount);
                itemEntity.setItemStack(stack);
            }

            player.sendActionBar(Component.text("¡Luna de Suerte: Multiplicador x" + multiplier + " en drops!", NamedTextColor.GOLD));
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block b : event.getBlocks()) {
            if (isOre(b.getType())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block b : event.getBlocks()) {
            if (isOre(b.getType())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private boolean isOre(Material mat) {
        if (mat == null) return false;
        String name = mat.name();
        return name.endsWith("_ORE") || mat == Material.ANCIENT_DEBRIS;
    }
}
