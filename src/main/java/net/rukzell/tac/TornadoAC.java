package net.rukzell.tac;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import net.rukzell.tac.cfg.MessagesCfg;
import net.rukzell.tac.checks.impl.badpackets.BadPacketsA;
import net.rukzell.tac.checks.impl.combat.aim.AimAssistAngleLocking;
import net.rukzell.tac.checks.impl.combat.aim.AimAssistConsistency;
import net.rukzell.tac.checks.impl.combat.aim.AimAssistSpike;
import net.rukzell.tac.checks.impl.combat.killaura.KillAuraSnap;
import net.rukzell.tac.checks.impl.combat.killaura.KillAuraInvalid;
import net.rukzell.tac.checks.impl.inventory.InventoryA;
import net.rukzell.tac.checks.impl.sprint.SprintA;
import net.rukzell.tac.listeners.CheckListener;
import net.rukzell.tac.listeners.ConnectionListener;
import net.rukzell.tac.services.CheckService;
import net.rukzell.tac.services.CommandService;
import net.rukzell.tac.services.ConfigService;
import net.rukzell.tac.utils.Logger;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class TornadoAC extends JavaPlugin {
    private static TornadoAC instance;
    private ConnectionListener connectionListener;
    private CheckService checkService;
    private ConfigService configService;
    private CommandService commandService;
    private CheckListener checkListener;

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
    public void onLoad() {
       PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
       PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        create();
        saveDefaultConfig();

        // reg. commands
        Objects.requireNonNull(getCommand("tac")).setExecutor(commandService);
        Objects.requireNonNull(getCommand("tac")).setTabCompleter(commandService);

        // reg. bukkit event listeners
        getServer().getPluginManager().registerEvents(connectionListener, this);

        // reg. packetevents listeners
        PacketEvents.getAPI().getEventManager().registerListener(checkListener, PacketListenerPriority.NORMAL);
        PacketEvents.getAPI().init();
        Logger.log("TAC successfully loaded");
    }

    @Override
    public void onDisable() {
        Logger.log("TAC disabled");
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

    public static TornadoAC get() {
        return instance;
    }
}
