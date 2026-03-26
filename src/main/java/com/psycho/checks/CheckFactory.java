package com.psycho.checks;

import com.psycho.player.PsychoPlayer;

@FunctionalInterface
public interface CheckFactory {
    Check create(PsychoPlayer player);
}
