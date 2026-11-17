package kg.fastfood.crm.math;

import kg.fastfood.crm.math.CostCalculator.SoldProduct;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BreakEvenCalculatorTest {

    @Test
    void calculatesSingleProductAndAssortmentBreakEven() {
        // F = 60000, P = 220, Cv = 120 => Q_BE = 60000 / 100 = 600 шт.
        assertEquals(600.0, BreakEvenCalculator.unitsForProduct(60000.0, 220.0, 120.0), 1e-6);

        // Ассортимент: 100 бургеров (P=220, C=120 => маржа 100) + 100 колы (P=80, C=40 => маржа 40)
        // M_avg = (10000 + 4000) / 200 = 70; R = 22000 + 8000 = 30000; k_M = 14000 / 30000 = 7/15
        List<SoldProduct> sold = List.of(
                new SoldProduct("Бургер", 100, 220.0, 120.0),
                new SoldProduct("Кола", 100, 80.0, 40.0)
        );
        double mAvg = BreakEvenCalculator.weightedAverageMargin(sold);
        assertEquals(70.0, mAvg, 1e-6);

        double qBe = BreakEvenCalculator.unitsForAssortment(70000.0, mAvg);
        assertEquals(1000.0, qBe, 1e-6);

        double ratio = BreakEvenCalculator.contributionMarginRatio(14000.0, 30000.0);
        double rBe = BreakEvenCalculator.revenueBreakEven(70000.0, ratio);
        assertEquals(150000.0, rBe, 1e-4);
    }

    @Test
    void rejectsNonPositiveMargin() {
        assertThrows(IllegalArgumentException.class,
                () -> BreakEvenCalculator.unitsForProduct(50000.0, 100.0, 100.0));
        assertThrows(IllegalArgumentException.class,
                () -> BreakEvenCalculator.unitsForAssortment(50000.0, 0.0));
    }
}
