package net.rukzell.tac.utils;

import net.rukzell.tac.TornadoAC;

public class Logger {
    public static void log(String message) {
        TornadoAC.get().getLogger().info(message);
    }

    public static void warn(String message) {
        TornadoAC.get().getLogger().warning(message);
    }

    public static void error(String message) {
        TornadoAC.get().getLogger().severe(message);
    }
}
