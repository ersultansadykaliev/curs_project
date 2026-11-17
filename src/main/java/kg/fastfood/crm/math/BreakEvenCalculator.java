package kg.fastfood.crm.math;

import kg.fastfood.crm.math.CostCalculator.SoldProduct;

import java.util.List;

/**
 * Модель №5. Точка безубыточности.
 * <pre>
 *   Q_BE = F / (P − Cᵥ)                — для одного товара, в штуках
 *   M_avg = Σ nᵢ(Pᵢ − Cᵥᵢ) / Σ nᵢ     — средневзвешенная маржа с учётом структуры продаж
 *   Q_BE = F / M_avg                   — для ассортимента, в штуках
 *   R_BE = F / k_M,  k_M = M_total / R — в денежном выражении (k_M — доля маржи в выручке)
 * </pre>
 */
public final class BreakEvenCalculator {

    private BreakEvenCalculator() {
    }

    /** Q_BE = F / (P − Cᵥ) для одного товара. */
    public static double unitsForProduct(double fixedCosts, double price, double variableCostPerUnit) {
        requireNonNegative(fixedCosts);
        double margin = price - variableCostPerUnit;
        if (margin <= 0) {
            throw new IllegalArgumentException("Цена не покрывает переменные затраты (P − Cᵥ ≤ 0): безубыточность недостижима");
        }
        return fixedCosts / margin;
    }

    /** Средневзвешенная маржинальная прибыль на единицу M_avg. */
    public static double weightedAverageMargin(List<SoldProduct> products) {
        long totalUnits = 0;
        for (SoldProduct p : products) {
            totalUnits += p.unitsSold();
        }
        if (totalUnits <= 0) {
            throw new IllegalArgumentException("Нет продаж — невозможно определить структуру ассортимента");
        }
        return CostCalculator.totalMargin(products) / totalUnits;
    }

    /** Q_BE = F / M_avg для ассортимента. */
    public static double unitsForAssortment(double fixedCosts, double averageMargin) {
        requireNonNegative(fixedCosts);
        if (averageMargin <= 0) {
            throw new IllegalArgumentException("Средняя маржа не положительна: безубыточность недостижима");
        }
        return fixedCosts / averageMargin;
    }

    /** Доля маржинальной прибыли в выручке k_M = M_total / R. */
    public static double contributionMarginRatio(double totalMargin, double revenue) {
        if (revenue <= 0) {
            throw new IllegalArgumentException("Выручка должна быть больше нуля");
        }
        return totalMargin / revenue;
    }

    /** Точка безубыточности в деньгах R_BE = F / k_M. */
    public static double revenueBreakEven(double fixedCosts, double marginRatio) {
        requireNonNegative(fixedCosts);
        if (marginRatio <= 0) {
            throw new IllegalArgumentException("Доля маржи не положительна: безубыточность недостижима");
        }
        return fixedCosts / marginRatio;
    }

    private static void requireNonNegative(double fixedCosts) {
        if (fixedCosts < 0) {
            throw new IllegalArgumentException("Постоянные расходы не могут быть отрицательными");
        }
    }
}
