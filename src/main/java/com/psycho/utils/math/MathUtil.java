package com.psycho.utils.math;

import org.jtransforms.fft.DoubleFFT_1D;
import org.jtransforms.fft.FloatFFT_1D;

import java.util.*;
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

    public static double shannonEntropy(Collection<? extends Number> values) {
        int n = values.size();
        if (n == 0) return 0.0;

        Map<Double, Integer> frequency = new HashMap<>();

        for (Number num : values) {
            double v = num.doubleValue();
            frequency.put(v, frequency.getOrDefault(v, 0) + 1);
        }

        double entropy = 0.0;

        for (int count : frequency.values()) {
            double p = (double) count / n;
            entropy -= p * (Math.log(p) / Math.log(2));
        }

        return entropy;
    }

    public static double average(final Collection<? extends Number> values) {
        int count = values.size();
        if (count == 0) return 0.0;

        double sum = 0.0;
        for (Number number : values) {
            sum += number.doubleValue();
        }

        return sum / count;
    }

    public static double autocorrelation(Collection<? extends Number> values, int lag) {
        int n = values.size();
        if (n <= lag || lag < 1) return 0.0;

        double[] arr = new double[n];
        int index = 0;
        for (Number number : values) {
            arr[index++] = number.doubleValue();
        }

        double mean = average(values);

        double numerator = 0.0;
        double denominator = 0.0;

        for (int i = 0; i < n; i++) {
            double diff = arr[i] - mean;
            denominator += diff * diff;

            if (i >= lag) {
                numerator += diff * (arr[i - lag] - mean);
            }
        }

        if (denominator == 0.0) return 0.0;

        return numerator / denominator;
    }

    public static double highFreqRatio(Collection<? extends Number> deltas) {
        int N = deltas.size();
        if (N < 8) return 0.0;

        double[] data = new double[N];
        int i = 0;
        for (Number n : deltas) {
            data[i++] = n.doubleValue();
        }

        DoubleFFT_1D fft = new DoubleFFT_1D(N);
        fft.realForward(data);

        double totalEnergy = 0.0;
        double highFreqEnergy = 0.0;

        int halfN = N / 2;

        for (int k = 0; k < halfN; k++) {
            double re, im;

            if (k == 0) {
                re = data[0];
                im = 0.0;
            } else {
                re = data[2 * k];
                im = data[2 * k + 1];
            }

            double energy = re * re + im * im;
            totalEnergy += energy;

            if (k >= halfN / 2) {
                highFreqEnergy += energy;
            }
        }

        if (totalEnergy == 0.0) return 0.0;

        return highFreqEnergy / totalEnergy;
    }

    public static double kurtosis(Collection<? extends Number> values) {
        int n = values.size();
        if (n < 4) return 0.0;

        double mean = average(values);

        double m2 = 0.0;
        double m4 = 0.0;

        for (Number number : values) {
            double diff = number.doubleValue() - mean;
            double diff2 = diff * diff;

            m2 += diff2;
            m4 += diff2 * diff2;
        }

        m2 /= n;
        m4 /= n;

        if (m2 == 0.0) return 0.0;

        return (m4 / (m2 * m2)) - 3.0;
    }

    public static double spectralFlatness(Collection<? extends Number> deltas) {
        int N = deltas.size();
        if (N < 8) return 0.0;

        double[] data = new double[N];
        int i = 0;
        for (Number n : deltas) data[i++] = n.doubleValue();

        DoubleFFT_1D fft = new DoubleFFT_1D(N);
        fft.realForward(data);

        double[] magnitude = new double[N / 2];
        int halfN = N / 2;

        for (int k = 0; k < halfN; k++) {
            double re, im;
            if (k == 0) {
                re = data[0];
                im = 0.0;
            } else {
                re = data[2 * k];
                im = data[2 * k + 1];
            }
            magnitude[k] = Math.sqrt(re * re + im * im);
        }

        double logSum = 0.0;
        double arithSum = 0.0;
        int count = 0;
        for (double mag : magnitude) {
            if (mag <= 0) continue;
            logSum += Math.log(mag);
            arithSum += mag;
            count++;
        }

        if (count == 0 || arithSum == 0) return 0.0;

        double geoMean = Math.exp(logSum / count);
        double arithMean = arithSum / count;

        return geoMean / arithMean;
    }

    public static double max(Collection<? extends Number> values) {
        return values.stream().mapToDouble(Number::doubleValue).max().orElse(0.0);
    }

    public static double min(Collection<? extends Number> values) {
        return values.stream().mapToDouble(Number::doubleValue).min().orElse(0.0);
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
