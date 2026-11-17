package kg.fastfood.crm.entity;

/** Категории расходов предприятия. */
public enum ExpenseCategory {
    RENT("Аренда"),
    SALARY("Заработная плата"),
    UTILITIES("Коммунальные услуги"),
    FOOD_PURCHASE("Закупка продуктов"),
    INTERNET("Интернет и связь"),
    OTHER("Прочие расходы");

    private final String label;

    ExpenseCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Закупка продуктов НЕ входит в операционные расходы (OE): стоимость израсходованных
     * ингредиентов уже учтена в себестоимости проданной продукции (COGS).
     * Так исключается двойной учёт затрат.
     */
    public boolean isOperatingExpense() {
        return this != FOOD_PURCHASE;
    }
}
