package kg.fastfood.crm.math;

import java.util.List;

/**
 * Модель №4. Себестоимость и маржинальная прибыль.
 * <pre>
 *   C = Σ(qᵢ × pᵢ)                 — себестоимость одной порции
 *   M = P − C                      — маржинальная прибыль одного товара
 *   M_total = Σ(nᵢ × (Pᵢ − Cᵢ))    — общая маржинальная прибыль
 * </pre>
 */
public final class CostCalculator {

    private CostCalculator() {
    }

    /** Строка состава блюда: q — количество ингредиента, p — цена единицы ингредиента. */
    public record IngredientUsage(double quantity, double unitCost) {
    }

    /** Проданный товар: n — количество проданных единиц, price — цена P, cost — себестоимость C. */
    public record SoldProduct(String name, long unitsSold, double price, double cost) {
    }

    /** C = Σ(qᵢ × pᵢ). Пустой состав даёт себестоимость 0. */
    public static double productCost(List<IngredientUsage> recipe) {
        double cost = 0;
        if (recipe == null) {
            return cost;
        }
        for (IngredientUsage line : recipe) {
            if (line.quantity() < 0 || line.unitCost() < 0) {
                throw new IllegalArgumentException("Количество и стоимость ингредиента не могут быть отрицательными");
            }
            cost += line.quantity() * line.unitCost();
        }
        return cost;
    }

    /** M = P − C. */
    public static double unitMargin(double price, double cost) {
        if (price < 0 || cost < 0) {
            throw new IllegalArgumentException("Цена и себестоимость не могут быть отрицательными");
        }
        return price - cost;
    }

    /** Маржинальность в процентах от цены: (P − C) / P × 100. При P = 0 возвращает 0. */
    public static double marginPercent(double price, double cost) {
        if (price == 0) {
            return 0;
        }
        return unitMargin(price, cost) / price * 100.0;
    }

    /** M_total = Σ(nᵢ × (Pᵢ − Cᵢ)). */
    public static double totalMargin(List<SoldProduct> products) {
        double total = 0;
        for (SoldProduct p : products) {
            if (p.unitsSold() < 0) {
                throw new IllegalArgumentException("Количество продаж не может быть отрицательным");
            }
            total += p.unitsSold() * unitMargin(p.price(), p.cost());
        }
        return total;
    }
}
