package net.rukzell.tac;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
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
import net.rukzell.tac.services.ConfigService;
import net.rukzell.tac.utils.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public final class TornadoAC extends JavaPlugin {
    private static TornadoAC instance;
    private ConnectionListener connectionListener;
    private CheckService checkService;
    private ConfigService configService;
    private CheckListener checkListener;

    private void registerChecks() {
        checkService.registerCheck(new AimAssistAngleLocking(configService.loadCheck("checks.aimassist.anglelocking", 10)));
        checkService.registerCheck(new AimAssistConsistency(configService.loadCheck("checks.aimassist.consistency", 10)));
        checkService.registerCheck(new AimAssistSpike(configService.loadCheck("checks.aimassist.spike", 10)));
        checkService.registerCheck(new BadPacketsA(configService.loadCheck("checks.badpackets.a", 10)));
        checkService.registerCheck(new KillAuraSnap(configService.loadCheck("checks.killaura.snap", 10)));
        checkService.registerCheck(new KillAuraInvalid(configService.loadCheck("checks.killaura.invalid", 10)));
        checkService.registerCheck(new SprintA(configService.loadCheck("checks.sprint.a", 10)));
        checkService.registerCheck(new InventoryA(configService.loadCheck("checks.inventory.a", 10)));
    }

    private void create() {
        instance = this;
        connectionListener = new ConnectionListener();
        checkService = new CheckService();
        configService = new ConfigService(this);
        checkListener = new CheckListener();

        registerChecks();
    }

    @Override
    public void onEnable() {
        create();
        saveDefaultConfig();

        // reg. bukkit event listeners
        getServer().getPluginManager().registerEvents(connectionListener, this);

        // reg. packetevents listeners
        PacketEvents.getAPI().getEventManager().registerListener(checkListener, PacketListenerPriority.NORMAL);

        Logger.log("TAC successfully loaded");
    }

    @Override
    public void onDisable() {
        Logger.log("TAC disabled");
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
