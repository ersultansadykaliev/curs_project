package kg.fastfood.crm.math;

/**
 * Модель №7. Производительность персонала.
 * <pre>
 *   E = N / H
 * </pre>
 * N — количество заказов, H — отработанные человеко-часы. E показывает, сколько заказов
 * в среднем обрабатывается за один человеко-час.
 */
public final class StaffProductivity {

    private StaffProductivity() {
    }

    public static double productivity(long ordersCount, double personHours) {
        if (personHours <= 0) {
            throw new IllegalArgumentException("Количество человеко-часов должно быть больше нуля");
        }
        if (ordersCount < 0) {
            throw new IllegalArgumentException("Количество заказов не может быть отрицательным");
        }
        return ordersCount / personHours;
    }

    /**
     * Сколько сотрудников нужно, чтобы обработать expectedOrders заказов за час при производительности E:
     * ⌈expectedOrders / E⌉, но не меньше 1.
     */
    public static int requiredStaff(double expectedOrdersPerHour, double productivity) {
        if (productivity <= 0) {
            throw new IllegalArgumentException("Производительность должна быть больше нуля");
        }
        return Math.max(1, (int) Math.ceil(expectedOrdersPerHour / productivity));
    }
}
