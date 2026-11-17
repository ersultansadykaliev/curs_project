package kg.fastfood.crm.dto;

import java.time.LocalDate;

/** Строка финансового отчёта за день / неделю / месяц. */
public record FinanceRow(String label,
                         LocalDate start,
                         LocalDate end,
                         long ordersCount,
                         double revenue,
                         double cogs,
                         double grossProfit,
                         double operatingExpenses,
                         double operatingProfit) {
}
