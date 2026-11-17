package kg.fastfood.crm.math;

/**
 * Модель №2. Распределение Пуассона — вероятность получить ровно k заказов за интервал.
 * <pre>
 *   P(X = k) = (λᵏ · e^(−λ)) / k!
 *   λ = N / T   (N — заказов за период наблюдения, T — число одинаковых интервалов)
 * </pre>
 * Вероятность считается через логарифмы, чтобы λᵏ и k! не переполняли тип double при больших k.
 */
public final class PoissonDistribution {

    private PoissonDistribution() {
    }

    /** Параметр λ = N / T. */
    public static double lambda(long ordersCount, long intervalsCount) {
        if (intervalsCount <= 0) {
            throw new IllegalArgumentException("Количество интервалов T должно быть больше нуля");
        }
        if (ordersCount < 0) {
            throw new IllegalArgumentException("Количество заказов не может быть отрицательным");
        }
        return (double) ordersCount / intervalsCount;
    }

    /** P(X = k). */
    public static double probability(int k, double lambda) {
        validate(k, lambda);
        if (lambda == 0) {
            return k == 0 ? 1.0 : 0.0;
        }
        // ln P = k·ln λ − λ − ln(k!)
        double logP = k * Math.log(lambda) - lambda - logFactorial(k);
        return Math.exp(logP);
    }

    /** P(X ≤ k) — функция распределения. */
    public static double cumulative(int k, double lambda) {
        validate(k, lambda);
        double sum = 0;
        for (int i = 0; i <= k; i++) {
            sum += probability(i, lambda);
        }
        return Math.min(1.0, sum);
    }

    /** P(X ≥ k) — вероятность, что заказов будет не меньше k. */
    public static double atLeast(int k, double lambda) {
        if (k <= 0) {
            return 1.0;
        }
        return Math.max(0.0, 1.0 - cumulative(k - 1, lambda));
    }

    /**
     * Квантиль: наименьшее k, при котором P(X ≤ k) ≥ p.
     * Например, quantile(0.95, λ) — количество заказов, которое не будет превышено в 95% интервалов.
     */
    public static int quantile(double p, double lambda) {
        if (p <= 0 || p >= 1) {
            throw new IllegalArgumentException("Уровень вероятности должен быть в интервале (0; 1)");
        }
        validate(0, lambda);
        double sum = 0;
        int k = 0;
        while (true) {
            sum += probability(k, lambda);
            if (sum >= p || k > 10_000) {
                return k;
            }
            k++;
        }
    }

    /** ln(k!) = ln 1 + ln 2 + ... + ln k. */
    static double logFactorial(int k) {
        double result = 0;
        for (int i = 2; i <= k; i++) {
            result += Math.log(i);
        }
        return result;
    }

    private static void validate(int k, double lambda) {
        if (k < 0) {
            throw new IllegalArgumentException("k не может быть отрицательным");
        }
        if (lambda < 0 || Double.isNaN(lambda) || Double.isInfinite(lambda)) {
            throw new IllegalArgumentException("Параметр λ должен быть неотрицательным числом");
        }
    }
}
