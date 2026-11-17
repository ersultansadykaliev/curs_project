package kg.fastfood.crm.dto;

/** Строка таблицы товаров: цена, рассчитанная себестоимость, маржа и количество продаж. */
public record ProductRow(Long id,
                         String name,
                         String category,
                         double price,
                         double cost,
                         double margin,
                         double marginPercent,
                         long unitsSold,
                         int ingredientsCount,
                         boolean active) {
}
