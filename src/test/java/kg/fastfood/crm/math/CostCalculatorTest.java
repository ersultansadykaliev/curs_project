package kg.fastfood.crm.math;

import kg.fastfood.crm.math.CostCalculator.IngredientUsage;
import kg.fastfood.crm.math.CostCalculator.SoldProduct;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CostCalculatorTest {

    @Test
    void calculatesProductCostAndMarginalProfit() {
        // Булочка 1*18 + Котлета 1*78 + Сыр 0.025*720 + Соус 0.03*320 = 18 + 78 + 18 + 9.6 = 123.6
        double cost = CostCalculator.productCost(List.of(
                new IngredientUsage(1.0, 18.0),
                new IngredientUsage(1.0, 78.0),
                new IngredientUsage(0.025, 720.0),
                new IngredientUsage(0.03, 320.0)
        ));
        assertEquals(123.6, cost, 1e-6);
        assertEquals(96.4, CostCalculator.unitMargin(220.0, cost), 1e-6);
        assertEquals(43.818181, CostCalculator.marginPercent(220.0, cost), 1e-4);

        // M_total = 10*(220 - 120) + 5*(130 - 50) = 1000 + 400 = 1400
        double totalMargin = CostCalculator.totalMargin(List.of(
                new SoldProduct("Бургер", 10, 220.0, 120.0),
                new SoldProduct("Фри", 5, 130.0, 50.0)
        ));
        assertEquals(1400.0, totalMargin, 1e-6);
    }

    @Test
    void rejectsNegativeValues() {
        assertThrows(IllegalArgumentException.class,
                () -> CostCalculator.productCost(List.of(new IngredientUsage(-1, 10))));
        assertThrows(IllegalArgumentException.class,
                () -> CostCalculator.unitMargin(100, -5));
    }
}
