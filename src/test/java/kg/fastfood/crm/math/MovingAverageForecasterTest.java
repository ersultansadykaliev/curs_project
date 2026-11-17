package kg.fastfood.crm.math;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MovingAverageForecasterTest {

    @Test
    void forecastsSevenDayMovingAverageAndBacktest() {
        List<Double> series = List.of(1000.0, 2000.0, 3000.0, 4000.0, 5000.0, 6000.0, 7000.0, 8000.0);
        // Последние 7: 2000..8000 -> среднее = 5000.0
        assertEquals(5000.0, MovingAverageForecaster.forecastNext(series, 7), 1e-6);

        // Backtest для w=7 на 8 точках даёт 1 прогноз: среднее первых 7 (1000..7000) = 4000.0
        List<Double> bt = MovingAverageForecaster.backtest(series, 7);
        assertEquals(1, bt.size());
        assertEquals(4000.0, bt.get(0), 1e-6);
    }

    @Test
    void reportsInsufficientDataWithoutDummyValues() {
        List<Double> shortSeries = List.of(1000.0, 1200.0, 1100.0);
        assertFalse(MovingAverageForecaster.hasEnoughData(shortSeries.size(), 7));
        assertThrows(IllegalArgumentException.class,
                () -> MovingAverageForecaster.forecastNext(shortSeries, 7));
    }
}
