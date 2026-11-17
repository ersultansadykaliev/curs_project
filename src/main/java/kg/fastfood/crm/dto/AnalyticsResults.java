package kg.fastfood.crm.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Результаты математических моделей, которые передаются на страницу «Математическая аналитика».
 * Поле error ≠ null означает, что модель не может быть посчитана (например, недостаточно данных),
 * и вместо результата показывается это сообщение.
 */
public final class AnalyticsResults {

    private AnalyticsResults() {
    }

    /** Модель №1. Средние показатели. */
    public record AverageResult(double revenue,
                                long orders,
                                double averageCheck,
                                long days,
                                double meanOrdersPerDay,
                                double meanRevenuePerDay,
                                double stdRevenuePerDay,
                                double medianCheck,
                                List<String> labels,
                                List<Double> dailyOrders,
                                List<Double> dailyRevenue,
                                String interpretation,
                                String error) {
    }

    /** Строка таблицы распределения Пуассона. */
    public record PoissonRow(int k,
                             double probability,
                             double cumulative,
                             double atLeast,
                             long observedIntervals,
                             double observedShare) {
    }

    /** Модель №2. Распределение Пуассона. */
    public record PoissonResult(String intervalType,
                                String intervalLabel,
                                Integer hour,
                                long ordersCount,
                                long intervalsCount,
                                double lambda,
                                List<PoissonRow> rows,
                                int mostLikelyK,
                                int quantile95,
                                double observedVariance,
                                double dispersionIndex,
                                int queryK,
                                double queryProbability,
                                double queryAtLeast,
                                Double productivity,
                                Integer recommendedStaff,
                                String interpretation,
                                String error) {
    }

    /** MAE для одного размера окна — для сравнения вариантов прогноза. */
    public record WindowMae(int window, double mae, int observations) {
    }

    /** Модели №3 и №6. Скользящее среднее и оценка его точности (MAE). */
    public record ForecastResult(int window,
                                 int observations,
                                 Double forecastValue,
                                 LocalDate targetDate,
                                 List<String> labels,
                                 List<Double> actual,
                                 List<Double> predicted,
                                 Double mae,
                                 Double relativeMaePercent,
                                 int backtestCount,
                                 List<Double> absoluteErrors,
                                 List<WindowMae> windowComparison,
                                 String forecastInterpretation,
                                 String maeInterpretation,
                                 String error,
                                 String maeError) {
    }

    /** Сохранённый прогноз и (если дата уже наступила) фактическая выручка за этот день. */
    public record SavedForecastRow(Long id,
                                   java.time.LocalDateTime createdAt,
                                   int window,
                                   LocalDate targetDate,
                                   double forecastValue,
                                   Double mae,
                                   Double actualValue) {

        /** Абсолютная ошибка |y − ŷ|, когда фактическое значение уже известно. */
        public Double absoluteError() {
            return actualValue == null ? null : Math.abs(actualValue - forecastValue);
        }
    }

    /** Строка анализа маржинальности товара. */
    public record MarginRow(String name,
                            long units,
                            double averagePrice,
                            double cost,
                            double unitMargin,
                            double marginPercent,
                            double totalMargin,
                            double shareOfMargin,
                            boolean hasRecipe) {
    }

    /** Модель №4. Себестоимость и маржинальная прибыль. */
    public record MarginResult(List<MarginRow> rows,
                               double totalMargin,
                               double totalRevenue,
                               double totalCogs,
                               List<String> productsWithoutRecipe,
                               String interpretation,
                               String error) {
    }

    /** Точка безубыточности для отдельного товара. */
    public record BreakEvenRow(String name, double price, double variableCost, double margin, Double breakEvenUnits) {
    }

    /** Модель №5. Точка безубыточности. */
    public record BreakEvenResult(double fixedCosts,
                                  double variableOverhead,
                                  double variableOverheadPerUnit,
                                  long actualUnits,
                                  double actualRevenue,
                                  double averagePrice,
                                  double averageVariableCost,
                                  double weightedMargin,
                                  double breakEvenUnits,
                                  double marginRatio,
                                  double breakEvenRevenue,
                                  double safetyMarginPercent,
                                  long days,
                                  double breakEvenUnitsPerDay,
                                  double breakEvenOrdersPerDay,
                                  List<BreakEvenRow> productRows,
                                  List<String> chartLabels,
                                  List<Double> chartRevenue,
                                  List<Double> chartCosts,
                                  String interpretation,
                                  String error) {
    }

    /** Производительность сотрудника. */
    public record EmployeeProductivity(String name,
                                       String position,
                                       long shifts,
                                       double hours,
                                       long ordersDuringShifts,
                                       double productivity) {
    }

    /** Производительность по часу суток. */
    public record HourProductivity(int hour, double averageOrders, double averageStaff, Double productivity) {
    }

    /** Модель №7. Производительность персонала. */
    public record ProductivityResult(long orders,
                                     double personHours,
                                     double productivity,
                                     long daysWithShifts,
                                     List<EmployeeProductivity> employees,
                                     List<HourProductivity> hours,
                                     List<String> dayLabels,
                                     List<Double> dayProductivity,
                                     String interpretation,
                                     String error) {
    }
}
