package com.tuplugin.aeoneblockents;

import com.tuplugin.aeoneblockents.commands.EventCommand;
import com.tuplugin.aeoneblockents.hook.ProtectionHook;
import com.tuplugin.aeoneblockents.listener.EventListener;
import com.tuplugin.aeoneblockents.manager.EventManager;
import com.tuplugin.aeoneblockents.manager.FlagManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class DynamicEventsPlugin extends JavaPlugin {

    private EventManager eventManager;
    private FlagManager flagManager;
    private ProtectionHook protectionHook;
    private EventListener eventListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.protectionHook = new ProtectionHook();
        this.eventManager = new EventManager(this);
        this.flagManager = new FlagManager(this);
        this.eventListener = new EventListener(eventManager);

        EventCommand cmd = new EventCommand(eventManager, flagManager, this);
        Objects.requireNonNull(getCommand("aeoneblockents")).setExecutor(cmd);
        Objects.requireNonNull(getCommand("aeoneblockents")).setTabCompleter(cmd);

        getServer().getPluginManager().registerEvents(eventListener, this);

        getLogger().info("AEoneblockents 1.0.0 cargado con éxito para OneBlock.");
    }

    @Override
    public void onDisable() {
        if (eventManager != null) {
            eventManager.shutdown();
        }
        getLogger().info("AEoneblockents 1.0.0 deshabilitado correctamente.");
    }

    public void reloadPlugin() {
        reloadConfig();
        if (eventManager != null) {
            eventManager.reload();
        }
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public FlagManager getFlagManager() {
        return flagManager;
    }

    public ProtectionHook getProtectionHook() {
        return protectionHook;
    }

    public EventListener getEventListener() {
        return eventListener;
    }
}
