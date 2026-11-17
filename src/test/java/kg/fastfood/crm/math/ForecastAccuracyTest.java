package kg.fastfood.crm.math;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ForecastAccuracyTest {

    @Test
    void calculatesMaeAndRelativeMae() {
        // |100 - 110| = 10, |200 - 180| = 20, |300 - 300| = 0 => MAE = 30 / 3 = 10.0
        List<Double> actual = List.of(100.0, 200.0, 300.0);
        List<Double> predicted = List.of(110.0, 180.0, 300.0);

        assertEquals(10.0, ForecastAccuracy.mae(actual, predicted), 1e-6);
        // Среднее actual = 200 => 10 / 200 * 100 = 5.0%
        assertEquals(5.0, ForecastAccuracy.relativeMaePercent(actual, predicted), 1e-6);
    }

    @Test
    void rejectsMismatchedOrEmptyLists() {
        assertThrows(IllegalArgumentException.class,
                () -> ForecastAccuracy.mae(List.of(100.0), List.of(100.0, 200.0)));
        assertThrows(IllegalArgumentException.class,
                () -> ForecastAccuracy.mae(List.of(), List.of()));
    }
}
