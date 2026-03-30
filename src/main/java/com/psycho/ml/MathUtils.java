package com.psycho.ml;

import java.util.Random;

public class MathUtils {
    public static final Random RANDOM = new Random();

    public static double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    public static double sigmoidDerivative(double x) {
        double s = sigmoid(x);
        return s * (1.0 - s);
    }

    public static double tanh(double x) {
        return Math.tanh(x);
    }

    public static double tanhDerivative(double x) {
        double t = tanh(x);
        return 1.0 - t * t;
    }

    // Initialize an array with random values between min and max
    public static void randomize(double[][] array, double min, double max) {
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                array[i][j] = min + RANDOM.nextDouble() * (max - min);
            }
        }
    }

    public static void randomize(double[] array, double min, double max) {
        for (int i = 0; i < array.length; i++) {
            array[i] = min + RANDOM.nextDouble() * (max - min);
        }
    }
}
