package com.psycho.utils;

import com.psycho.Psycho;

public class Logger {
    public static void log(String message) {
        Psycho.get().getLogger().info(message);
    }

    public static void warn(String message) {
        Psycho.get().getLogger().warning(message);
    }

    public static void error(String message) {
        Psycho.get().getLogger().severe(message);
    }
}
