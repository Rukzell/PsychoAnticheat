package net.rukzell.tac.cfg;

public class CheckCfg {
    private int vlThreshold;
    private String punishCommand;
    private boolean enabled;

    public CheckCfg(int vlThreshold, String punishCommand, boolean enabled) {
        this.vlThreshold = vlThreshold;
        this.punishCommand = punishCommand;
        this.enabled = enabled;
    }

    public int vlThreshold() {
        return vlThreshold;
    }

    public String punishCommand() {
        return punishCommand;
    }

    public boolean enabled() {
        return enabled;
    }

    public void updateFromConfig(int vlThreshold, String punishCommand, boolean enabled) {
        this.vlThreshold = vlThreshold;
        this.punishCommand = punishCommand;
        this.enabled = enabled;
    }
}