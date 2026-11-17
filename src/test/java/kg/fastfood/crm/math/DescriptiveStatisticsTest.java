package kg.fastfood.crm.math;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DescriptiveStatisticsTest {

    @Test
    void calculatesAverageCheckAndMean() {
        // 45 000 сом за 150 заказов => средний чек 300.00 сом
        assertEquals(300.0, DescriptiveStatistics.averageCheck(45000.0, 150), 1e-6);
        assertEquals(20.0, DescriptiveStatistics.mean(List.of(10.0, 20.0, 30.0)), 1e-6);
        assertEquals(15.0, DescriptiveStatistics.median(List.of(10.0, 20.0, 12.0, 18.0)), 1e-6);
        assertEquals(10.0, DescriptiveStatistics.standardDeviation(List.of(10.0, 20.0, 30.0)), 1e-6);
    }

    @Test
    void guardsAgainstZeroOrdersAndEmptyData() {
        assertThrows(IllegalArgumentException.class, () -> DescriptiveStatistics.averageCheck(1000.0, 0));
        assertThrows(IllegalArgumentException.class, () -> DescriptiveStatistics.averageCheck(-10.0, 5));
        assertThrows(IllegalArgumentException.class, () -> DescriptiveStatistics.mean(List.of()));
    }
}
