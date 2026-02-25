package net.rukzell.tac.services;

import net.rukzell.tac.TornadoAC;
import net.rukzell.tac.checks.impl.badpackets.BadPacketsA;
import net.rukzell.tac.checks.impl.combat.aim.AimAssistAngleLocking;
import net.rukzell.tac.checks.impl.combat.aim.AimAssistConsistency;
import net.rukzell.tac.checks.impl.combat.aim.AimAssistDistribution;
import net.rukzell.tac.checks.impl.combat.aim.AimAssistSpike;
import net.rukzell.tac.checks.impl.combat.killaura.KillAuraInvalid;
import net.rukzell.tac.checks.impl.combat.killaura.KillAuraPattern;
import net.rukzell.tac.checks.impl.combat.killaura.KillAuraSnap;
import net.rukzell.tac.checks.impl.inventory.InventoryA;
import net.rukzell.tac.checks.impl.sprint.SprintA;
import net.rukzell.tac.checks.impl.sprint.SprintB;
import net.rukzell.tac.checks.Check;
import net.rukzell.tac.checks.impl.sprint.SprintC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CheckService {
    private final TornadoAC plugin;
    private final List<Check> registeredChecks;

    public CheckService() {
        this.plugin = TornadoAC.get();
        this.registeredChecks = new ArrayList<>();
    }

    public void initialize() {
        registerChecks();
    }

    public void registerCheck(Check check) {
        if (!registeredChecks.contains(check)) {
            registeredChecks.add(check);
        }
    }

    public void unregisterCheck(Check check) {
        registeredChecks.remove(check);
    }

    public List<Check> getRegisteredChecks() {
        return Collections.unmodifiableList(registeredChecks);
    }

    private void registerChecks() {
        ConfigService cfg = plugin.getConfigService();

        registerCheck(new AimAssistAngleLocking("checks.aimassist.anglelocking", cfg.loadCheck("checks.aimassist.anglelocking", 10)));
        registerCheck(new AimAssistConsistency("checks.aimassist.consistency", cfg.loadCheck("checks.aimassist.consistency", 10)));
        registerCheck(new AimAssistSpike("checks.aimassist.spike", cfg.loadCheck("checks.aimassist.spike", 10)));
        registerCheck(new AimAssistDistribution("checks.aimassist.distribution", cfg.loadCheck("checks.aimassist.distribution", 10)));
        registerCheck(new BadPacketsA("checks.badpackets.a", cfg.loadCheck("checks.badpackets.a", 10)));
        registerCheck(new KillAuraSnap("checks.killaura.snap", cfg.loadCheck("checks.killaura.snap", 10)));
        registerCheck(new KillAuraInvalid("checks.killaura.invalid", cfg.loadCheck("checks.killaura.invalid", 10)));
        registerCheck(new KillAuraPattern("checks.killaura.pattern", cfg.loadCheck("checks.killaura.pattern", 10)));
        registerCheck(new SprintA("checks.sprint.a", cfg.loadCheck("checks.sprint.a", 10)));
        registerCheck(new SprintB("checks.sprint.b", cfg.loadCheck("checks.sprint.b", 10)));
        registerCheck(new SprintC("checks.sprint.c", cfg.loadCheck("checks.sprint.c", 10)));
        registerCheck(new InventoryA("checks.inventory.a", cfg.loadCheck("checks.inventory.a", 10)));
    }

    public void reload() {
        registeredChecks.clear();
        registerChecks();
    }
}