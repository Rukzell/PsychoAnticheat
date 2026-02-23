package net.rukzell.tac.utils.math;

import java.util.List;

public class MathUtil {
    public static double stddev(List<Float> values) {
        if (values.isEmpty()) return 0.0;

        double mean = values.stream()
                .mapToDouble(Float::doubleValue)
                .average()
                .orElse(0.0);

        double sumSquaredDiffs = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .sum();

        return Math.sqrt(sumSquaredDiffs / values.size());
    }
}
