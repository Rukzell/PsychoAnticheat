package com.psycho.cfg;

public record MessagesCfg(String prefix, String noPermission, String alertMessage) {
    public String formatAlert(String player, String check, String vlBar, int vl, int maxVl, String info) {
        String msg = alertMessage
                .replace("{prefix}", prefix)
                .replace("{player}", player)
                .replace("{check}", check)
                .replace("{vlBar}", vlBar)
                .replace("{vl}", String.valueOf(vl))
                .replace("{maxVl}", String.valueOf(maxVl))
                .replace("{info}", info.isEmpty() ? "" : "§7(" + info + ")");
        return msg;
    }
}