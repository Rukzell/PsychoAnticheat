package com.psycho.ml.gru;

import java.io.*;
import java.util.*;

/**
 * Gated Recurrent Unit (GRU)
 *
 * Forward pass equations:
 *   z_t  = sigmoid(Wxz * x_t + Whz * h_{t-1} + bz)       — update gate
 *   r_t  = sigmoid(Wxr * x_t + Whr * h_{t-1} + br)       — reset gate
 *   h̃_t  = tanh   (Wxh * x_t + Whh * (r_t ⊙ h_{t-1}) + bh) — candidate
 *   h_t  = (1 − z_t) ⊙ h_{t-1} + z_t ⊙ h̃_t              — new hidden
 *   y    = sigmoid(Why * h_T + by)                         — output
 */
public class GRU {

    private final int inputSize;
    private final int hiddenSize;
    private final int outputSize;

    // ── Hidden (candidate) ──────────────────────────────────────────────────
    private double[][] Wxh;   // [hiddenSize][inputSize]
    private double[][] Whh;   // [hiddenSize][hiddenSize]
    private double[]   bh;    // [hiddenSize]

    // ── Update gate (z) ─────────────────────────────────────────────────────
    private double[][] Wxz;
    private double[][] Whz;
    private double[]   bz;

    // ── Reset gate (r) ──────────────────────────────────────────────────────
    private double[][] Wxr;
    private double[][] Whr;
    private double[]   br;

    // ── Output layer ────────────────────────────────────────────────────────
    private double[][] Why;   // [outputSize][hiddenSize]
    private double[]   by;    // [outputSize]

    // ── Adam hyper-params ───────────────────────────────────────────────────
    private final double gradientClipping = 5.0;
    private final double beta1 = 0.9;
    private final double beta2 = 0.999;
    private final double eps   = 1e-8;
    private int adamT = 0;

    // ── Adam moments (m = 1st, v = 2nd) ────────────────────────────────────
    private double[][] mWxh, vWxh;
    private double[][] mWhh, vWhh;
    private double[]   mbh,  vbh;

    private double[][] mWxz, vWxz;
    private double[][] mWhz, vWhz;
    private double[]   mbz,  vbz;

    private double[][] mWxr, vWxr;
    private double[][] mWhr, vWhr;
    private double[]   mbr,  vbr;

    private double[][] mWhy, vWhy;
    private double[]   mby,  vby;

    private final Random rnd = new Random();

    // ════════════════════════════════════════════════════════════════════════
    //  Construction
    // ════════════════════════════════════════════════════════════════════════

    public GRU(int inputSize, int hiddenSize, int outputSize) {
        this.inputSize  = inputSize;
        this.hiddenSize = hiddenSize;
        this.outputSize = outputSize;

        // Xavier scale
        double r = Math.sqrt(2.0 / (inputSize + hiddenSize));

        Wxh = randomMatrix(hiddenSize, inputSize,  r);
        Whh = randomMatrix(hiddenSize, hiddenSize, r);
        bh  = new double[hiddenSize];

        Wxz = randomMatrix(hiddenSize, inputSize,  r);
        Whz = randomMatrix(hiddenSize, hiddenSize, r);
        bz  = new double[hiddenSize];

        Wxr = randomMatrix(hiddenSize, inputSize,  r);
        Whr = randomMatrix(hiddenSize, hiddenSize, r);
        br  = new double[hiddenSize];

        Why = randomMatrix(outputSize, hiddenSize, r);
        by  = new double[outputSize];

        // Adam moments – all zeros by default
        mWxh = new double[hiddenSize][inputSize];  vWxh = new double[hiddenSize][inputSize];
        mWhh = new double[hiddenSize][hiddenSize]; vWhh = new double[hiddenSize][hiddenSize];
        mbh  = new double[hiddenSize];             vbh  = new double[hiddenSize];

        mWxz = new double[hiddenSize][inputSize];  vWxz = new double[hiddenSize][inputSize];
        mWhz = new double[hiddenSize][hiddenSize]; vWhz = new double[hiddenSize][hiddenSize];
        mbz  = new double[hiddenSize];             vbz  = new double[hiddenSize];

        mWxr = new double[hiddenSize][inputSize];  vWxr = new double[hiddenSize][inputSize];
        mWhr = new double[hiddenSize][hiddenSize]; vWhr = new double[hiddenSize][hiddenSize];
        mbr  = new double[hiddenSize];             vbr  = new double[hiddenSize];

        mWhy = new double[outputSize][hiddenSize]; vWhy = new double[outputSize][hiddenSize];
        mby  = new double[outputSize];             vby  = new double[outputSize];
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Inference
    // ════════════════════════════════════════════════════════════════════════

    /** Run the GRU over a variable-length sequence and return the output vector. */
    public double[] forward(double[][] sequence) {
        double[] h = new double[hiddenSize];

        for (double[] x : sequence) {
            h = gruStep(x, h);
        }

        double[] y = new double[outputSize];
        for (int i = 0; i < outputSize; i++) {
            double sum = by[i];
            for (int j = 0; j < hiddenSize; j++)
                sum += Why[i][j] * h[j];
            y[i] = sigmoid(sum);
        }
        return y;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Training  (mini-batch SGD, one sample at a time, Adam optimizer)
    // ════════════════════════════════════════════════════════════════════════

    public void train(double[][][] sequences, double[][] targets,
                      double lr, int epochs) {
        long positives = 0;
        for (double[] t : targets) if (t[0] == 1.0) positives++;
        long negatives = targets.length - positives;
        double total = targets.length;
        double posWeight = (positives > 0) ? total / (2.0 * positives) : 1.0;
        double negWeight = (negatives > 0) ? total / (2.0 * negatives) : 1.0;

        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < sequences.length; i++) order.add(i);

        for (int epoch = 0; epoch < epochs; epoch++) {
            Collections.shuffle(order, rnd);
            double totalLoss = 0.0;

            for (int idx : order) {
                double[][] seq    = sequences[idx];
                double[]   target = targets[idx];
                int        T      = seq.length;

                // ── Forward pass ───────────────────────────────
                double[][] hs  = new double[T + 1][hiddenSize];
                double[][] zs  = new double[T][hiddenSize];
                double[][] rs  = new double[T][hiddenSize];
                double[][] hcs = new double[T][hiddenSize];
                double[][] rhs = new double[T][hiddenSize];

                for (int t = 0; t < T; t++) {
                    double[] x     = seq[t];
                    double[] hPrev = hs[t];

                    for (int i = 0; i < hiddenSize; i++) {
                        double pz = bz[i];
                        for (int j = 0; j < inputSize;  j++) pz += Wxz[i][j] * x[j];
                        for (int j = 0; j < hiddenSize; j++) pz += Whz[i][j] * hPrev[j];
                        zs[t][i] = sigmoid(pz);

                        double pr = br[i];
                        for (int j = 0; j < inputSize;  j++) pr += Wxr[i][j] * x[j];
                        for (int j = 0; j < hiddenSize; j++) pr += Whr[i][j] * hPrev[j];
                        rs[t][i] = sigmoid(pr);

                        rhs[t][i] = rs[t][i] * hPrev[i];
                    }

                    for (int i = 0; i < hiddenSize; i++) {
                        double ph = bh[i];
                        for (int j = 0; j < inputSize;  j++) ph += Wxh[i][j] * x[j];
                        for (int j = 0; j < hiddenSize; j++) ph += Whh[i][j] * rhs[t][j];
                        hcs[t][i] = Math.tanh(ph);
                        hs[t + 1][i] = (1.0 - zs[t][i]) * hs[t][i] + zs[t][i] * hcs[t][i];
                    }
                }

                // Output
                double[] y = new double[outputSize];
                for (int i = 0; i < outputSize; i++) {
                    double sum = by[i];
                    for (int j = 0; j < hiddenSize; j++) sum += Why[i][j] * hs[T][j];
                    y[i] = sigmoid(sum);
                }

                // Weighted loss
                for (int i = 0; i < outputSize; i++) {
                    double w = (target[i] == 1.0) ? posWeight : negWeight;
                    totalLoss += -w * (target[i] * Math.log(y[i] + 1e-8) +
                            (1 - target[i]) * Math.log(1 - y[i] + 1e-8));
                }

                // ── Backward pass ──────────────────────────────

                // Weighted dy
                double[] dy = new double[outputSize];
                for (int i = 0; i < outputSize; i++) {
                    double w = (target[i] == 1.0) ? posWeight : negWeight;
                    dy[i] = w * (y[i] - target[i]);
                }

                double[][] dWhy = new double[outputSize][hiddenSize];
                double[]   dby  = new double[outputSize];

                for (int i = 0; i < outputSize; i++) {
                    dby[i] = dy[i];
                    for (int j = 0; j < hiddenSize; j++) dWhy[i][j] = dy[i] * hs[T][j];
                }

                double[] dhNext = new double[hiddenSize];
                for (int i = 0; i < hiddenSize; i++)
                    for (int k = 0; k < outputSize; k++)
                        dhNext[i] += dy[k] * Why[k][i];

                double[][] dWxh = new double[hiddenSize][inputSize];
                double[][] dWhh = new double[hiddenSize][hiddenSize];
                double[]   dbh  = new double[hiddenSize];

                double[][] dWxz = new double[hiddenSize][inputSize];
                double[][] dWhz = new double[hiddenSize][hiddenSize];
                double[]   dbz  = new double[hiddenSize];

                double[][] dWxr = new double[hiddenSize][inputSize];
                double[][] dWhr = new double[hiddenSize][hiddenSize];
                double[]   dbr  = new double[hiddenSize];

                // BPTT
                for (int t = T - 1; t >= 0; t--) {
                    double[] x     = seq[t];
                    double[] hPrev = hs[t];

                    double[] dpre_hc = new double[hiddenSize];
                    double[] dpre_z  = new double[hiddenSize];
                    double[] dpre_r  = new double[hiddenSize];
                    double[] d_rh    = new double[hiddenSize];
                    double[] dhPrev  = new double[hiddenSize];

                    for (int i = 0; i < hiddenSize; i++) {
                        double dhc = dhNext[i] * zs[t][i];
                        dpre_hc[i] = dhc * (1 - hcs[t][i] * hcs[t][i]);

                        double dz = dhNext[i] * (hcs[t][i] - hPrev[i]);
                        dpre_z[i] = dz * zs[t][i] * (1 - zs[t][i]);
                    }

                    for (int i = 0; i < hiddenSize; i++)
                        for (int j = 0; j < hiddenSize; j++)
                            d_rh[j] += dpre_hc[i] * Whh[i][j];

                    for (int i = 0; i < hiddenSize; i++)
                        dpre_r[i] = d_rh[i] * hPrev[i] * rs[t][i] * (1 - rs[t][i]);

                    for (int i = 0; i < hiddenSize; i++) {
                        dhPrev[i] += dhNext[i] * (1 - zs[t][i]);
                        dhPrev[i] += d_rh[i] * rs[t][i];
                    }
                    for (int j = 0; j < hiddenSize; j++)
                        for (int i = 0; i < hiddenSize; i++) {
                            dhPrev[j] += dpre_z[i] * Whz[i][j];
                            dhPrev[j] += dpre_r[i] * Whr[i][j];
                        }

                    for (int i = 0; i < hiddenSize; i++) {
                        dbh[i] += dpre_hc[i];
                        for (int j = 0; j < inputSize;  j++) dWxh[i][j] += dpre_hc[i] * x[j];
                        for (int j = 0; j < hiddenSize; j++) dWhh[i][j] += dpre_hc[i] * rhs[t][j];

                        dbz[i] += dpre_z[i];
                        for (int j = 0; j < inputSize;  j++) dWxz[i][j] += dpre_z[i] * x[j];
                        for (int j = 0; j < hiddenSize; j++) dWhz[i][j] += dpre_z[i] * hPrev[j];

                        dbr[i] += dpre_r[i];
                        for (int j = 0; j < inputSize;  j++) dWxr[i][j] += dpre_r[i] * x[j];
                        for (int j = 0; j < hiddenSize; j++) dWhr[i][j] += dpre_r[i] * hPrev[j];
                    }

                    dhNext = dhPrev;
                }

                // Gradient clipping
                clip(dWxh, gradientClipping); clip(dWhh, gradientClipping); clip(dbh, gradientClipping);
                clip(dWxz, gradientClipping); clip(dWhz, gradientClipping); clip(dbz, gradientClipping);
                clip(dWxr, gradientClipping); clip(dWhr, gradientClipping); clip(dbr, gradientClipping);
                clip(dWhy, gradientClipping); clip(dby,  gradientClipping);

                // Adam update
                adamT++;
                adamUpdate2D(Why, dWhy, mWhy, vWhy, lr, adamT);
                adamUpdate1D(by,  dby,  mby,  vby,  lr, adamT);

                adamUpdate2D(Wxh, dWxh, mWxh, vWxh, lr, adamT);
                adamUpdate2D(Whh, dWhh, mWhh, vWhh, lr, adamT);
                adamUpdate1D(bh,  dbh,  mbh,  vbh,  lr, adamT);

                adamUpdate2D(Wxz, dWxz, mWxz, vWxz, lr, adamT);
                adamUpdate2D(Whz, dWhz, mWhz, vWhz, lr, adamT);
                adamUpdate1D(bz,  dbz,  mbz,  vbz,  lr, adamT);

                adamUpdate2D(Wxr, dWxr, mWxr, vWxr, lr, adamT);
                adamUpdate2D(Whr, dWhr, mWhr, vWhr, lr, adamT);
                adamUpdate1D(br,  dbr,  mbr,  vbr,  lr, adamT);
            }

            if (epoch % 10 == 0)
                System.out.printf("Epoch %4d  Loss: %.6f%n", epoch, totalLoss / sequences.length);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════════════════════════

    /** One GRU step (inference only, no state stored). */
    private double[] gruStep(double[] x, double[] hPrev) {
        double[] z  = new double[hiddenSize];
        double[] r  = new double[hiddenSize];
        double[] rh = new double[hiddenSize];
        double[] hc = new double[hiddenSize];
        double[] h  = new double[hiddenSize];

        for (int i = 0; i < hiddenSize; i++) {
            double pz = bz[i], pr = br[i];
            for (int j = 0; j < inputSize;  j++) { pz += Wxz[i][j] * x[j]; pr += Wxr[i][j] * x[j]; }
            for (int j = 0; j < hiddenSize; j++) { pz += Whz[i][j] * hPrev[j]; pr += Whr[i][j] * hPrev[j]; }
            z[i]  = sigmoid(pz);
            r[i]  = sigmoid(pr);
            rh[i] = r[i] * hPrev[i];
        }
        for (int i = 0; i < hiddenSize; i++) {
            double ph = bh[i];
            for (int j = 0; j < inputSize;  j++) ph += Wxh[i][j] * x[j];
            for (int j = 0; j < hiddenSize; j++) ph += Whh[i][j] * rh[j];
            hc[i] = Math.tanh(ph);
            h[i]  = (1.0 - z[i]) * hPrev[i] + z[i] * hc[i];
        }
        return h;
    }

    private void adamUpdate2D(double[][] W, double[][] dW,
                              double[][] m, double[][] v,
                              double lr, int t) {
        double b1t = 1.0 - Math.pow(beta1, t);
        double b2t = 1.0 - Math.pow(beta2, t);
        for (int i = 0; i < W.length; i++)
            for (int j = 0; j < W[i].length; j++) {
                m[i][j] = beta1 * m[i][j] + (1 - beta1) * dW[i][j];
                v[i][j] = beta2 * v[i][j] + (1 - beta2) * dW[i][j] * dW[i][j];
                W[i][j] -= lr * (m[i][j] / b1t) / (Math.sqrt(v[i][j] / b2t) + eps);
            }
    }

    private void adamUpdate1D(double[] w, double[] dw,
                              double[] m, double[] v,
                              double lr, int t) {
        double b1t = 1.0 - Math.pow(beta1, t);
        double b2t = 1.0 - Math.pow(beta2, t);
        for (int i = 0; i < w.length; i++) {
            m[i] = beta1 * m[i] + (1 - beta1) * dw[i];
            v[i] = beta2 * v[i] + (1 - beta2) * dw[i] * dw[i];
            w[i] -= lr * (m[i] / b1t) / (Math.sqrt(v[i] / b2t) + eps);
        }
    }

    private double[][] randomMatrix(int rows, int cols, double scale) {
        double[][] m = new double[rows][cols];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                m[i][j] = (rnd.nextDouble() * 2 - 1) * scale;
        return m;
    }

    private void clip(double[][] m, double c) {
        for (double[] row : m)
            for (int j = 0; j < row.length; j++)
                row[j] = Math.max(-c, Math.min(c, row[j]));
    }

    private void clip(double[] v, double c) {
        for (int i = 0; i < v.length; i++)
            v[i] = Math.max(-c, Math.min(c, v[i]));
    }

    private static double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Serialisation  (binary, version = 1)
    // ════════════════════════════════════════════════════════════════════════

    private static final int FORMAT_VERSION = 1;

    public void save(File file) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)))) {

            dos.writeInt(FORMAT_VERSION);
            dos.writeInt(inputSize);
            dos.writeInt(hiddenSize);
            dos.writeInt(outputSize);

            writeMatrix(dos, Wxh); writeMatrix(dos, Whh); writeVector(dos, bh);
            writeMatrix(dos, Wxz); writeMatrix(dos, Whz); writeVector(dos, bz);
            writeMatrix(dos, Wxr); writeMatrix(dos, Whr); writeVector(dos, br);
            writeMatrix(dos, Why); writeVector(dos, by);

            dos.flush();
        }
    }

    public static GRU load(File file) throws IOException {
        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {

            int version = dis.readInt(); // читаем VERSION
            if (version != FORMAT_VERSION)
                throw new IOException("Unsupported model version: " + version);

            int in  = dis.readInt();
            int hid = dis.readInt();
            int out = dis.readInt();

            GRU gru = new GRU(in, hid, out);

            readMatrix(dis, gru.Wxh); readMatrix(dis, gru.Whh); readVector(dis, gru.bh);
            readMatrix(dis, gru.Wxz); readMatrix(dis, gru.Whz); readVector(dis, gru.bz);
            readMatrix(dis, gru.Wxr); readMatrix(dis, gru.Whr); readVector(dis, gru.br);
            readMatrix(dis, gru.Why); readVector(dis, gru.by);

            return gru;
        }
    }

    private static void writeMatrix(DataOutputStream dos, double[][] m) throws IOException {
        for (double[] row : m)
            for (double v : row)
                dos.writeDouble(v);
    }

    private static void writeVector(DataOutputStream dos, double[] v) throws IOException {
        for (double x : v) dos.writeDouble(x);
    }

    private static void readMatrix(DataInputStream dis, double[][] m) throws IOException {
        for (double[] row : m)
            for (int j = 0; j < row.length; j++)
                row[j] = dis.readDouble();
    }

    private static void readVector(DataInputStream dis, double[] v) throws IOException {
        for (int i = 0; i < v.length; i++) v[i] = dis.readDouble();
    }
}