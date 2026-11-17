package kg.fastfood.crm.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StaffProductivityTest {

    @Test
    void calculatesProductivityAndRequiredStaff() {
        // 120 заказов за 40 человеко-часов => E = 3.0 заказа/чел·ч
        double e = StaffProductivity.productivity(120, 40.0);
        assertEquals(3.0, e, 1e-6);

        // При ожидаемых 10 заказах/ч и E = 3.0 нужно ceil(10/3) = 4 сотрудника
        assertEquals(4, StaffProductivity.requiredStaff(10.0, e));
        // Минимум 1 сотрудник
        assertEquals(1, StaffProductivity.requiredStaff(0.5, e));
    }

    @Test
    void guardsZeroHours() {
        assertThrows(IllegalArgumentException.class, () -> StaffProductivity.productivity(10, 0.0));
        assertThrows(IllegalArgumentException.class, () -> StaffProductivity.requiredStaff(10, 0.0));
    }
}
