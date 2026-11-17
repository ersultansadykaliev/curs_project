package kg.fastfood.crm.math;

import java.util.List;

/**
 * Модель №6. Оценка точности прогноза.
 * <pre>
 *   MAE = Σ|yᵢ − ŷᵢ| / n
 * </pre>
 * yᵢ — фактическое значение, ŷᵢ — прогноз, n — количество наблюдений. Чем меньше MAE, тем точнее прогноз.
 */
public final class ForecastAccuracy {

    private ForecastAccuracy() {
    }

    /** Средняя абсолютная ошибка. */
    public static double mae(List<Double> actual, List<Double> predicted) {
        validate(actual, predicted);
        double sum = 0;
        for (int i = 0; i < actual.size(); i++) {
            sum += Math.abs(actual.get(i) - predicted.get(i));
        }
        return sum / actual.size();
    }

    /**
     * Относительная ошибка: MAE в процентах от средней фактической величины.
     * Нужна, чтобы понять, много это или мало. Возвращает NaN, если среднее фактическое = 0.
     */
    public static double relativeMaePercent(List<Double> actual, List<Double> predicted) {
        double mae = mae(actual, predicted);
        double meanActual = DescriptiveStatistics.mean(actual);
        if (meanActual == 0) {
            return Double.NaN;
        }
        return mae / Math.abs(meanActual) * 100.0;
    }

    private static void validate(List<Double> actual, List<Double> predicted) {
        if (actual == null || predicted == null || actual.isEmpty()) {
            throw new IllegalArgumentException("Нет наблюдений для оценки точности прогноза");
        }
        if (actual.size() != predicted.size()) {
            throw new IllegalArgumentException("Количество фактических и прогнозных значений не совпадает");
        }
    }
}
