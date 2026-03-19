package com.psycho.cfg;

public class CheckCfg {
    private int vlThreshold;
    private String punishCommand;
    private double decay;
    private double bufferThreshold;
    private boolean enabled;

    public CheckCfg(int vlThreshold, String punishCommand, double decay, double bufferThreshold, boolean enabled) {
        this.vlThreshold = vlThreshold;
        this.punishCommand = punishCommand;
        this.enabled = enabled;
        this.decay = decay;
        this.bufferThreshold = bufferThreshold;
    }

    public int vlThreshold() {
        return vlThreshold;
    }

    public double decay() {
        return decay;
    }

    public double bufferThreshold() {
        return bufferThreshold;
    }

    public String punishCommand() {
        return punishCommand;
    }

    public boolean enabled() {
        return enabled;
    }

    public void updateFromConfig(int vlThreshold, String punishCommand, double decay, double bufferThreshold, boolean enabled) {
        this.vlThreshold = vlThreshold;
        this.punishCommand = punishCommand;
        this.enabled = enabled;
        this.decay = decay;
        this.bufferThreshold = bufferThreshold;
    }
}