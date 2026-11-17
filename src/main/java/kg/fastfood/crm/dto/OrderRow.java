package kg.fastfood.crm.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** Строка списка заказов. */
public record OrderRow(Long id,
                       String orderNumber,
                       LocalDate date,
                       LocalTime time,
                       double total,
                       int itemsCount,
                       String itemsSummary,
                       String comment) {
}
