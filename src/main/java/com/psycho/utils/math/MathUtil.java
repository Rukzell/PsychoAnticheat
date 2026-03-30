package com.psycho.utils.math;

import org.jtransforms.fft.DoubleFFT_1D;

import java.util.*;
import java.util.stream.Collectors;

public class MathUtil {
    public static double sineFitError(double[] data, double w) {
        int n = data.length;

        double sumSin2 = 0;
        double sumCos2 = 0;
        double sumSinCos = 0;
        double sumYSin = 0;
        double sumYCos = 0;

        for (int t = 0; t < n; t++) {
            double sin = Math.sin(w * t);
            double cos = Math.cos(w * t);
            double y = data[t];

            sumSin2 += sin * sin;
            sumCos2 += cos * cos;
            sumSinCos += sin * cos;

            sumYSin += y * sin;
            sumYCos += y * cos;
        }

        double det = sumSin2 * sumCos2 - sumSinCos * sumSinCos;
        if (Math.abs(det) < 1e-6) return Double.MAX_VALUE;

        double B = (sumYSin * sumCos2 - sumYCos * sumSinCos) / det;
        double C = (sumYCos * sumSin2 - sumYSin * sumSinCos) / det;

        double error = 0;

        for (int t = 0; t < n; t++) {
            double pred = B * Math.sin(w * t) + C * Math.cos(w * t);
            double diff = data[t] - pred;
            error += diff * diff;
        }

        return error / n;
    }

    public static double bestSineFit(double[] data) {
        double bestError = Double.MAX_VALUE;

        for (double w = 0.1; w < 1.5; w += 0.02) {
            double err = sineFitError(data, w);
            bestError = Math.min(bestError, err);
        }

        return bestError;
    }

    public static double[] zNormalize(Collection<? extends Number> values) {
        int size = values.size();
        double[] out = new double[size];

        if (size == 0) {
            return out;
        }

        double sum = 0.0;

        for (Number n : values) {
            sum += n.doubleValue();
        }

        double mean = sum / size;

        double variance = 0.0;
        for (Number n : values) {
            double d = n.doubleValue() - mean;
            variance += d * d;
        }

        double std = Math.sqrt(variance / size);

        int i = 0;
        if (std == 0) {
            for (Number n : values) {
                out[i++] = n.doubleValue() - mean;
            }
        } else {
            for (Number n : values) {
                out[i++] = (n.doubleValue() - mean) / std;
            }
        }

        return out;
    }

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


    public static double[] welchPSD(Collection<? extends Number> samples, int segmentSize, int overlap) {
        int n = samples.size();
        if (n < segmentSize) return new double[0];

        double[] signal = new double[n];
        int idx = 0;
        for (Number num : samples) {
            signal[idx++] = num.doubleValue();
        }

        int step = segmentSize - overlap;
        int segments = (signal.length - overlap) / step;
        if (segments <= 0) return new double[0];

        double mean = 0;
        for (double v : signal) mean += v;
        mean /= signal.length;

        double[] window = new double[segmentSize];
        for (int i = 0; i < segmentSize; i++) {
            window[i] = 0.54 - 0.46 * Math.cos(2 * Math.PI * i / (segmentSize - 1));
        }

        DoubleFFT_1D fft = new DoubleFFT_1D(segmentSize);
        double[] fftBuffer = new double[segmentSize * 2];
        double[] psd = new double[segmentSize / 2];

        double totalPower = 0;

        for (int s = 0; s < segments; s++) {
            int offset = s * step;
            for (int i = 0; i < segmentSize; i++) {
                double v = (signal[offset + i] - mean) * window[i];
                fftBuffer[2 * i] = v;
                fftBuffer[2 * i + 1] = 0;
            }

            fft.complexForward(fftBuffer);

            for (int i = 0; i < segmentSize / 2; i++) {
                double re = fftBuffer[2 * i];
                double im = fftBuffer[2 * i + 1];
                double power = re * re + im * im;
                psd[i] += power;
                totalPower += power;
            }
        }

        for (int i = 0; i < psd.length; i++) {
            psd[i] /= segments;
        }

        return psd;
    }

    public static double[] welchPSD(double[] data, int window, int overlap) {
        List<Double> list = new ArrayList<>(data.length);

        for (double v : data) {
            list.add(v);
        }

        return welchPSD(list, window, overlap);
    }

    public static double spectralFlatness(double[] psd) {
        double geoMean = 1;
        double arithMean = 0;
        int n = psd.length;

        for (double p : psd) {
            geoMean *= (p + 1e-10);
            arithMean += p;
        }

        geoMean = Math.pow(geoMean, 1.0 / n);
        arithMean /= n;

        return geoMean / (arithMean + 1e-10);
    }

    public static double highFreqRatio(double[] psd) {
        int n = psd.length;
        double high = 0, total = 0;
        int cutoff = n / 2;

        for (int i = 0; i < n; i++) {
            if (i >= cutoff) high += psd[i];
            total += psd[i];
        }

        return total == 0 ? 0 : high / total;
    }

    public static double max(Collection<? extends Number> values) {
        double max = Double.NEGATIVE_INFINITY;
        for (Number v : values) {
            double d = v.doubleValue();
            if (d > max) max = d;
        }
        return max == Double.NEGATIVE_INFINITY ? 0.0 : max;
    }

    public static double min(Collection<? extends Number> values) {
        double min = Double.POSITIVE_INFINITY;
        for (Number v : values) {
            double d = v.doubleValue();
            if (d < min) min = d;
        }
        return min == Double.POSITIVE_INFINITY ? 0.0 : min;
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
