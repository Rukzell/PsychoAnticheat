package com.psycho.ml.models;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Gated Recurrent Unit (GRU)
 * <p>
 * Forward pass equations:
 * z_t  = sigmoid(Wxz * x_t + Whz * h_{t-1} + bz)          — update gate
 * r_t  = sigmoid(Wxr * x_t + Whr * h_{t-1} + br)          — reset gate
 * h̃_t  = tanh   (Wxh * x_t + Whh * (r_t ⊙ h_{t-1}) + bh) — candidate
 * h_t  = (1 − z_t) ⊙ h_{t-1} + z_t ⊙ h̃_t                 — new hidden
 * y    = sigmoid(Why * h_T + by)                            — output
 */
public class GRU {

    private static final int FORMAT_VERSION = 2;
    private final int inputSize;
    private final int hiddenSize;
    private final int outputSize;
    // ── Adam hyper-params ───────────────────────────────────────────────────
    private final double gradientClipping = 5.0;
    private final double beta1 = 0.9, beta2 = 0.999, eps = 1e-8;
    private final Random rnd = new Random();
    // ── Weights ─────────────────────────────────────────────────────────────
    private final double[][] Wxh;
    private final double[][] Whh;
    private final double[] bh;
    private final double[][] Wxz;
    private final double[][] Whz;
    private final double[] bz;
    private final double[][] Wxr;
    private final double[][] Whr;
    private final double[] br;
    private final double[][] Why;
    private final double[] by;
    private int adamT = 0;
    // ── Adam moments ────────────────────────────────────────────────────────
    private double[][] mWxh, vWxh, mWhh, vWhh;
    private double[] mbh, vbh;
    private double[][] mWxz, vWxz, mWhz, vWhz;
    private double[] mbz, vbz;
    private double[][] mWxr, vWxr, mWhr, vWhr;
    private double[] mbr, vbr;
    private double[][] mWhy, vWhy;
    private double[] mby, vby;

    // ════════════════════════════════════════════════════════════════════════
    //  Construction
    // ════════════════════════════════════════════════════════════════════════

    public GRU(int inputSize, int hiddenSize, int outputSize) {
        this.inputSize = inputSize;
        this.hiddenSize = hiddenSize;
        this.outputSize = outputSize;

        double r = Math.sqrt(2.0 / (inputSize + hiddenSize));

        Wxh = randomMatrix(hiddenSize, inputSize, r);
        Whh = randomMatrix(hiddenSize, hiddenSize, r);
        bh = new double[hiddenSize];
        Wxz = randomMatrix(hiddenSize, inputSize, r);
        Whz = randomMatrix(hiddenSize, hiddenSize, r);
        bz = new double[hiddenSize];
        Wxr = randomMatrix(hiddenSize, inputSize, r);
        Whr = randomMatrix(hiddenSize, hiddenSize, r);
        br = new double[hiddenSize];
        Why = randomMatrix(outputSize, hiddenSize, r);
        by = new double[outputSize];

        initAdam();
    }

    private static double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Unified forward pass
    // ════════════════════════════════════════════════════════════════════════

    public static GRU load(File file) throws IOException {
        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {

            int version = dis.readInt();
            if (version != FORMAT_VERSION)
                throw new IOException("Unsupported model version: " + version +
                        " (expected " + FORMAT_VERSION + "). Retrain the model.");

            int in = dis.readInt(), hid = dis.readInt(), out = dis.readInt();
            GRU gru = new GRU(in, hid, out);
            gru.adamT = dis.readInt();

            // Weights
            readMatrix(dis, gru.Wxh);
            readMatrix(dis, gru.Whh);
            readVector(dis, gru.bh);
            readMatrix(dis, gru.Wxz);
            readMatrix(dis, gru.Whz);
            readVector(dis, gru.bz);
            readMatrix(dis, gru.Wxr);
            readMatrix(dis, gru.Whr);
            readVector(dis, gru.br);
            readMatrix(dis, gru.Why);
            readVector(dis, gru.by);

            // Adam moments
            readMatrix(dis, gru.mWxh);
            readMatrix(dis, gru.vWxh);
            readMatrix(dis, gru.mWhh);
            readMatrix(dis, gru.vWhh);
            readVector(dis, gru.mbh);
            readVector(dis, gru.vbh);

            readMatrix(dis, gru.mWxz);
            readMatrix(dis, gru.vWxz);
            readMatrix(dis, gru.mWhz);
            readMatrix(dis, gru.vWhz);
            readVector(dis, gru.mbz);
            readVector(dis, gru.vbz);

            readMatrix(dis, gru.mWxr);
            readMatrix(dis, gru.vWxr);
            readMatrix(dis, gru.mWhr);
            readMatrix(dis, gru.vWhr);
            readVector(dis, gru.mbr);
            readVector(dis, gru.vbr);

            readMatrix(dis, gru.mWhy);
            readMatrix(dis, gru.vWhy);
            readVector(dis, gru.mby);
            readVector(dis, gru.vby);

            return gru;
        }
    }

    private static void writeMatrix(DataOutputStream dos, double[][] m) throws IOException {
        for (double[] row : m) for (double v : row) dos.writeDouble(v);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Training
    // ════════════════════════════════════════════════════════════════════════

    private static void writeVector(DataOutputStream dos, double[] v) throws IOException {
        for (double x : v) dos.writeDouble(x);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════════════════════════

    private static void readMatrix(DataInputStream dis, double[][] m) throws IOException {
        for (double[] row : m) for (int j = 0; j < row.length; j++) row[j] = dis.readDouble();
    }

    private static void readVector(DataInputStream dis, double[] v) throws IOException {
        for (int i = 0; i < v.length; i++) v[i] = dis.readDouble();
    }

    private void initAdam() {
        mWxh = new double[hiddenSize][inputSize];
        vWxh = new double[hiddenSize][inputSize];
        mWhh = new double[hiddenSize][hiddenSize];
        vWhh = new double[hiddenSize][hiddenSize];
        mbh = new double[hiddenSize];
        vbh = new double[hiddenSize];

        mWxz = new double[hiddenSize][inputSize];
        vWxz = new double[hiddenSize][inputSize];
        mWhz = new double[hiddenSize][hiddenSize];
        vWhz = new double[hiddenSize][hiddenSize];
        mbz = new double[hiddenSize];
        vbz = new double[hiddenSize];

        mWxr = new double[hiddenSize][inputSize];
        vWxr = new double[hiddenSize][inputSize];
        mWhr = new double[hiddenSize][hiddenSize];
        vWhr = new double[hiddenSize][hiddenSize];
        mbr = new double[hiddenSize];
        vbr = new double[hiddenSize];

        mWhy = new double[outputSize][hiddenSize];
        vWhy = new double[outputSize][hiddenSize];
        mby = new double[outputSize];
        vby = new double[outputSize];
    }

    private TrainingContext forwardFull(double[][] sequence) {
        TrainingContext context = new TrainingContext(sequence.length, hiddenSize, outputSize);
        forwardFull(sequence, context);
        return context;
    }

    private void forwardFull(double[][] sequence, TrainingContext res) {
        int T = sequence.length;
        for (int t = 0; t < T; t++) {
            double[] x = sequence[t];
            double[] hPrev = res.hs[t];

            for (int i = 0; i < hiddenSize; i++) {
                double pz = bz[i], pr = br[i];
                for (int j = 0; j < inputSize; j++) {
                    pz += Wxz[i][j] * x[j];
                    pr += Wxr[i][j] * x[j];
                }
                for (int j = 0; j < hiddenSize; j++) {
                    pz += Whz[i][j] * hPrev[j];
                    pr += Whr[i][j] * hPrev[j];
                }
                res.zs[t][i] = sigmoid(pz);
                res.rs[t][i] = sigmoid(pr);
                res.rhs[t][i] = res.rs[t][i] * hPrev[i];
            }

            for (int i = 0; i < hiddenSize; i++) {
                double ph = bh[i];
                for (int j = 0; j < inputSize; j++) ph += Wxh[i][j] * x[j];
                for (int j = 0; j < hiddenSize; j++) ph += Whh[i][j] * res.rhs[t][j];
                res.hcs[t][i] = Math.tanh(ph);
                res.hs[t + 1][i] = (1.0 - res.zs[t][i]) * hPrev[i] + res.zs[t][i] * res.hcs[t][i];
            }
        }
        for (int i = 0; i < outputSize; i++) {
            double sum = by[i];
            for (int j = 0; j < hiddenSize; j++) sum += Why[i][j] * res.hs[T][j];
            res.y[i] = sigmoid(sum);
        }
    }

    public double[] forward(double[][] sequence) {
        return Arrays.copyOf(forwardFull(sequence).y, outputSize);
    }

    public InferenceContext createInferenceContext() {
        return new InferenceContext(hiddenSize);
    }

    public double predictScalar(double[][] sequence, InferenceContext context) {
        if (outputSize != 1) {
            throw new IllegalStateException("predictScalar() requires outputSize == 1, got " + outputSize);
        }

        double[] hPrev = context.stateA;
        double[] hCurr = context.stateB;
        Arrays.fill(hPrev, 0.0);
        Arrays.fill(hCurr, 0.0);

        for (double[] x : sequence) {
            for (int i = 0; i < hiddenSize; i++) {
                double pz = bz[i];
                double pr = br[i];
                double[] wxz = Wxz[i];
                double[] wxr = Wxr[i];
                double[] whz = Whz[i];
                double[] whr = Whr[i];

                for (int j = 0; j < inputSize; j++) {
                    double xj = x[j];
                    pz += wxz[j] * xj;
                    pr += wxr[j] * xj;
                }

                for (int j = 0; j < hiddenSize; j++) {
                    double hp = hPrev[j];
                    pz += whz[j] * hp;
                    pr += whr[j] * hp;
                }

                double z = sigmoid(pz);
                double r = sigmoid(pr);
                context.z[i] = z;
                context.rh[i] = r * hPrev[i];
            }

            for (int i = 0; i < hiddenSize; i++) {
                double ph = bh[i];
                double[] wxh = Wxh[i];
                double[] whh = Whh[i];

                for (int j = 0; j < inputSize; j++) {
                    ph += wxh[j] * x[j];
                }

                for (int j = 0; j < hiddenSize; j++) {
                    ph += whh[j] * context.rh[j];
                }

                double hc = Math.tanh(ph);
                hCurr[i] = (1.0 - context.z[i]) * hPrev[i] + context.z[i] * hc;
            }

            double[] tmp = hPrev;
            hPrev = hCurr;
            hCurr = tmp;
        }

        double sum = by[0];
        double[] why0 = Why[0];
        for (int j = 0; j < hiddenSize; j++) {
            sum += why0[j] * hPrev[j];
        }
        return sigmoid(sum);
    }

    public void train(double[][][] sequences, double[][] targets, double lr, int epochs) {

        // ── Auto class weights ───────────────────────────────────────────
        long positives = 0;
        for (double[] t : targets) if (t[0] == 1.0) positives++;
        long negatives = targets.length - positives;
        double total = targets.length;
        double posWeight = (positives > 0) ? total / (2.0 * positives) : 1.0;
        double negWeight = (negatives > 0) ? total / (2.0 * negatives) : 1.0;

        System.out.printf("Positives: %d | Negatives: %d | posWeight: %.4f | negWeight: %.4f%n",
                positives, negatives, posWeight, negWeight);

        int[] order = new int[sequences.length];
        for (int i = 0; i < sequences.length; i++) {
            order[i] = i;
        }

        int sequenceLength = sequences.length == 0 ? 0 : sequences[0].length;
        TrainingContext fwd = new TrainingContext(sequenceLength, hiddenSize, outputSize);
        GradientBuffers gradients = new GradientBuffers(hiddenSize, inputSize, outputSize);

        for (int epoch = 0; epoch < epochs; epoch++) {
            shuffle(order);
            double totalLoss = 0.0;

            for (int idx : order) {
                double[][] seq = sequences[idx];
                double[] target = targets[idx];
                int T = seq.length;

                // ── Forward ──────────────────────────────────
                gradients.clear();
                forwardFull(seq, fwd);
                double[] y = fwd.y;
                double[][] hs = fwd.hs;
                double[][] zs = fwd.zs;
                double[][] rs = fwd.rs;
                double[][] hcs = fwd.hcs;
                double[][] rhs = fwd.rhs;

                // ── Weighted loss ──────────────────────────────────────
                for (int i = 0; i < outputSize; i++) {
                    double w = (target[i] == 1.0) ? posWeight : negWeight;
                    totalLoss += -w * (target[i] * Math.log(y[i] + 1e-8) +
                            (1 - target[i]) * Math.log(1 - y[i] + 1e-8));
                }

                // ── Backward ───────────────────────────────────────────
                double[] dy = gradients.dy;
                for (int i = 0; i < outputSize; i++) {
                    double w = (target[i] == 1.0) ? posWeight : negWeight;
                    dy[i] = w * (y[i] - target[i]);
                }

                double[][] dWhy = gradients.dWhy;
                double[] dby = gradients.dby;
                for (int i = 0; i < outputSize; i++) {
                    dby[i] = dy[i];
                    for (int j = 0; j < hiddenSize; j++) dWhy[i][j] = dy[i] * hs[T][j];
                }

                double[] dhNext = gradients.dhNext;
                for (int i = 0; i < hiddenSize; i++)
                    for (int k = 0; k < outputSize; k++)
                        dhNext[i] += dy[k] * Why[k][i];

                double[][] dWxh = gradients.dWxh;
                double[][] dWhh = gradients.dWhh;
                double[] dbh = gradients.dbh;
                double[][] dWxz = gradients.dWxz;
                double[][] dWhz = gradients.dWhz;
                double[] dbz = gradients.dbz;
                double[][] dWxr = gradients.dWxr;
                double[][] dWhr = gradients.dWhr;
                double[] dbr = gradients.dbr;

                // BPTT
                for (int t = T - 1; t >= 0; t--) {
                    double[] x = seq[t];
                    double[] hPrev = hs[t];

                    double[] dpre_hc = gradients.dpre_hc;
                    double[] dpre_z = gradients.dpre_z;
                    double[] dpre_r = gradients.dpre_r;
                    double[] d_rh = gradients.d_rh;
                    double[] dhPrev = gradients.dhPrev;
                    Arrays.fill(dpre_hc, 0.0);
                    Arrays.fill(dpre_z, 0.0);
                    Arrays.fill(dpre_r, 0.0);
                    Arrays.fill(d_rh, 0.0);
                    Arrays.fill(dhPrev, 0.0);

                    for (int i = 0; i < hiddenSize; i++) {
                        dpre_hc[i] = dhNext[i] * zs[t][i] * (1 - hcs[t][i] * hcs[t][i]);
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
                        for (int j = 0; j < inputSize; j++) dWxh[i][j] += dpre_hc[i] * x[j];
                        for (int j = 0; j < hiddenSize; j++) dWhh[i][j] += dpre_hc[i] * rhs[t][j];

                        dbz[i] += dpre_z[i];
                        for (int j = 0; j < inputSize; j++) dWxz[i][j] += dpre_z[i] * x[j];
                        for (int j = 0; j < hiddenSize; j++) dWhz[i][j] += dpre_z[i] * hPrev[j];

                        dbr[i] += dpre_r[i];
                        for (int j = 0; j < inputSize; j++) dWxr[i][j] += dpre_r[i] * x[j];
                        for (int j = 0; j < hiddenSize; j++) dWhr[i][j] += dpre_r[i] * hPrev[j];
                    }

                    double[] swap = dhNext;
                    dhNext = dhPrev;
                    gradients.dhPrev = swap;
                }

                // ── Normalize by sequence length ───────────────
                double invT = 1.0 / T;
                scale(dWxh, invT);
                scale(dWhh, invT);
                scale(dbh, invT);
                scale(dWxz, invT);
                scale(dWhz, invT);
                scale(dbz, invT);
                scale(dWxr, invT);
                scale(dWhr, invT);
                scale(dbr, invT);

                // ── Gradient clipping ──────────────────────────────────
                clip(dWxh, gradientClipping);
                clip(dWhh, gradientClipping);
                clip(dbh, gradientClipping);
                clip(dWxz, gradientClipping);
                clip(dWhz, gradientClipping);
                clip(dbz, gradientClipping);
                clip(dWxr, gradientClipping);
                clip(dWhr, gradientClipping);
                clip(dbr, gradientClipping);
                clip(dWhy, gradientClipping);
                clip(dby, gradientClipping);

                // ── Adam update (per-sample — standard online SGD) ─────
                adamT++;
                adamUpdate2D(Why, dWhy, mWhy, vWhy, lr, adamT);
                adamUpdate1D(by, dby, mby, vby, lr, adamT);

                adamUpdate2D(Wxh, dWxh, mWxh, vWxh, lr, adamT);
                adamUpdate2D(Whh, dWhh, mWhh, vWhh, lr, adamT);
                adamUpdate1D(bh, dbh, mbh, vbh, lr, adamT);

                adamUpdate2D(Wxz, dWxz, mWxz, vWxz, lr, adamT);
                adamUpdate2D(Whz, dWhz, mWhz, vWhz, lr, adamT);
                adamUpdate1D(bz, dbz, mbz, vbz, lr, adamT);

                adamUpdate2D(Wxr, dWxr, mWxr, vWxr, lr, adamT);
                adamUpdate2D(Whr, dWhr, mWhr, vWhr, lr, adamT);
                adamUpdate1D(br, dbr, mbr, vbr, lr, adamT);
            }

            if (epoch == 0 || epoch == epochs - 1 || epoch % 20 == 0) {
                System.out.printf("Epoch %4d  Loss: %.6f%n", epoch, totalLoss / sequences.length);
                evaluate(sequences, targets);
            }
        }
    }

    private void scale(double[][] m, double s) {
        for (double[] row : m) for (int j = 0; j < row.length; j++) row[j] *= s;
    }

    private void scale(double[] v, double s) {
        for (int i = 0; i < v.length; i++) v[i] *= s;
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
        for (double[] row : m) for (int j = 0; j < row.length; j++) row[j] = Math.max(-c, Math.min(c, row[j]));
    }

    private void clip(double[] v, double c) {
        for (int i = 0; i < v.length; i++) v[i] = Math.max(-c, Math.min(c, v[i]));
    }

    public void save(File file) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)))) {

            dos.writeInt(FORMAT_VERSION);
            dos.writeInt(inputSize);
            dos.writeInt(hiddenSize);
            dos.writeInt(outputSize);
            dos.writeInt(adamT);

            // Weights
            writeMatrix(dos, Wxh);
            writeMatrix(dos, Whh);
            writeVector(dos, bh);
            writeMatrix(dos, Wxz);
            writeMatrix(dos, Whz);
            writeVector(dos, bz);
            writeMatrix(dos, Wxr);
            writeMatrix(dos, Whr);
            writeVector(dos, br);
            writeMatrix(dos, Why);
            writeVector(dos, by);

            // Adam moments
            writeMatrix(dos, mWxh);
            writeMatrix(dos, vWxh);
            writeMatrix(dos, mWhh);
            writeMatrix(dos, vWhh);
            writeVector(dos, mbh);
            writeVector(dos, vbh);

            writeMatrix(dos, mWxz);
            writeMatrix(dos, vWxz);
            writeMatrix(dos, mWhz);
            writeMatrix(dos, vWhz);
            writeVector(dos, mbz);
            writeVector(dos, vbz);

            writeMatrix(dos, mWxr);
            writeMatrix(dos, vWxr);
            writeMatrix(dos, mWhr);
            writeMatrix(dos, vWhr);
            writeVector(dos, mbr);
            writeVector(dos, vbr);

            writeMatrix(dos, mWhy);
            writeMatrix(dos, vWhy);
            writeVector(dos, mby);
            writeVector(dos, vby);

            dos.flush();
        }
    }

    public void evaluate(double[][][] sequences, double[][] targets) {

        int tp = 0, tn = 0, fp = 0, fn = 0;
        InferenceContext context = createInferenceContext();

        for (int i = 0; i < sequences.length; i++) {

            double y = predictScalar(sequences[i], context) >= 0.5 ? 1.0 : 0.0;
            double t = targets[i][0];

            if (t == 1.0 && y == 1.0) tp++;
            else if (t == 0.0 && y == 0.0) tn++;
            else if (t == 0.0 && y == 1.0) fp++;
            else if (t == 1.0 && y == 0.0) fn++;
        }

        int total = tp + tn + fp + fn;

        double accuracy = (double) (tp + tn) / total;

        double recall = tp / (tp + fn + 1e-8);

        double precision = tp / (tp + fp + 1e-8);

        double fpr = fp / (fp + tn + 1e-8);

        double f1 = 2 * precision * recall / (precision + recall + 1e-8);

        System.out.printf(
                "Accuracy: %.4f | Recall: %.4f | Precision: %.4f | F1: %.4f | FPR: %.4f%n",
                accuracy, recall, precision, f1, fpr
        );
    }

    // ── Internal forward result (for BPTT) ──────────────────────────────────
    private static class ForwardResult {
        double[][] hs, zs, rs, hcs, rhs;
        double[] y;
    }

    private void shuffle(int[] order) {
        for (int i = order.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int tmp = order[i];
            order[i] = order[j];
            order[j] = tmp;
        }
    }

    private static final class TrainingContext extends ForwardResult {
        private TrainingContext(int sequenceLength, int hiddenSize, int outputSize) {
            this.hs = new double[sequenceLength + 1][hiddenSize];
            this.zs = new double[sequenceLength][hiddenSize];
            this.rs = new double[sequenceLength][hiddenSize];
            this.hcs = new double[sequenceLength][hiddenSize];
            this.rhs = new double[sequenceLength][hiddenSize];
            this.y = new double[outputSize];
        }
    }

    private static final class GradientBuffers {
        private final double[][] dWxh;
        private final double[][] dWhh;
        private final double[] dbh;
        private final double[][] dWxz;
        private final double[][] dWhz;
        private final double[] dbz;
        private final double[][] dWxr;
        private final double[][] dWhr;
        private final double[] dbr;
        private final double[][] dWhy;
        private final double[] dby;
        private final double[] dy;
        private double[] dhNext;
        private double[] dhPrev;
        private final double[] dpre_hc;
        private final double[] dpre_z;
        private final double[] dpre_r;
        private final double[] d_rh;

        private GradientBuffers(int hiddenSize, int inputSize, int outputSize) {
            this.dWxh = new double[hiddenSize][inputSize];
            this.dWhh = new double[hiddenSize][hiddenSize];
            this.dbh = new double[hiddenSize];
            this.dWxz = new double[hiddenSize][inputSize];
            this.dWhz = new double[hiddenSize][hiddenSize];
            this.dbz = new double[hiddenSize];
            this.dWxr = new double[hiddenSize][inputSize];
            this.dWhr = new double[hiddenSize][hiddenSize];
            this.dbr = new double[hiddenSize];
            this.dWhy = new double[outputSize][hiddenSize];
            this.dby = new double[outputSize];
            this.dy = new double[outputSize];
            this.dhNext = new double[hiddenSize];
            this.dhPrev = new double[hiddenSize];
            this.dpre_hc = new double[hiddenSize];
            this.dpre_z = new double[hiddenSize];
            this.dpre_r = new double[hiddenSize];
            this.d_rh = new double[hiddenSize];
        }

        private void clear() {
            clearMatrix(dWxh);
            clearMatrix(dWhh);
            Arrays.fill(dbh, 0.0);
            clearMatrix(dWxz);
            clearMatrix(dWhz);
            Arrays.fill(dbz, 0.0);
            clearMatrix(dWxr);
            clearMatrix(dWhr);
            Arrays.fill(dbr, 0.0);
            clearMatrix(dWhy);
            Arrays.fill(dby, 0.0);
            Arrays.fill(dy, 0.0);
            Arrays.fill(dhNext, 0.0);
            Arrays.fill(dhPrev, 0.0);
        }

        private static void clearMatrix(double[][] matrix) {
            for (double[] row : matrix) {
                Arrays.fill(row, 0.0);
            }
        }
    }

    public static final class InferenceContext {
        private final double[] stateA;
        private final double[] stateB;
        private final double[] z;
        private final double[] rh;

        private InferenceContext(int hiddenSize) {
            this.stateA = new double[hiddenSize];
            this.stateB = new double[hiddenSize];
            this.z = new double[hiddenSize];
            this.rh = new double[hiddenSize];
        }
    }
}
