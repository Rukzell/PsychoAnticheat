package com.psycho.services;

import com.psycho.Psycho;
import com.psycho.checks.Check;
import com.psycho.checks.CheckFactory;
import com.psycho.checks.impl.badpackets.BadPacketsA;
import com.psycho.checks.impl.combat.aim.*;
import com.psycho.checks.impl.combat.killaura.KillAuraInvalid;
import com.psycho.checks.impl.combat.killaura.KillAuraPattern;
import com.psycho.checks.impl.combat.killaura.KillAuraSnap;
import com.psycho.checks.impl.inventory.InventoryA;
import com.psycho.checks.impl.inventory.InventoryB;
import com.psycho.checks.impl.ml.AimML;
import com.psycho.checks.impl.sprint.SprintA;
import com.psycho.checks.impl.sprint.SprintB;
import com.psycho.checks.impl.sprint.SprintC;
import com.psycho.player.PsychoPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CheckService {
    private final Psycho plugin;
    private final List<CheckFactory> registeredFactories;

    public CheckService() {
        this.plugin = Psycho.get();
        this.registeredFactories = new ArrayList<>();
    }

    public void initialize() {
        registerFactories();
    }

    public void registerFactory(CheckFactory factory) {
        registeredFactories.add(factory);
    }

    public List<CheckFactory> getRegisteredFactories() {
        return Collections.unmodifiableList(registeredFactories);
    }

    public List<Check> createChecksForPlayer(PsychoPlayer player) {
        List<Check> checks = new ArrayList<>();
        for (CheckFactory factory : registeredFactories) {
            checks.add(factory.create(player));
        }
        return checks;
    }

    private void registerFactories() {
        ConfigService cfg = plugin.getConfigService();

        registerFactory(p -> new AimAxisLocking(p, "checks.aimassist.anglelocking", cfg.loadCheck("checks.aimassist.anglelocking", 10)));
        registerFactory(p -> new AimConsistency(p, "checks.aimassist.consistency", cfg.loadCheck("checks.aimassist.consistency", 10)));
        registerFactory(p -> new AimSpike(p, "checks.aimassist.spike", cfg.loadCheck("checks.aimassist.spike", 10)));
        registerFactory(p -> new AimDistribution(p, "checks.aimassist.distribution", cfg.loadCheck("checks.aimassist.distribution", 10)));
        registerFactory(p -> new AimSynthetic(p, "checks.aimassist.syntheticnoise", cfg.loadCheck("checks.aimassist.syntheticnoise", 10)));
        registerFactory(p -> new AimDynamics(p, "checks.aimassist.dynamics", cfg.loadCheck("checks.aimassist.dynamics", 10)));
        registerFactory(p -> new AimFrequency(p, "checks.aimassist.frequency", cfg.loadCheck("checks.aimassist.frequency", 10)));
        registerFactory(p -> new AimML(p, "checks.aimassist.ml", cfg.loadCheck("checks.aimassist.ml", 10)));
        registerFactory(p -> new BadPacketsA(p, "checks.badpackets.a", cfg.loadCheck("checks.badpackets.a", 10)));
        registerFactory(p -> new KillAuraSnap(p, "checks.killaura.snap", cfg.loadCheck("checks.killaura.snap", 10)));
        registerFactory(p -> new KillAuraInvalid(p, "checks.killaura.invalid", cfg.loadCheck("checks.killaura.invalid", 10)));
        registerFactory(p -> new KillAuraPattern(p, "checks.killaura.pattern", cfg.loadCheck("checks.killaura.pattern", 10)));
        registerFactory(p -> new SprintA(p, "checks.sprint.a", cfg.loadCheck("checks.sprint.a", 10)));
        registerFactory(p -> new SprintB(p, "checks.sprint.b", cfg.loadCheck("checks.sprint.b", 10)));
        registerFactory(p -> new SprintC(p, "checks.sprint.c", cfg.loadCheck("checks.sprint.c", 10)));
        registerFactory(p -> new InventoryA(p, "checks.inventory.a", cfg.loadCheck("checks.inventory.a", 10)));
        registerFactory(p -> new InventoryB(p, "checks.inventory.b", cfg.loadCheck("checks.inventory.b", 10)));
    }

    public void reload() {
        registeredFactories.clear();
        registerFactories();
    }
}