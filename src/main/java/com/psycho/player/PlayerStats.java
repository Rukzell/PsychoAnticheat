package com.psycho.player;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PlayerStats {
    private final PsychoPlayer player;
    private List<String> failedChecks;

    public PlayerStats(PsychoPlayer player) {
        this.player = player;
        this.failedChecks = new ArrayList<>();
    }
}
