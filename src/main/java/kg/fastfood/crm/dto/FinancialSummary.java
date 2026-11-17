package kg.fastfood.crm.dto;

/**
 * Финансовые показатели за период.
 * <pre>
 *   R    = Σ(Pᵢ × nᵢ)  — выручка
 *   COGS = Σ(Cᵢ × nᵢ)  — себестоимость реализованной продукции
 *   GP   = R − COGS    — валовая прибыль
 *   OP   = GP − OE     — операционная прибыль (OE — расходы без закупки продуктов)
 * </pre>
 */
public record FinancialSummary(PeriodRange period,
                               long ordersCount,
                               long unitsSold,
                               double revenue,
                               double averageCheck,
                               double cogs,
                               double grossProfit,
                               double operatingExpenses,
                               double fixedOperatingExpenses,
                               double variableOperatingExpenses,
                               double operatingProfit,
                               double foodPurchases,
                               long ordersWithoutItems,
                               double revenueWithoutItems) {

    /** Валовая маржа в процентах от выручки. */
    public double grossMarginPercent() {
        return revenue == 0 ? 0 : grossProfit / revenue * 100.0;
    }

    /** Рентабельность по операционной прибыли, %. */
    public double operatingMarginPercent() {
        return revenue == 0 ? 0 : operatingProfit / revenue * 100.0;
    }

    /** Общие расходы = COGS + OE. */
    public double totalCosts() {
        return cogs + operatingExpenses;
    }
}
