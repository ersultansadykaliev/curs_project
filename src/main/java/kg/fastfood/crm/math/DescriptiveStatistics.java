package kg.fastfood.crm.math;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Модель №1. Средние показатели продаж.
 * <pre>
 *   X̄ = Σxᵢ / n        — среднее арифметическое
 *   A = R / N          — средний чек (R — выручка, N — количество заказов)
 * </pre>
 */
public final class DescriptiveStatistics {

    private DescriptiveStatistics() {
    }

    /** Среднее арифметическое X̄ = Σxᵢ / n. */
    public static double mean(List<Double> values) {
        requireNotEmpty(values);
        double sum = 0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.size();
    }

    /** Средний чек A = R / N. */
    public static double averageCheck(double revenue, long ordersCount) {
        if (ordersCount <= 0) {
            throw new IllegalArgumentException("Количество заказов должно быть больше нуля");
        }
        if (revenue < 0) {
            throw new IllegalArgumentException("Выручка не может быть отрицательной");
        }
        return revenue / ordersCount;
    }

    /** Выборочная дисперсия s² = Σ(xᵢ − X̄)² / (n − 1). Для одного значения возвращает 0. */
    public static double sampleVariance(List<Double> values) {
        requireNotEmpty(values);
        if (values.size() == 1) {
            return 0;
        }
        double mean = mean(values);
        double sum = 0;
        for (double v : values) {
            sum += (v - mean) * (v - mean);
        }
        return sum / (values.size() - 1);
    }

    /** Стандартное отклонение s = √s². */
    public static double standardDeviation(List<Double> values) {
        return Math.sqrt(sampleVariance(values));
    }

    /** Медиана — значение, которое делит упорядоченный ряд пополам. */
    public static double median(List<Double> values) {
        requireNotEmpty(values);
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int n = sorted.size();
        if (n % 2 == 1) {
            return sorted.get(n / 2);
        }
        return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }

    private static void requireNotEmpty(List<Double> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Нет данных для расчёта");
        }
    }
}
