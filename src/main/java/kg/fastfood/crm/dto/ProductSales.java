package kg.fastfood.crm.dto;

/** Продажи товара за период. */
public record ProductSales(Long productId, String name, long units, double revenue, double cogs) {

    public double margin() {
        return revenue - cogs;
    }
}
