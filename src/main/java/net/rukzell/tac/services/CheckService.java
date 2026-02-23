package net.rukzell.tac.services;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import net.rukzell.tac.player.TornadoPlayer;
import net.rukzell.tac.checks.Check;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CheckService {
    private final List<Check> registeredChecks;

    public CheckService() {
        this.registeredChecks = new ArrayList<>();
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

    public void handleChecks(TornadoPlayer player, PacketReceiveEvent event) {
        registeredChecks.forEach(check -> check.handle(player, event));
    }
}