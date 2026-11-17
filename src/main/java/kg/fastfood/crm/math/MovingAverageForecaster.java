package kg.fastfood.crm.math;

import java.util.ArrayList;
import java.util.List;

/**
 * Модель №3. Простое скользящее среднее.
 * <pre>
 *   F(t+1) = [R(t) + R(t−1) + ... + R(t−w+1)] / w
 * </pre>
 * По умолчанию w = 7 (неделя). Если наблюдений меньше w, прогноз не строится —
 * модель сообщает о нехватке данных вместо того, чтобы выдумывать значения.
 */
public final class MovingAverageForecaster {

    public static final int DEFAULT_WINDOW = 7;

    private MovingAverageForecaster() {
    }

    /** Хватает ли данных для прогноза с окном window. */
    public static boolean hasEnoughData(int observations, int window) {
        return window >= 1 && observations >= window;
    }

    /** Прогноз на следующий период по последним window значениям ряда. */
    public static double forecastNext(List<Double> series, int window) {
        validateWindow(window);
        if (series == null || !hasEnoughData(series.size(), window)) {
            int have = series == null ? 0 : series.size();
            throw new IllegalArgumentException("Недостаточно данных: для окна " + window
                    + " нужно минимум " + window + " наблюдений, имеется " + have);
        }
        double sum = 0;
        for (int i = series.size() - window; i < series.size(); i++) {
            sum += series.get(i);
        }
        return sum / window;
    }

    /**
     * Ретроспективный прогноз (backtest): для каждого t ≥ window строится прогноз по предыдущим
     * window значениям. Результат i соответствует фактическому значению series[window + i].
     * Используется для оценки точности модели (MAE).
     */
    public static List<Double> backtest(List<Double> series, int window) {
        validateWindow(window);
        List<Double> predictions = new ArrayList<>();
        if (series == null) {
            return predictions;
        }
        for (int t = window; t < series.size(); t++) {
            double sum = 0;
            for (int i = t - window; i < t; i++) {
                sum += series.get(i);
            }
            predictions.add(sum / window);
        }
        return predictions;
    }

    private static void validateWindow(int window) {
        if (window < 1) {
            throw new IllegalArgumentException("Период скользящего среднего должен быть не меньше 1");
        }
    }
}
