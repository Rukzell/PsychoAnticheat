package com.psycho.cfg;

public class CheckCfg {
    private double probThreshold;
    private int vlThreshold;
    private String punishCommand;
    private double decay;
    private double bufferThreshold;
    private boolean enabled;
    private long vlDecayInterval;

    public CheckCfg(int vlThreshold, String punishCommand, double decay, double bufferThreshold, double probThreshold, boolean enabled, long vlDecayInterval) {
        this.vlThreshold = vlThreshold;
        this.punishCommand = punishCommand;
        this.enabled = enabled;
        this.decay = decay;
        this.bufferThreshold = bufferThreshold;
        this.probThreshold = probThreshold;
        this.vlDecayInterval = vlDecayInterval;
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

    public double probThreshold() {
        return probThreshold;
    }

    public String punishCommand() {
        return punishCommand;
    }

    public boolean enabled() {
        return enabled;
    }

    public long vlDecayInterval() {
        return vlDecayInterval;
    }

    public void updateFromConfig(int vlThreshold, String punishCommand, double decay, double bufferThreshold, double probThreshold, boolean enabled, long vlDecayInterval) {
        this.vlThreshold = vlThreshold;
        this.punishCommand = punishCommand;
        this.enabled = enabled;
        this.decay = decay;
        this.bufferThreshold = bufferThreshold;
        this.probThreshold = probThreshold;
        this.vlDecayInterval = vlDecayInterval;
    }
}