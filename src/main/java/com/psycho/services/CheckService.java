package com.psycho.services;

import com.psycho.Psycho;
import com.psycho.checks.Check;
import com.psycho.checks.CheckFactory;
import com.psycho.checks.impl.badpackets.BadPacketsA;
import com.psycho.checks.impl.badpackets.BadPacketsB;
import com.psycho.checks.impl.badpackets.BadPacketsC;
import com.psycho.checks.impl.combat.aim.*;
import com.psycho.checks.impl.combat.aim.ml.AimAssistML;
import com.psycho.checks.impl.combat.killaura.*;
import com.psycho.checks.impl.inventory.InventoryA;
import com.psycho.checks.impl.inventory.InventoryB;
import com.psycho.checks.impl.movement.NoSlow;
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

        registerFactory(p -> new AimAssistA(p, "checks.aimassist.a", cfg.loadCheck("checks.aimassist.a", 10)));
        registerFactory(p -> new AimAssistB(p, "checks.aimassist.b", cfg.loadCheck("checks.aimassist.b", 10)));
        registerFactory(p -> new AimAssistC(p, "checks.aimassist.c", cfg.loadCheck("checks.aimassist.c", 10)));
        registerFactory(p -> new AimAssistD(p, "checks.aimassist.d", cfg.loadCheck("checks.aimassist.d", 10)));
        registerFactory(p -> new AimAssistE(p, "checks.aimassist.e", cfg.loadCheck("checks.aimassist.e", 10)));
        registerFactory(p -> new AimAssistF(p, "checks.aimassist.f", cfg.loadCheck("checks.aimassist.f", 10)));
        registerFactory(p -> new AimAssistG(p, "checks.aimassist.g", cfg.loadCheck("checks.aimassist.g", 10)));
        registerFactory(p -> new AimAssistML(p, "checks.aimassist.ml", cfg.loadCheck("checks.aimassist.ml", 10)));
        registerFactory(p -> new BadPacketsA(p, "checks.badpackets.a", cfg.loadCheck("checks.badpackets.a", 10)));
        registerFactory(p -> new BadPacketsB(p, "checks.badpackets.b", cfg.loadCheck("checks.badpackets.b", 10)));
        registerFactory(p -> new BadPacketsC(p, "checks.badpackets.c", cfg.loadCheck("checks.badpackets.c", 10)));
        registerFactory(p -> new KillAuraA(p, "checks.killaura.a", cfg.loadCheck("checks.killaura.a", 10)));
        registerFactory(p -> new KillAuraB(p, "checks.killaura.b", cfg.loadCheck("checks.killaura.b", 10)));
        registerFactory(p -> new KillAuraC(p, "checks.killaura.c", cfg.loadCheck("checks.killaura.c", 10)));
        registerFactory(p -> new KillAuraD(p, "checks.killaura.d", cfg.loadCheck("checks.killaura.d", 10)));
        registerFactory(p -> new KillAuraE(p, "checks.killaura.e", cfg.loadCheck("checks.killaura.e", 10)));
        registerFactory(p -> new KillAuraF(p, "checks.killaura.f", cfg.loadCheck("checks.killaura.f", 10)));
        registerFactory(p -> new NoSlow(p, "checks.movement.noslow", cfg.loadCheck("checks.movement.noslow", 10)));
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
