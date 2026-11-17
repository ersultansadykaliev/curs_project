package kg.fastfood.crm.entity;

/** Тип расхода: постоянный (не зависит от объёма продаж) или переменный. */
public enum ExpenseType {
    FIXED("Постоянный"),
    VARIABLE("Переменный");

    private final String label;

    ExpenseType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
