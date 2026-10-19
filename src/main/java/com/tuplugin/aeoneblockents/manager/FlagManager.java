package com.tuplugin.aeoneblockents.manager;

import com.tuplugin.aeoneblockents.DynamicEventsPlugin;

import java.util.*;

public class FlagManager {

    private final DynamicEventsPlugin plugin;

    private static final Map<String, List<String>> ALLOWED_FLAGS = new HashMap<>();

    static {
        ALLOWED_FLAGS.put("global", List.of("auto-scheduler-enabled", "interval-minutes", "min-players-required"));
        ALLOWED_FLAGS.put("acid_rain", List.of("duration-seconds", "damage", "protect-in-claims"));
        ALLOWED_FLAGS.put("blood_moon", List.of("duration-seconds", "prevent-sleep", "day-interval", "trigger-mode", "mob-spawning.interval-seconds", "mob-spawning.max-mobs-per-player", "mob-spawning.creeper-anti-grief"));
        ALLOWED_FLAGS.put("lucky_mining", List.of("duration-seconds", "multiplier", "bonus-diamonds", "bonus-diamonds-chance", "stack-with-fortune"));
        ALLOWED_FLAGS.put("lucky_harvest", List.of("duration-seconds", "multiplier", "auto-replant"));
    }

    public FlagManager(DynamicEventsPlugin plugin) {
        this.plugin = plugin;
    }

    public List<String> getAvailableTargets() {
        return new ArrayList<>(ALLOWED_FLAGS.keySet());
    }

    public List<String> getAvailableFlags(String target) {
        return ALLOWED_FLAGS.getOrDefault(target.toLowerCase(), Collections.emptyList());
    }

    public boolean setFlag(String target, String flag, String rawValue) {
        target = target.toLowerCase();
        List<String> validFlags = ALLOWED_FLAGS.get(target);
        if (validFlags == null || !validFlags.contains(flag.toLowerCase())) {
            return false;
        }

        String path = target.equals("global")
                ? "settings." + flag.toLowerCase()
                : "events." + target + "." + flag.toLowerCase();

        Object parsed = parseValue(rawValue);

        // Validación estricta para evitar divisiones por cero o valores negativos inválidos
        if (flag.contains("duration") || flag.contains("interval") || flag.contains("multiplier")) {
            if (parsed instanceof Number num && num.doubleValue() <= 0) {
                return false;
            }
        }

        plugin.getConfig().set(path, parsed);
        plugin.saveConfig();
        plugin.getEventManager().refreshConfigs();
        return true;
    }

    public String getFlagValue(String target, String flag) {
        target = target.toLowerCase();
        String path = target.equals("global")
                ? "settings." + flag.toLowerCase()
                : "events." + target + "." + flag.toLowerCase();
        Object val = plugin.getConfig().get(path);
        return val != null ? val.toString() : "no-definido";
    }

    private Object parseValue(String val) {
        if ("true".equalsIgnoreCase(val) || "false".equalsIgnoreCase(val)) {
            return Boolean.parseBoolean(val);
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException ignored) {}
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException ignored) {}
        return val;
    }
}
