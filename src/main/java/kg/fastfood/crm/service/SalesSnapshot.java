package kg.fastfood.crm.service;

import kg.fastfood.crm.dto.PeriodRange;
import kg.fastfood.crm.entity.Expense;
import kg.fastfood.crm.entity.Order;

import java.util.List;
import java.util.Map;

/**
 * Исходные данные за период, загруженные из БД одним разом:
 * заказы с составом, себестоимость товаров (id → C) и расходы.
 * Все расчёты (финансы, модели) выполняются над этим снимком.
 */
public record SalesSnapshot(PeriodRange period,
                            List<Order> orders,
                            Map<Long, Double> productCosts,
                            List<Expense> expenses) {

    /** Себестоимость единицы товара (0, если состав не задан). */
    public double costOf(Long productId) {
        return productCosts.getOrDefault(productId, 0.0);
    }
}
