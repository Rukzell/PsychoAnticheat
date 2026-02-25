package net.rukzell.tac.utils.math;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class MathUtil {
    public static double stddev(final Collection<? extends Number> values) {
        final double variance = variance(values);

        return Math.sqrt(variance);
    }

    public static double variance(final Collection<? extends Number> values) {
        int count = 0;

        double sum = 0.0;
        double variance = 0.0;

        final double average;

        for (final Number number : values) {
            sum += number.doubleValue();
            ++count;
        }

        average = sum / count;

        for (final Number number : values) {
            variance += Math.pow(number.doubleValue() - average, 2.0);
        }

        return variance / count;
    }

    public static int distinct(Collection<? extends Number> values) {
        return (int) values.stream().distinct().count();
    }

    public static double robustRangeIQR(Collection<? extends Number> values) {
        int n = values.size();
        if (n < 8) return 0.0;

        double[] arr = new double[n];
        int i = 0;
        for (Number value : values) {
            arr[i++] = Math.abs(value.doubleValue());
        }

        Arrays.sort(arr);

        double q1 = percentile(arr, 25.0);
        double q3 = percentile(arr, 75.0);
        double median = percentile(arr, 50.0);

        double iqr = q3 - q1;
        if (median < 1e-4) return 0.0;

        return iqr / median;
    }

    public static Collection<Float> roundedValues(Collection<Float> values, float... modules) {
        return values.stream()
                .filter(value -> isRounded(value, modules))
                .collect(Collectors.toList());
    }

    public static boolean isRounded(float value, float... modules) {
        float rounded = Math.round(value);
        if (value == rounded) return true;

        for (float mod : modules) {
            if (mod != 0 && value % mod == 0.0F) {
                return true;
            }
        }

        return false;
    }

    private static double percentile(double[] arr, double percent) {
        if (arr.length == 0) return 0.0;

        double index = (percent / 100.0) * (arr.length - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);

        if (lower == upper) return arr[lower];

        double weight = index - lower;
        return arr[lower] * (1.0 - weight) + arr[upper] * weight;
    }
}
