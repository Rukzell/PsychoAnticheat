package com.psycho;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.psycho.cfg.MessagesCfg;
import com.psycho.listeners.CheckListener;
import com.psycho.listeners.ConnectionListener;
import com.psycho.services.CheckService;
import com.psycho.services.CommandService;
import com.psycho.services.ConfigService;
import com.psycho.utils.Logger;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class Psycho extends JavaPlugin {
    private static Psycho instance;
    private ConnectionListener connectionListener;
    private CheckService checkService;
    private ConfigService configService;
    private CommandService commandService;
    private CheckListener checkListener;

    public static Psycho get() {
        return instance;
    }

    private void create() {
        instance = this;
        connectionListener = new ConnectionListener();
        checkService = new CheckService();
        configService = new ConfigService(this);
        commandService = new CommandService(this);
        checkListener = new CheckListener();

        checkService.initialize();
    }

    @Override
    public void onEnable() {
        create();
        saveDefaultConfig();

        new File(getDataFolder(), "ml").mkdirs();

        PacketEvents.getAPI().init();
        PacketEvents.getAPI().load();

        // reg. commands
        Objects.requireNonNull(getCommand("psycho")).setExecutor(commandService);
        Objects.requireNonNull(getCommand("psycho")).setTabCompleter(commandService);

        // reg. bukkit event listeners
        getServer().getPluginManager().registerEvents(connectionListener, this);

        // reg. packetevents listeners
        PacketEvents.getAPI().getEventManager().registerListener(checkListener, PacketListenerPriority.NORMAL);

        getServer().dispatchCommand(getServer().getConsoleSender(), "psycho reload");
        Logger.log("Psycho successfully loaded");
    }

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
    }

    @Override
    public void onDisable() {
        Logger.log("Psycho disabled");
        PacketEvents.getAPI().terminate();
    }

    public MessagesCfg getMessagesCfg() {
        return configService.getMessagesCfg();
    }

    public CheckService getCheckService() {
        return checkService;
    }

    public ConfigService getConfigService() {
        return configService;
    }

    public ConnectionListener getConnectionListener() {
        return connectionListener;
    }
}
