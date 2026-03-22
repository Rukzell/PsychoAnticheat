package com.psycho.ml.gru;

import java.io.*;

/**
 * Per-feature Z-score normalizer.
 * <p>
 * Usage:
 * FeatureNormalizer norm = new FeatureNormalizer(6);
 * norm.fit(sequences);                // считаем mean/std по тренировочному набору
 * double[][][] trainNorm = norm.transform(sequences);
 * norm.save(new File("norm.bin"));
 * <p>
 * // inference
 * FeatureNormalizer norm = FeatureNormalizer.load(new File("norm.bin"));
 * double[][] seqNorm = norm.transform(seq);
 */
public class FeatureNormalizer {

    private static final int VERSION = 1;
    private final int features;
    private final double[] mean;
    private final double[] std;
    private boolean fitted = false;

    // ── Fit ─────────────────────────────────────────────────────────────────

    public FeatureNormalizer(int features) {
        this.features = features;
        this.mean = new double[features];
        this.std = new double[features];
    }

    // ── Transform ────────────────────────────────────────────────────────────

    public static FeatureNormalizer load(File file) throws IOException {
        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            int version = dis.readInt();
            if (version != VERSION)
                throw new IOException("Unsupported normalizer version: " + version);
            int n = dis.readInt();
            FeatureNormalizer fn = new FeatureNormalizer(n);
            for (int f = 0; f < n; f++) fn.mean[f] = dis.readDouble();
            for (int f = 0; f < n; f++) fn.std[f] = dis.readDouble();
            fn.fitted = true;
            return fn;
        }
    }

    /**
     * Compute mean and std over ALL timesteps of ALL sequences.
     */
    public void fit(double[][][] sequences) {
        long[] count = new long[features];
        double[] sum = new double[features];
        double[] sum2 = new double[features];

        for (double[][] seq : sequences) {
            for (double[] x : seq) {
                for (int f = 0; f < features; f++) {
                    sum[f] += x[f];
                    sum2[f] += x[f] * x[f];
                    count[f]++;
                }
            }
        }

        for (int f = 0; f < features; f++) {
            mean[f] = sum[f] / count[f];
            double variance = sum2[f] / count[f] - mean[f] * mean[f];
            std[f] = Math.sqrt(variance + 1e-8);   // eps чтобы не делить на 0
        }

        fitted = true;
    }

    /**
     * Normalize entire dataset. Returns new array, исходный не меняет.
     */
    public double[][][] transform(double[][][] sequences) {
        checkFitted();
        double[][][] out = new double[sequences.length][][];
        for (int i = 0; i < sequences.length; i++)
            out[i] = transform(sequences[i]);
        return out;
    }

    // ── Save / Load ──────────────────────────────────────────────────────────

    /**
     * Normalize a single sequence (для inference).
     */
    public double[][] transform(double[][] seq) {
        checkFitted();
        double[][] out = new double[seq.length][features];
        for (int t = 0; t < seq.length; t++)
            for (int f = 0; f < features; f++)
                out[t][f] = (seq[t][f] - mean[f]) / std[f];
        return out;
    }

    public void transform(double[][] seq, double[][] out) {
        checkFitted();
        for (int t = 0; t < seq.length; t++)
            for (int f = 0; f < features; f++)
                out[t][f] = (seq[t][f] - mean[f]) / std[f];
    }

    public void save(File file) throws IOException {
        checkFitted();
        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)))) {
            dos.writeInt(VERSION);
            dos.writeInt(features);
            for (int f = 0; f < features; f++) dos.writeDouble(mean[f]);
            for (int f = 0; f < features; f++) dos.writeDouble(std[f]);
            dos.flush();
        }
    }

    // ── Debug ────────────────────────────────────────────────────────────────

    public void printStats() {
        System.out.printf("%-12s %10s %10s%n", "Feature", "Mean", "Std");
        String[] names = {"deltaYaw", "deltaPitch", "accelYaw", "accelPitch", "jerkYaw", "jerkPitch"};
        for (int f = 0; f < features; f++) {
            String name = (f < names.length) ? names[f] : ("f" + f);
            System.out.printf("%-12s %10.4f %10.4f%n", name, mean[f], std[f]);
        }
    }

    private void checkFitted() {
        if (!fitted) throw new IllegalStateException("Call fit() before transform()");
    }
}