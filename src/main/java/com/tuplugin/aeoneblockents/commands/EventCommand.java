package com.tuplugin.aeoneblockents.commands;

import com.tuplugin.aeoneblockents.DynamicEventsPlugin;
import com.tuplugin.aeoneblockents.manager.EventManager;
import com.tuplugin.aeoneblockents.manager.FlagManager;
import com.tuplugin.aeoneblockents.manager.ServerEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class EventCommand implements CommandExecutor, TabCompleter {

    private final EventManager manager;
    private final FlagManager flagManager;
    private final DynamicEventsPlugin plugin;

    public EventCommand(EventManager manager, FlagManager flagManager, DynamicEventsPlugin plugin) {
        this.manager = manager;
        this.flagManager = flagManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> handleStart(sender, args);
            case "stop" -> handleStop(sender);
            case "status" -> handleStatus(sender);
            case "reload" -> handleReload(sender);
            case "flag" -> handleFlag(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleStart(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Uso: /aeo start <evento>", NamedTextColor.RED));
            return;
        }
        String id = args[1].toLowerCase();
        if (manager.getActiveEvent() != null) {
            sender.sendMessage(Component.text("Ya hay un evento en curso: " + manager.getActiveEvent().getDisplayName(), NamedTextColor.RED));
            return;
        }
        if (manager.startEvent(id)) {
            sender.sendMessage(Component.text("Evento '" + id + "' iniciado con éxito en OneBlock.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Evento '" + id + "' no encontrado. Válidos: " + String.join(", ", manager.getEventIds()), NamedTextColor.RED));
        }
    }

    private void handleStop(CommandSender sender) {
        if (manager.stopActiveEvent()) {
            sender.sendMessage(Component.text("Evento activo detenido manualmente.", NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Component.text("No hay ningún evento activo actualmente.", NamedTextColor.RED));
        }
    }

    private void handleStatus(CommandSender sender) {
        ServerEvent active = manager.getActiveEvent();
        if (active != null && active.isActive()) {
            sender.sendMessage(Component.text("=== Evento en curso ===", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("Nombre: " + active.getDisplayName(), NamedTextColor.AQUA));
            sender.sendMessage(Component.text("Restante: " + active.getRemainingSeconds() + " segundos", NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Component.text("No hay ningún evento activo en OneBlock.", NamedTextColor.GRAY));
        }
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadPlugin();
        sender.sendMessage(Component.text("Configuración de AEoneblockents recargada con éxito.", NamedTextColor.GREEN));
    }

    private void handleFlag(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Uso: /aeo flag <evento|global> <flag> [valor]", NamedTextColor.RED));
            return;
        }

        String target = args[1].toLowerCase();
        String flag = args[2].toLowerCase();

        if (args.length == 3) {
            String currentVal = flagManager.getFlagValue(target, flag);
            sender.sendMessage(Component.text("Flag [" + target + " -> " + flag + "] = " + currentVal, NamedTextColor.AQUA));
            return;
        }

        String rawVal = args[3];
        if (flagManager.setFlag(target, flag, rawVal)) {
            sender.sendMessage(Component.text("Flag [" + target + " -> " + flag + "] actualizada a: " + rawVal, NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Error: Flag inválida o valor rechazado (use true/false o números positivos).", NamedTextColor.RED));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== Comandos de AEoneblockents ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/aeo start <evento> - Inicia un evento", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/aeo stop - Detiene el evento activo", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/aeo status - Estado del evento actual", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/aeo reload - Recarga la configuración", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/aeo flag <target> <flag> [valor] - Configuración dinámica", NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> sub = List.of("start", "stop", "status", "reload", "flag");
            StringUtil.copyPartialMatches(args[0], sub, completions);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            StringUtil.copyPartialMatches(args[1], manager.getEventIds(), completions);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("flag")) {
            StringUtil.copyPartialMatches(args[1], flagManager.getAvailableTargets(), completions);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("flag")) {
            StringUtil.copyPartialMatches(args[2], flagManager.getAvailableFlags(args[1]), completions);
        } else if (args.length == 4 && args[0].equalsIgnoreCase("flag")) {
            completions.addAll(List.of("true", "false"));
        }

        Collections.sort(completions);
        return completions;
    }
}
